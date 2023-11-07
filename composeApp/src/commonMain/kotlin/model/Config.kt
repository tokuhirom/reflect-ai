package model

data class Config(
    var defaultModelName: String = aiModels[0].name,
    var prompt: String = """You are a assistant to help the Java/Kotlin developers.
        I prefer kotlin scripting when writing gradle script.
        You are rugged and taciturn.
        The developer prefers Japanese. You must answer in Japanese.
        """.trimIndent(),
    var apiToken: String = ""
)
