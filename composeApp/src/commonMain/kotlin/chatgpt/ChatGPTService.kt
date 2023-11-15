package chatgpt

import com.aallam.ktoken.Tokenizer
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionMode
import com.aallam.openai.client.OpenAI
import feature.fetchurl.FetchURLFunction
import feature.termdefinition.FetchTermDefinitionFunction
import feature.termdefinition.RegisterTermDefinitionFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import model.AIModel
import org.slf4j.LoggerFactory

private fun ChatCompletionChunk.toChatCompletionStreamItem(): ChatCompletionStreamItem {
    return StringChatCompletionStreamItem(this.choices.firstOrNull()?.delta?.content ?: "")
}

sealed class ChatCompletionStreamItem

data class StringChatCompletionStreamItem(
    val content: String
) : ChatCompletionStreamItem()

data class FunctionChatCompletionStreamItem(
    val chatMessage: ChatMessage
) : ChatCompletionStreamItem()

class ChatGPTService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val functions = listOf(
        FetchURLFunction(),
        FetchTermDefinitionFunction(),
        RegisterTermDefinitionFunction(),
    )

    suspend fun sendMessage(
        apiKey: String,
        aiModel: AIModel,
        prompt: String,
        messages: List<ChatMessage>,
        progressUpdate: (String) -> Unit,
    ): Flow<ChatCompletionStreamItem> {
        val openai = OpenAI(token = apiKey)

        // gpt-3.5-turbo is max 4,097 tokens.
        // we must take 500 tokens for response.
        // https://platform.openai.com/docs/models/gpt-3-5
        val tokenizer = aiModel.tokenizer
        val remainTokens = aiModel.maxTokens - tokenizer.encode(prompt).size - (aiModel.maxTokens / 8)
        val usingMessages = getMessagesByTokenCount(messages, tokenizer, remainTokens)

        println("Using model: ${aiModel.name}")
        progressUpdate("Calling OpenAPI: ${aiModel.name}(using ${usingMessages.size} messages)")

        val f = openai.chatCompletions(
            ChatCompletionRequest(
                model = aiModel.modelId,
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = prompt
                    )
                ) + usingMessages,
                functionCall = FunctionMode.Auto,
                functions = functions.map { it.definition }.toList()
            )
        )

        val firstItem = f.take(1).first()
        val funcall = firstItem.choices.first().delta.functionCall
        if (funcall != null) {
            val argument =
                f.map { it.choices.first().delta.functionCall?.argumentsOrNull ?: "" }.toList().joinToString("")
            logger.info("ARGUMENT: ${funcall.name} $argument")
            progressUpdate("Running function: ${funcall.name}: $argument")

            val funcallMsg = try {
                functions.firstOrNull { it.name == funcall.name }?.callFunction(
                    argument,
                    remainTokens - tokenizer.encode(messages.last().content!!).size
                ) ?: ChatMessage(
                    role = ChatRole.Function,
                    name = funcall.name,
                    content = "Unknown function: ${funcall.name}",
                )
            } catch (e: Exception) {
                logger.error("Failed to call function: ${funcall.name}", e)
                ChatMessage(
                    role = ChatRole.Function,
                    name = funcall.name,
                    content = "Cannot call function: ${funcall.name}(${e.javaClass.canonicalName} ${e.message})}",
                )
            }

            progressUpdate("Calling OpenAI API again...")

            val usingMessages2 =
                getMessagesByTokenCount(
                    messages,
                    tokenizer,
                    remainTokens - tokenizer.encode(funcallMsg.content!!).size
                )
            val chatCompletionChunkFlow = openai.chatCompletions(
                ChatCompletionRequest(
                    model = aiModel.modelId,
                    messages = listOf(
                        ChatMessage(
                            role = ChatRole.System,
                            content = prompt
                        ),
                    ) + usingMessages2 + listOf(funcallMsg),
                )
            )

            return  (
                listOf(flowOf(FunctionChatCompletionStreamItem(funcallMsg)))
                + listOf(
                chatCompletionChunkFlow.map { logs ->
                    logs.toChatCompletionStreamItem()
                }
            )).merge()
        } else {
            return listOf(flowOf(firstItem), f).merge().map { logs ->
                logs.toChatCompletionStreamItem()
            }
        }
    }


    private fun getMessagesByTokenCount(
        messages: List<ChatMessage>,
        tokenizer: Tokenizer,
        remainTokens: Int
    ): List<ChatMessage> {
        var remainTokens1 = remainTokens
        val usingMessages = mutableListOf<ChatMessage>()
        for (chatMessage in messages.reversed()) {
            val tokens = tokenizer.encode(chatMessage.content!!).size
            if (remainTokens1 < tokens) {
                break
            }

            usingMessages.add(chatMessage)

            remainTokens1 -= tokens
        }
        return usingMessages.reversed()
    }
}

