package reflectai.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.nio.file.Path
import java.nio.file.Paths

data class Config(
    var defaultModelName: String? = null,
    var prompt: String = """You are a assistant to help the Java/Kotlin developers.
        I prefer kotlin scripting when writing gradle script.
        You are rugged and taciturn.
        The developer prefers Japanese. You must answer in Japanese.
        """.trimIndent(),
    var apiToken: String? = null,
    var googleSearchConfig : GoogleSearchConfig = GoogleSearchConfig(),
    val dataDirectory: String = Paths.get(System.getProperty("user.home"), "ReflectAI").toString(),
) {
    @get:JsonIgnore
    val dataDirectoryPath: Path
        get() = Paths.get(dataDirectory)
}

data class GoogleSearchConfig(
    var apiKey: String? = null,
    var searchEngineId: String? = null,
)
