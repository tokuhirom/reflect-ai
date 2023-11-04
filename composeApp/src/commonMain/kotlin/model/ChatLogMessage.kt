package model

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import java.time.Instant

data class ChatLog(
    val logs: List<ChatLogMessage> = emptyList(),
)

data class ChatLogMessage(
    val role: ChatLogRole,
    val message: String,
    val timestamp: Instant = Instant.now(),
) {
    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            role = when (role) {
                ChatLogRole.AI -> ChatRole.System
                ChatLogRole.User -> ChatRole.User
            },
            content = message
        )
    }
}

enum class ChatLogRole {
    AI,
    User,
}
