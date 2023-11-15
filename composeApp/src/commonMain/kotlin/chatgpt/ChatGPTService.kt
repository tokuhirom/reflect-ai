package chatgpt

import com.aallam.ktoken.Tokenizer
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionMode
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import model.AIModel
import org.slf4j.LoggerFactory

sealed class ChatCompletionStreamItem

data class StringChatCompletionStreamItem(
    val content: String
) : ChatCompletionStreamItem()

data class FunctionChatCompletionStreamItem(
    val chatMessage: ChatMessage
) : ChatCompletionStreamItem()

class ChatGPTService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fetchUrlFunction = FetchURLFunction()

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
                functions = listOf(
                    fetchUrlFunction.definition,
                )
            )
        )

        val firstItem = f.take(1).first()
        val funcall = firstItem.choices.first().delta.functionCall
        val (funcallMessage, chatCompletionChunkFlow) = if (funcall != null) {
            val argument =
                f.map { it.choices.first().delta.functionCall?.argumentsOrNull ?: "" }.toList().joinToString("")
            logger.info("ARGUMENT: ${funcall.name} $argument")
            progressUpdate("Running function: ${funcall.name}: $argument")

             val funcallMsg = when (funcall.name) {
                "fetch_url" -> {
                    fetchUrlFunction.callFunction(
                        argument,
                        progressUpdate,
                        remainTokens - tokenizer.encode(messages.last().content!!).size
                    )
                }

                else -> {
                    throw RuntimeException("Unknown function call: ${funcall.name}")
                }
            }

            val usingMessages2 =
                getMessagesByTokenCount(
                    messages,
                    tokenizer,
                    remainTokens - tokenizer.encode(funcallMsg.content!!).size
                )
            funcallMsg to openai.chatCompletions(
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
        } else {
            null to listOf(flowOf(firstItem), f).merge()
        }

        val head = if (funcallMessage != null) {
            listOf<Flow<ChatCompletionStreamItem>>(flowOf(FunctionChatCompletionStreamItem(funcallMessage)))
        } else {
            emptyList()
        }
        val result : Flow<ChatCompletionStreamItem> = (head + listOf(
            chatCompletionChunkFlow.map { logs ->
                StringChatCompletionStreamItem(logs.choices.firstOrNull()?.delta?.content ?: "")
            }
        )).merge()
        return result
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