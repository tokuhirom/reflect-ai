package reflectai.engine

import com.aallam.openai.api.chat.ChatMessage

sealed class ChatCompletionStreamItem

data class StringChatCompletionStreamItem(
    val content: String
) : ChatCompletionStreamItem()

data class FunctionChatCompletionStreamItem(
    val chatMessage: ChatMessage
) : ChatCompletionStreamItem()

data class ErrorChatCompletionStreamItem(
    val message: String
) : ChatCompletionStreamItem()
