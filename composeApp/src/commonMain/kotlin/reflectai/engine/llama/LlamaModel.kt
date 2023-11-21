package reflectai.engine.llama

import reflectai.ConfigRepository
import reflectai.engine.AIModel
import reflectai.engine.AIModelType
import reflectai.engine.ModelRepository
import java.nio.file.Files
import java.text.NumberFormat
import kotlin.io.path.exists


data class LlamaModel(val modelPath: String, override val name: String) : AIModel {
    override val type: AIModelType
        get() = AIModelType.LLAMA

    override fun getLabel(numberFormat: NumberFormat): String {
        return name
    }

    override fun countToken(text: String): Int {
        throw NotImplementedError("countToken is not implemented")
    }
}

class LlamaModelRepository(private val configRepository: ConfigRepository) : ModelRepository {
    override fun getModels() : List<AIModel> {
        val path = configRepository.loadSettings().dataDirectoryPath
        val modelPath = path.resolve("models/llama/")
        if (!modelPath.exists()) {
            return emptyList()
        }

        val ggufFiles = Files.list(modelPath)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".gguf") }
            .toList()

        return ggufFiles.map {
            LlamaModel(it.toString(), it.fileName.toString())
        }.toList()
    }

    fun findByName(modelName: String): LlamaModel {
        val models = getModels()
        return models.first { it.name == modelName } as LlamaModel
    }
}
