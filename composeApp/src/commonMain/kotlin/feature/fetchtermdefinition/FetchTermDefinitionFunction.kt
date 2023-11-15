package feature.fetchtermdefinition

import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.Parameters
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feature.OpenAIFunction
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import truncateAt

data class FetchTermDefinitionArgument(val word: String)

class FetchTermDefinitionFunction : OpenAIFunction {
    override val name = "fetch_term_definition"
    private val objectMapper = jacksonObjectMapper()
    override val definition = ChatCompletionFunction(
        name,
        """This function fetches the definition of a given term and, if available, provides a URL for further
            | information related to the term. The term can be any specialized or internal term commonly used
            | within the organization.""".trimMargin(),
        Parameters.buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("word") {
                    put("type", "string")
                    put("description",
                        """This is the term for which the function will fetch the definition and, if available,
                            | a related URL. It can be any specialized or internal term commonly used within the
                            | organization.""".trimMargin()
                    )
                }
            }
            putJsonArray("required") {
                add("word")
            }
        }
    )

    override suspend fun callFunction(
        argumentJson: String,
        remainTokens: Int,
    ): ChatMessage {
        val content = try {
            val args = objectMapper.readValue<FetchTermDefinitionArgument>(argumentJson)
            when (args.word) {
                "yappo" -> {
                    "Yappo は Osawa Kazuhiro の愛称です。"
                }
                else -> {
                    "Unknown word ${args.word}"
                }
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
