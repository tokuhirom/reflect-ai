import com.aallam.ktoken.Tokenizer
import com.aallam.ktoken.loader.LocalPbeLoader
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import okio.FileSystem
import org.slf4j.LoggerFactory

class ChatGPTService(private val apiKey: String, private val prompt: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun sendMessage(messages: List<ChatMessage>): String {
        val openai = OpenAI(token = apiKey)

        // gpt-3.5-turbo is max 4,097 tokens.
        // we must take 500 tokens for response.
        // https://platform.openai.com/docs/models/gpt-3-5
        val tokenizer = Tokenizer.of(model = "gpt-3.5-turbo", loader = LocalPbeLoader(FileSystem.RESOURCES))
        var remainTokens = 4097 - tokenizer.encode(prompt).size - 500
        val usingMessages = mutableListOf<ChatMessage>()
        for (chatMessage in messages.reversed()) {
            val tokens = tokenizer.encode(chatMessage.content!!).size
            if (remainTokens < tokens) {
                break
            }

            usingMessages.add(chatMessage)

            remainTokens -= tokens
        }

        val response = openai.chatCompletion(ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"), // should be configurable.
            messages =
                listOf(ChatMessage(
                    role = ChatRole.System,
                    content = prompt
                )) + usingMessages.reversed()
        ))
        return response.choices.firstOrNull()?.message?.content ?: ""
    }
}
