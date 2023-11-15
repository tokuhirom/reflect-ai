package feature.termdefinition

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

data class RegisterTermDefinitionArgument(val word: String, val definition: String)

class RegisterTermDefinitionFunction : OpenAIFunction {
    private val objectMapper = jacksonObjectMapper()
    private val termDefinitionRepository = TermDefinitionRepository()

    override val name: String = "register_term_definition"
    override val definition: ChatCompletionFunction = ChatCompletionFunction(
        name,
        """This function registers the definition of a given term and, if available, a related URL. The term
            | and its related information can be used to enrich the understanding and responses of the
            |  ChatGPT system.""".trimMargin(),
        Parameters.buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("word") {
                    put("type", "string")
                    put("description",
                        "This is the term that the function will register."
                    )
                }
                putJsonObject("definition") {
                    put("type", "string")
                    put("description",
                        "This is the definition of the term that the function will register, along with related" +
                                " URL if available."
                    )
                }
            }
            putJsonArray("required") {
                add("word")
                add("defintion")
            }
        }
    )

    override suspend fun callFunction(argumentJson: String, remainTokens: Int): ChatMessage {
        val argument = objectMapper.readValue<RegisterTermDefinitionArgument>(argumentJson)
        termDefinitionRepository.addWord(argument.word, argument.definition)

        return ChatMessage(
            role = ChatRole.Function,
            name = name,
            content = "Registered term definition: ${argument.word.truncateAt(20)}",
        )
    }
}
