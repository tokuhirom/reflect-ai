package feature.termdefinition

import ConfigRepository
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class WordMapping(val words: Map<String, String>)

class TermDefinitionRepository(private val configRepository: ConfigRepository) {
    private val objectMapper = jacksonObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)

    private fun filePath(): Path {
        val path = configRepository.loadSettings().dataDirectoryPath.resolve("features/termdefinition/words.json")
        path.parent.toFile().mkdirs()
        return path
    }

    fun addWord(word: String, definition: String) {
        val path = filePath()
        val newWords = if (path.toFile().exists()) {
            val words = objectMapper.readValue<WordMapping>(path.readText())
            words.words.toMutableMap()
        } else {
            mutableMapOf()
        }

        // 冗長すぎる場合には、要約する必要がある。しきい値を超えた長さになったときに、ChatGPT で要約させれば良さそう。
        newWords[word] = if (newWords.containsKey(word)) {
            newWords[word] + "\n" + definition
        } else {
            definition
        }

        val tempFile = path.resolveSibling(path.name + ".tmp")
        tempFile.writeText(objectMapper.writeValueAsString(WordMapping(newWords)))
        Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    fun getWord(word: String): String {
        val path = filePath()
        val readValue = objectMapper.readValue<WordMapping>(path.readText())
        return if (readValue.words.contains(word)) {
            readValue.words[word]!!
        } else {
            "Unknown word: $word"
        }
    }
}
