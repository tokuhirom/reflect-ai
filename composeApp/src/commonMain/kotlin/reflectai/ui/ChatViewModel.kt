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
import reflectai.model.AIModel
import reflectai.model.ChatLogMessage
import reflectai.model.ChatLogRole
import reflectai.model.aiModels
import reflectai.openai.ErrorChatCompletionStreamItem
import reflectai.openai.FunctionChatCompletionStreamItem
import reflectai.openai.OpenAIService
import reflectai.openai.StringChatCompletionStreamItem

class ChatViewModel(
    private val openAIService: OpenAIService,
    private val chatLogRepository: ChatLogRepository,
    private val configRepository: ConfigRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val initialConversation = chatLogRepository.loadConversations().logs

    var message by mutableStateOf(TextFieldValue(""))
    var conversation by mutableStateOf(initialConversation)
    var progressIndicator by mutableStateOf(TextFieldValue(""))
    var targetAiModel by mutableStateOf(getDefaultAIModel())

    private fun getDefaultAIModel(): AIModel {
        val config = configRepository.loadSettings()
        return aiModels.firstOrNull { it.name == config.defaultModelName }
            ?: aiModels.first()
    }

    fun sendMessage() {
        if (message.text.isNotEmpty()) {
            conversation += ChatLogMessage(ChatLogRole.User, message.text)
            message = TextFieldValue("")

            CoroutineScope(Dispatchers.Main).launch {
                var current = ChatLogMessage(ChatLogRole.AI, "", inProgress = true)
                conversation += current

                fun updateMessage(msg: String, role: ChatLogRole, inProgress: Boolean = false) {
                    current = ChatLogMessage(
                        role,
                        current.message + msg,
                        current.id,
                        inProgress = inProgress,
                        timestamp = current.timestamp
                    )
                    conversation = conversation.filter { it.id != current.id } + current
                }

                try {
                    openAIService.sendMessage(
                        targetAiModel,
                        conversation.toList()
                            .filter { it.role != ChatLogRole.Error }
                            .map { it.toChatMessage() },
                    ) {
                        progressIndicator = TextFieldValue(it)
                    }.onCompletion {
                        println("chatCompletions complete.")
                        updateMessage("", ChatLogRole.AI, false)
                        chatLogRepository.saveConversations(conversation)
                    }.collect {item ->
                        when (item) {
                            is StringChatCompletionStreamItem -> {
                                updateMessage(item.content, ChatLogRole.AI, true)
                            }

                            is FunctionChatCompletionStreamItem -> {
                                conversation = conversation.filter { it.id != current.id } + ChatLogMessage(
                                    ChatLogRole.Function,
                                    item.chatMessage.content!!,
                                    name = item.chatMessage.name)  + current
                            }

                            is ErrorChatCompletionStreamItem -> {
                                updateMessage(item.message, ChatLogRole.Error, true)
                            }
                        }
                        chatLogRepository.saveConversations(conversation)
                    }
                } catch (e: Exception) {
                    logger.error("Got an error : $e", e)
                    updateMessage(if (e.message != null) {
                        "${e.javaClass.canonicalName} ${e.message}"
                    } else {
                        "Got an error : $e"
                    }, ChatLogRole.Error)
                    chatLogRepository.saveConversations(conversation)
                }
            }
        }
    }
}
