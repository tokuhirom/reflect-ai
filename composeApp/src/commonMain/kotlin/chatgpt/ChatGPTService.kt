package chatgpt

import com.aallam.ktoken.Tokenizer
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionMode
import com.aallam.openai.client.OpenAI
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import model.AIModel
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import truncateAt

sealed class ChatCompletionStreamItem

data class StringChatCompletionStreamItem(
    val content: String
) : ChatCompletionStreamItem()

data class FunctionChatCompletionStreamItem(
    val chatMessage: ChatMessage
) : ChatCompletionStreamItem()

class ChatGPTService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()
    private val ktorClient = io.ktor.client.HttpClient() {
        install(Logging)
    }

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
                    fetchUrCompletionFunction,
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

             when (funcall.name) {
                "fetch_url" -> {
                    val content = try {
                        val args = objectMapper.readValue<FetchUrlArgument>(argument)
                        val url = args.url
                        // fetch content by url using ktor.
                        val response = ktorClient.get(url)
                        val contentType = response.headers["content-type"] ?: "application/octet-stream"

                        if (contentType.contains("text/html")) {
                            Jsoup.parse(response.body<String>()).text()
                        } else if (contentType.contains("text")) {
                            response.body<String>()
                        } else {
                            "Unsupported content type: $contentType"
                        }
                    } catch (e: Exception) {
                        "Failed to fetch content: ${e.javaClass.canonicalName} ${e.message}"
                    }
                    progressUpdate("Calling OpenAI API again...")

                    val funcallMsg = ChatMessage(
                        role = ChatRole.Function,
                        name = funcall.name,
                        content = content.truncateAt(
                            remainTokens - tokenizer.encode(messages.last().content!!).size
                        ),
                    )

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
                }

                else -> {
                    throw RuntimeException("Unknown function call: ${funcall.name}")
                }
            }
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
