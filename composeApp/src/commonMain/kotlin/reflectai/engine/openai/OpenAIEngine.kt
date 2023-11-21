package reflectai.engine.openai

import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.FunctionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import reflectai.ConfigRepository
import reflectai.engine.ChatCompletionStreamItem
import reflectai.engine.ErrorChatCompletionStreamItem
import reflectai.engine.FunctionChatCompletionStreamItem
import reflectai.engine.StringChatCompletionStreamItem
import reflectai.feature.FunctionRepository

private fun ChatCompletionChunk.toChatCompletionStreamItem(): ChatCompletionStreamItem {
    return StringChatCompletionStreamItem(this.choices.firstOrNull()?.delta?.content ?: "")
}

class OpenAIEngine(
    private val functionRepository: FunctionRepository,
    private val configRepository: ConfigRepository,
    private val openaiProvider: OpenAIProvider
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun generate(
        openAiModel: OpenAIModel,
        messages: List<ChatMessage>,
        progressUpdate: (String) -> Unit,
    ): Flow<ChatCompletionStreamItem> {
        if (!openaiProvider.isAvailable()) {
            return flowOf(
                ErrorChatCompletionStreamItem("OpenAI token is not available")
            )
        }

        val openai = openaiProvider.get()
        val config = configRepository.loadSettings()
        val prompt = config.prompt

        // gpt-3.5-turbo is max 4,097 tokens.
        // we must take 500 tokens for response.
        // https://platform.openai.com/docs/models/gpt-3-5
        val remainTokens = openAiModel.maxTokens - openAiModel.countToken(prompt) - (openAiModel.maxTokens / 8)
        val usingMessages = getMessagesByTokenCount(messages, openAiModel, remainTokens)

        println("Using model: ${openAiModel.name}")
        progressUpdate("Calling OpenAPI: ${openAiModel.name}(using ${usingMessages.size} messages)")

        val f = openai.chatCompletions(
            ChatCompletionRequest(
                model = openAiModel.modelId,
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = prompt
                    )
                ) + usingMessages,
                functionCall = FunctionMode.Auto,
                functions = functionRepository.toList()
            )
        )

        val firstItem = f.take(1).first()
        val funcall = firstItem.choices.first().delta.functionCall
        if (funcall != null) {
            val argument =
                f.map { it.choices.first().delta.functionCall?.argumentsOrNull ?: "" }.toList().joinToString("")
            logger.info("ARGUMENT: ${funcall.name} $argument")
            progressUpdate("Running function: ${funcall.name}: $argument")

            val function = functionRepository.getByName(funcall.name)

            val funcallMsg = try {
                function?.callFunction(
                    argument,
                    remainTokens - openAiModel.countToken(messages.last().content!!)
                ) ?: ChatMessage(
                    role = ChatRole.Function,
                    name = funcall.name,
                    content = "Unknown function: ${funcall.name}",
                )
            } catch (e: Exception) {
                logger.error("Failed to call function: ${funcall.name}, args=`${argument}`", e)
                ChatMessage(
                    role = ChatRole.Function,
                    name = funcall.name,
                    content = "Cannot call function: ${funcall.name}(${e.javaClass.canonicalName} ${e.message})}",
                )
            }

            if (function != null && function.dontSendToOpenAIAgain()) {
                logger.info("Don't send to OpenAI again: ${funcall.name}")
                return flowOf(FunctionChatCompletionStreamItem(funcallMsg))
            }

            progressUpdate("Calling OpenAI API again...")

            val usingMessages2 =
                getMessagesByTokenCount(
                    messages,
                    openAiModel,
                    remainTokens - openAiModel.countToken(funcallMsg.content!!)
                )
            val chatCompletionChunkFlow = openai.chatCompletions(
                ChatCompletionRequest(
                    model = openAiModel.modelId,
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
        aiModel: OpenAIModel,
        remainTokens: Int
    ): List<ChatMessage> {
        var remainTokens1 = remainTokens
        val usingMessages = mutableListOf<ChatMessage>()
        for (chatMessage in messages.reversed()) {
            val tokens = aiModel.countToken(chatMessage.content!!)
            if (remainTokens1 < tokens) {
                break
            }

            usingMessages.add(chatMessage)

            remainTokens1 -= tokens
        }
        return usingMessages.reversed()
    }
}

