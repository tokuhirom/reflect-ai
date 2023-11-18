package reflectai.model

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import java.time.Instant
import java.util.*

data class ChatLog(
    val logs: List<ChatLogMessage> = emptyList(),
)

data class ChatLogMessage(
    val role: ChatLogRole,
    var message: String,
    val id: String = UUID.randomUUID().toString(),
    /**
     * The name of the author of this message.
     * [name] is required if role is `[ChatRole.Function], and it should be the name of the function whose response is
     * in the [content]. May contain a-z, A-Z, 0-9, and underscores, with a maximum length of 64 characters.
     */
    val name: String? = null,
    val inProgress: Boolean = false,
    val timestamp: Instant = Instant.now(),
) {
    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            role = when (role) {
                ChatLogRole.AI -> ChatRole.Assistant
                ChatLogRole.Error -> ChatRole.System
                ChatLogRole.User -> ChatRole.User
                ChatLogRole.Function -> ChatRole.Function
            },
            content = message,
            name = name,
        )
    }
}

enum class ChatLogRole {
    AI,
    User,
    Error,
    Function,
}
