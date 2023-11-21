package reflectai

import com.aallam.openai.api.ExperimentalOpenAI
import com.aallam.openai.api.embedding.Embedding
import com.aallam.openai.api.embedding.embeddingRequest
import com.aallam.openai.api.exception.InvalidRequestException
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.extension.similarity
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.plugins.*
import org.slf4j.LoggerFactory
import reflectai.engine.openai.OpenAIProvider
import reflectai.model.ChatLogMessage
import reflectai.model.Config
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

// key is UUID ID of ChatLogMessage
data class SimilarSearchData(val logs: MutableMap<String, SimilarSearchRaw>) {
    @OptIn(ExperimentalOpenAI::class)
    suspend fun searchSimilar(message: String, openAI: OpenAI, modelId: ModelId, topN: Int): List<Pair<SimilarSearchRaw, Double>> {
        val resp = openAI.embeddings(
            embeddingRequest {
                model = modelId
                input = listOf(message)
            }
        )
        val embedding = resp.embeddings.first()
        return logs.values.map {
            it to embedding.similarity(Embedding(it.embedding, 0))
        }.sortedByDescending { it.second }.take(topN)
    }
}

data class SimilarSearchRaw(val message: String, val timestamp: Instant, val embedding: List<Double>)

// 類似検索で過去の発言を検索する機能
class SimilarSearchRepository(private val objectMapper: ObjectMapper, private val configRepository: ConfigRepository, private val openAIProvider: OpenAIProvider) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun update(logs: List<ChatLogMessage>) {
            // open log file
            val databasePath = databasePath()
            val data = if (databasePath.exists()) {
                objectMapper.readValue<SimilarSearchData>(databasePath.readText())
            } else {
                SimilarSearchData(mutableMapOf())
            }

            // for each logs
            val config = configRepository.loadSettings()
            logs.forEach {log ->
                if (!data.logs.containsKey(log.id) && log.message.isNotEmpty()) {
                    try {
                        updateByMessage(log, data, config)

                        val jsonData = objectMapper.writeValueAsString(data)
                        val tempFile = databasePath.resolveSibling(databasePath.name + ".tmp")
                        tempFile.writeText(jsonData)
                        Files.move(tempFile, databasePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    } catch (e: InvalidRequestException) {
                        logger.error("Cannot update similar search database: $log", e)
                    } catch (e: ClientRequestException) {
                        logger.error("Cannot update similar search database: $log", e)
                    }
                }
            }
    }


    private fun databasePath(): Path {
        return configRepository.loadSettings().dataDirectoryPath.resolve("similar_search.json")
    }

    private suspend fun updateByMessage(log: ChatLogMessage, data: SimilarSearchData, config: Config) {
        val embeddings = openAIProvider.get().embeddings(
            embeddingRequest {
                model = ModelId(config.embeddingsModelId)
                input = listOf(log.message)
            }
        )
        data.logs[log.id] = SimilarSearchRaw(
            message = log.message,
            timestamp = log.timestamp,
            embedding = embeddings.embeddings.first().embedding
        )
    }
}
