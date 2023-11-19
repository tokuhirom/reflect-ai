package reflectai.engine.openai

import com.aallam.ktoken.Tokenizer
import com.aallam.ktoken.loader.LocalPbeLoader
import com.aallam.openai.api.model.ModelId
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import reflectai.ConfigRepository
import reflectai.engine.AIModel
import reflectai.engine.AIModelType
import reflectai.engine.ModelRepository
import java.text.NumberFormat


private val openAiModels = listOf(
    OpenAIModel("gpt-3.5-turbo-1106", 16385),
    OpenAIModel("gpt-4-1106-preview", 128000),
    OpenAIModel("gpt-3.5-turbo", 4097),
    OpenAIModel("gpt-3.5-turbo-16k", 16385),
    OpenAIModel("gpt-4", 8192),
    OpenAIModel("gpt-4-32k", 32768),
)

class OpenAIModelRepository(private val configRepository: ConfigRepository): ModelRepository {
    override fun getModels() : List<AIModel> {
        return openAiModels
    }
}

data class OpenAIModel(override val name: String, val maxTokens: Int) : AIModel {
    override val type: AIModelType
        get() = AIModelType.OPENAI

    val tokenizer = runBlocking {
        Tokenizer.of(model = name, loader = LocalPbeLoader(FileSystem.RESOURCES))
    }
    val modelId = ModelId(name)

    override fun getLabel(numberFormat: NumberFormat): String {
        return name + " (${maxTokens} max tokens)"
    }
}
