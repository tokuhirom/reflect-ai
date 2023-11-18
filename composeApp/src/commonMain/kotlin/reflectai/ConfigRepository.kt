package reflectai

import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import reflectai.model.Config
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class ConfigRepository {
    private val objectMapper = jacksonMapperBuilder().build()

    // 設定を保存する
    fun saveSettings(config: Config, path: Path = configPath()) {
        val jsonData = objectMapper.writeValueAsString(config)

        if (path.parent.notExists()) {
            path.parent.toFile().mkdirs()
        }

        println("Saving $path: $jsonData")
        val tempFile = path.resolveSibling(path.name + ".tmp")
        tempFile.writeText(jsonData)
        Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    // 設定を読み込む
    fun loadSettings(path: Path = configPath()): Config {
        if (!path.exists()) return Config()
        val jsonData = path.toFile().readText()
        return objectMapper.readValue(jsonData)
    }

    private fun configPath(): Path {
        val settingsPath = System.getProperty("user.home") + "/.config/reflect-ai/config.json"
        return Paths.get(settingsPath)
    }
}
