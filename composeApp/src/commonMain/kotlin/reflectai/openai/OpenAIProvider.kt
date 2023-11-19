package reflectai.openai

import com.aallam.openai.client.OpenAI
import reflectai.ConfigRepository

class OpenAIProvider(val configRepository: ConfigRepository) {
    private val apiToken: String?
        get() = configRepository.loadSettings().apiToken

    fun isAvailable(): Boolean {
        return apiToken != null && apiToken!!.isNotEmpty()
    }

    fun get(): OpenAI {
        return OpenAI(apiToken ?: throw IllegalStateException("apiToken is null"))
    }
}
