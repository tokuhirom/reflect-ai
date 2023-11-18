package feature

import com.aallam.openai.api.chat.ChatCompletionFunction

class FunctionRepository(private val functions: List<OpenAIFunction>) {

    fun toList(): List<ChatCompletionFunction> {
        return functions.map { it.definition }.toList()
    }

    fun getByName(name: String): OpenAIFunction? {
        return functions.firstOrNull { it.name == name }
    }
}
