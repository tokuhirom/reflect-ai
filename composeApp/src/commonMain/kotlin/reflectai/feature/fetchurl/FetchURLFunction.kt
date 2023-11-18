package reflectai.feature.fetchurl

import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.Parameters
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import reflectai.feature.OpenAIFunction
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.jsoup.Jsoup
import reflectai.ui.truncateAt


data class FetchUrlArgument(val url: String)

class FetchURLFunction(private val objectMapper: ObjectMapper, private val ktorClient: HttpClient)  :
    OpenAIFunction {
    override val name = "fetch_url"

    override val definition = ChatCompletionFunction(
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

    override  suspend fun callFunction(
        argumentJson: String,
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

        return ChatMessage(
            role = ChatRole.Function,
            name = name,
            content = content.truncateAt(remainTokens),
        )
    }
}
