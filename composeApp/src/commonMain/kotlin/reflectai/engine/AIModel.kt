package reflectai.engine

import java.text.NumberFormat

enum class AIModelType {
    OPENAI,
    LLAMA
}

interface AIModel {
    val type: AIModelType
    val name: String
    fun getLabel(numberFormat: NumberFormat): String
    fun countTokenAvailable(): Boolean = false
    fun countToken(text: String): Int
}
