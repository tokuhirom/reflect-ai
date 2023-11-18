package reflectai.model

import com.aallam.ktoken.Tokenizer
import com.aallam.ktoken.loader.LocalPbeLoader
import com.aallam.openai.api.model.ModelId
import kotlinx.coroutines.runBlocking
import okio.FileSystem


val aiModels = listOf(
    AIModel("gpt-3.5-turbo-1106", 16385),
    AIModel("gpt-4-1106-preview", 128000),
    AIModel("gpt-3.5-turbo", 4097),
    AIModel("gpt-3.5-turbo-16k", 16385),
    AIModel("gpt-4", 8192),
    AIModel("gpt-4-32k", 32768),
)

data class AIModel(val name: String, val maxTokens: Int) {
    val tokenizer = runBlocking {
        Tokenizer.of(model = name, loader = LocalPbeLoader(FileSystem.RESOURCES))
    }
    val modelId = ModelId(name)
}
