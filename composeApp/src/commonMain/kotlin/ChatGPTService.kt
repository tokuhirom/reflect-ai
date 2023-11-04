import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import org.slf4j.LoggerFactory

class ChatGPTService(private val apiKey: String, private val prompt: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun sendMessage(messages: List<ChatMessage>): String {
        val openai = OpenAI(token = apiKey)
        val response = openai.chatCompletion(ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"), // should be configurable.
            messages =
                listOf(ChatMessage(
                    role = ChatRole.System,
                    content = prompt
                )) + messages
        ))
        return response.choices.firstOrNull()?.message?.content ?: ""
    }
}
