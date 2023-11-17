package feature

import com.aallam.openai.api.chat.ChatCompletionFunction
import feature.fetchurl.FetchURLFunction
import feature.googlesearch.GoogleSearchFunction
import feature.imagegen.ImageGenFunction
import feature.termdefinition.FetchTermDefinitionFunction
import feature.termdefinition.RegisterTermDefinitionFunction

class FunctionRepository {
    private val functions = listOf(
        FetchURLFunction(),
        FetchTermDefinitionFunction(),
        RegisterTermDefinitionFunction(),
        GoogleSearchFunction(),
        ImageGenFunction(),
    )

    fun toList(): List<ChatCompletionFunction> {
        return functions.map { it.definition }.toList()
    }

    fun firstOrNull(predicate: (OpenAIFunction) -> Boolean): OpenAIFunction? {
        return functions.firstOrNull { predicate(it) }
    }

    fun getByName(name: String): OpenAIFunction? {
        return functions.firstOrNull { it.name == name }
    }
}
