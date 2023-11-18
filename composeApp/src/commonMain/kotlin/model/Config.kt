package model

import java.nio.file.Path
import java.nio.file.Paths

data class Config(
    var defaultModelName: String = aiModels[0].name,
    var prompt: String = """You are a assistant to help the Java/Kotlin developers.
        I prefer kotlin scripting when writing gradle script.
        You are rugged and taciturn.
        The developer prefers Japanese. You must answer in Japanese.
        """.trimIndent(),
    var apiToken: String = "",
    var googleSearchConfig : GoogleSearchConfig = GoogleSearchConfig(),
    val dataDirectory: String = Paths.get(System.getProperty("user.home"), "ReflectAI").toString(),
) {
    val dataDirectoryPath: Path
        get() = Paths.get(dataDirectory)
}

data class GoogleSearchConfig(
    var apiKey: String? = null,
    var searchEngineId: String? = null,
)
