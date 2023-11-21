package reflectai.engine

import java.text.NumberFormat

enum class AIModelType {
    OPENAI,
    LLAMA
}

sealed interface AIModel {
    val type: AIModelType
    val name: String
    fun getLabel(numberFormat: NumberFormat): String
}
