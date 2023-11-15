package chatgpt

import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.Parameters
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import truncateAt


data class FetchUrlArgument(val url: String)

class FetchURLFunction {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()
    private val ktorClient = io.ktor.client.HttpClient() {
        install(Logging)
    }
    val name = "fetch_url"

    val definition = ChatCompletionFunction(
        name,
        "Fetch content by URL",
        Parameters.buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("url") {
                    put("type", "string")
                    put("format", "uri")
                    put("description", "URL to fetch")
                }
            }
            putJsonArray("required") {
                add("url")
            }
        }
    )

    suspend fun callFunction(
        argumentJson: String,
        progressUpdate: (String) -> Unit,
        remainTokens: Int,
    ): ChatMessage {
        val content = try {
            val args = objectMapper.readValue<FetchUrlArgument>(argumentJson)
            val url = args.url
            // fetch content by url using ktor.
            val response = ktorClient.get(url)
            val contentType = response.headers["content-type"] ?: "application/octet-stream"

            if (contentType.contains("text/html")) {
                Jsoup.parse(response.body<String>()).text()
            } else if (contentType.contains("text")) {
                response.body<String>()
            } else {
                "Unsupported content type: $contentType"
            }
        } catch (e: Exception) {
            "Failed to fetch content: ${e.javaClass.canonicalName} ${e.message}"
        }
        progressUpdate("Calling OpenAI API again...")

        return ChatMessage(
            role = ChatRole.Function,
            name = name,
            content = content.truncateAt(remainTokens),
        )
    }
}
