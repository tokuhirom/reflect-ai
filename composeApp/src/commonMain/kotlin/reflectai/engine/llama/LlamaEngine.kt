package reflectai.engine.llama

import com.aallam.openai.api.chat.ChatMessage
import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import reflectai.ConfigRepository
import reflectai.engine.ChatCompletionStreamItem
import reflectai.engine.StringChatCompletionStreamItem
import java.util.concurrent.ConcurrentHashMap

class LlamaEngine(private val configRepository: ConfigRepository, private val llamaModelRepository: LlamaModelRepository) {
    // TODO make it configurable?
    private var modelParams: ModelParameters = ModelParameters.Builder()
        .setNGpuLayers(43)
        .build()
    private var inferParams: InferenceParameters = InferenceParameters.Builder()
        .setTemperature(0.7f)
        .setPenalizeNl(true)
        .setMirostat(InferenceParameters.MiroStat.V2)
        .setAntiPrompt("\n")
        .build()

    // modelPath -> LlamaModel
    private val modelCache = ConcurrentHashMap<String, LlamaModel>()

    fun generate(
        modelName: String,
        messages: List<ChatMessage>,
        progressUpdate: (String) -> Unit,
    ) : Flow<ChatCompletionStreamItem> {
        val prompt = configRepository.loadSettings().prompt

        val model = modelCache.computeIfAbsent(modelName) {
            val aiModel = llamaModelRepository.findByName(modelName)
            LlamaModel(aiModel.modelPath, modelParams)
        }
        val result = model.generate(
            prompt + "\n\n" + messages.joinToString("\n") {
                it.role.toString() + ":" + it.content
            }, inferParams
        )
        return result.asFlow().map {
            StringChatCompletionStreamItem(it.text)
        }
    }
}
