package reflectai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import reflectai.ChatLogRepository
import reflectai.ConfigRepository
import reflectai.engine.AIModel
import reflectai.engine.ErrorChatCompletionStreamItem
import reflectai.engine.FunctionChatCompletionStreamItem
import reflectai.engine.ModelRepository
import reflectai.engine.StringChatCompletionStreamItem
import reflectai.engine.llama.LlamaEngine
import reflectai.engine.llama.LlamaModel
import reflectai.engine.openai.OpenAIEngine
import reflectai.engine.openai.OpenAIModel
import reflectai.model.ChatLogMessage
import reflectai.model.ChatLogRole

class ChatViewModel(
    private val openAIEngine: OpenAIEngine,
    private val chatLogRepository: ChatLogRepository,
    private val configRepository: ConfigRepository,
    private val modelRepositories: List<ModelRepository>,
    private val llamaEngine: LlamaEngine,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val initialConversation = chatLogRepository.loadConversations().logs

    var message by mutableStateOf(TextFieldValue(""))
    var conversation by mutableStateOf(initialConversation)
    var progressIndicator by mutableStateOf(TextFieldValue(""))
    var targetAiModel by mutableStateOf(getDefaultAIModel())

    private fun getDefaultAIModel(): AIModel {
        val config = configRepository.loadSettings()
        val models = modelRepositories.flatMap { it.getModels() }
        return models.firstOrNull { it.name == config.defaultModelName }
            ?: models.first()
    }

    fun sendMessage() {
        conversation += ChatLogMessage(ChatLogRole.User, message.text)
        message = TextFieldValue("")

        CoroutineScope(Dispatchers.Main).launch {
            var current = ChatLogMessage(ChatLogRole.AI, "", inProgress = true)
            conversation += current

            try {
                val result= callEngine()

                result.onCompletion {
                    println("chatCompletions complete.")
                    if (current.message.isEmpty()) {
                        // remove current message if it is empty.
                        // When call gen_image, it returns empty message.
                        conversation = conversation.filter { it.id != current.id }
                    } else {
                        current = updateMessage(current, "", ChatLogRole.AI, false)
                    }
                    chatLogRepository.saveConversations(conversation)
                }.collect {item ->
                    when (item) {
                        is StringChatCompletionStreamItem -> {
                            current = updateMessage(current, item.content, ChatLogRole.AI, true)
                        }

                        is FunctionChatCompletionStreamItem -> {
                            conversation = conversation.filter { it.id != current.id } + ChatLogMessage(
                                ChatLogRole.Function,
                                item.chatMessage.content!!,
                                name = item.chatMessage.name)  + current
                        }

                        is ErrorChatCompletionStreamItem -> {
                            current = updateMessage(current, item.message, ChatLogRole.Error, true)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Got an error : $e", e)
                current = updateMessage(current, if (e.message != null) {
                    "${e.javaClass.canonicalName} ${e.message}"
                } else {
                    "Got an error : $e"
                }, ChatLogRole.Error)
                chatLogRepository.saveConversations(conversation)
            }
        }
    }

    private fun updateMessage(current: ChatLogMessage, msg: String, role: ChatLogRole, inProgress: Boolean = false): ChatLogMessage {
        val next = ChatLogMessage(
            role,
            current.message + msg,
            current.id,
            inProgress = inProgress,
            timestamp = current.timestamp
        )
        conversation = conversation.filter { it.id != current.id } + next
        return next
    }

    private suspend fun callEngine() = when (targetAiModel) {
        is OpenAIModel -> {
            openAIEngine.generate(
                targetAiModel as OpenAIModel,
                conversation.toList()
                    .filter { it.role != ChatLogRole.Error }
                    .map { it.toChatMessage() },
            ) {
                progressIndicator = TextFieldValue(it)
            }
        }

        is LlamaModel -> {
            llamaEngine.generate(
                targetAiModel.name,
                conversation.toList()
                    .filter { it.role != ChatLogRole.Error }
                    .map { it.toChatMessage() }
                    .takeLast(1),
            ) {
                progressIndicator = TextFieldValue(it)
            }
        }

        else -> {
            throw Exception("Unknown AI model: $targetAiModel")
        }
    }
}
