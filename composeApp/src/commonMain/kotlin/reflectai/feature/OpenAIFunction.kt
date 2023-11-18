package reflectai.feature

import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.ChatMessage

interface OpenAIFunction {
    val name: String
    val definition: ChatCompletionFunction
    suspend fun callFunction(
        argumentJson: String,
        remainTokens: Int,
    ): ChatMessage

    fun dontSendToOpenAIAgain(): Boolean = false
}
