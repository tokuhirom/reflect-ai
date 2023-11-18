package reflectai.feature.termdefinition

import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.Parameters
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import reflectai.feature.OpenAIFunction
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import reflectai.ui.truncateAt

data class FetchTermDefinitionArgument(val word: String)

class FetchTermDefinitionFunction(private val objectMapper: ObjectMapper, private val teamDefinitionRepository: TermDefinitionRepository) :
    OpenAIFunction {
    override val name = "fetch_term_definition"
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
        val args = objectMapper.readValue<FetchTermDefinitionArgument>(argumentJson)
        val definition = teamDefinitionRepository.getWord(args.word)

        return ChatMessage(
            role = ChatRole.Function,
            name = name,
            content = definition.truncateAt(remainTokens),
        )
    }
}
