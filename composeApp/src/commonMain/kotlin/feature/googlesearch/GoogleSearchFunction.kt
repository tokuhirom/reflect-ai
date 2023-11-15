package feature.googlesearch

import ConfigRepository
import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.Parameters
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feature.OpenAIFunction
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class GoogleCustomSearchResponse(
    val items: List<GoogleCustomSearchItem>,
    val socialmediaposting: List<GoogleCustomSearchSocialMediaPosting>?,
)

data class GoogleCustomSearchItem(val title: String, val link: String, val snippet: String)

data class GoogleCustomSearchSocialMediaPosting(val articlebody: String, val url: String)

data class GoogleSearchArgument(val query: String)

class GoogleSearchFunction : OpenAIFunction {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val configRepository = ConfigRepository()

    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())

    private val ktorClient = io.ktor.client.HttpClient() {
        install(Logging)
    }

    override val name = "google_search"
    override val definition = ChatCompletionFunction(
        name,
        """Search the web by google search.""".trimMargin(),
        Parameters.buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("query") {
                    put("type", "string")
                    put("description",
                        """Query for google search engine.""".trimMargin()
                    )
                }
            }
            putJsonArray("required") {
                add("query")
            }
        }
    )

    override suspend fun callFunction(argumentJson: String, remainTokens: Int): ChatMessage {
        val args = objectMapper.readValue<GoogleSearchArgument>(argumentJson)

        val config = configRepository.loadSettings()
        val apiKey = config.googleSearchConfig.apiKey ?: return ChatMessage(
            role = ChatRole.Function,
            name = name,
            content = "Missing API key for google custom search",
        )
        val searchEngineId = config.googleSearchConfig.searchEngineId ?: return ChatMessage(
            role = ChatRole.Function,
            name = name,
            content = "Missing searchEngineId for google custom search",
        )

        val url = "https://customsearch.googleapis.com/customsearch/v1?key=${apiKey}&cx=${searchEngineId}&q=${URLEncoder.encode(args.query, StandardCharsets.UTF_8)}"
        logger.info("Querying google: $url")
        val response = ktorClient.get(url)
        val json = response.bodyAsText()
        val res = objectMapper.readValue<GoogleCustomSearchResponse>(json)

        val content = "# Search results\n\n" + res.items.map {
            "## [${it.title}](${it.link})\n\n${it.snippet}\n\n"
        } + "# Social media\n\n" + res.socialmediaposting?.joinToString("\n\n----\n\n") {
            "${it.articlebody}\n\n${it.url}\n"
        }

        logger.info("google search result for ${args.query}:\n$content")

        return ChatMessage(
            role = ChatRole.Function,
            name = name,
            content = content,
        )
    }
}
