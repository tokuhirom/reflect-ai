package chatgpt

import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.Parameters
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val fetchUrCompletionFunction = ChatCompletionFunction(
    "fetch_url",
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


data class FetchUrlArgument(val url: String)
