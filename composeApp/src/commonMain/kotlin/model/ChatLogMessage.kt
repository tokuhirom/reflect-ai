package model

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
    val timestamp: Instant = Instant.now(),
) {
    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            role = when (role) {
                ChatLogRole.AI -> ChatRole.Assistant
                ChatLogRole.Error -> ChatRole.System
                ChatLogRole.User -> ChatRole.User
            },
            content = message
        )
    }
}

enum class ChatLogRole {
    AI,
    User,
    Error,
}
