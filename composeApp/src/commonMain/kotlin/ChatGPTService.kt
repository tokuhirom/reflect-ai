import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.client.OpenAI
import model.AIModel
import org.slf4j.LoggerFactory

class ChatGPTService(private val apiKey: String, private val prompt: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun sendMessage(aiModel: AIModel, messages: List<ChatMessage>): String {
        val openai = OpenAI(token = apiKey)

        // gpt-3.5-turbo is max 4,097 tokens.
        // we must take 500 tokens for response.
        // https://platform.openai.com/docs/models/gpt-3-5
        val tokenizer = aiModel.tokenizer
        var remainTokens = aiModel.maxTokens - tokenizer.encode(prompt).size - 500
        val usingMessages = mutableListOf<ChatMessage>()
        for (chatMessage in messages.reversed()) {
            val tokens = tokenizer.encode(chatMessage.content!!).size
            if (remainTokens < tokens) {
                break
            }

            usingMessages.add(chatMessage)

            remainTokens -= tokens
        }

        println("Using model: ${aiModel.name}")

        val response = openai.chatCompletion(
            ChatCompletionRequest(
                model = aiModel.modelId,
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = prompt
                    )
                ) + usingMessages.reversed()
            )
        )
        return response.choices.firstOrNull()?.message?.content ?: ""
    }
}
