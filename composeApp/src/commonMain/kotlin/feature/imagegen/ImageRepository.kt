package feature.imagegen

import ConfigRepository
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.relativeTo
import kotlin.io.path.writeBytes

class ImageRepository(private val configRepository: ConfigRepository) {
    fun save(byteArray: ByteArray): String {
        val imageDirectory = imageDirectory()
        val path = imageDirectory.resolve(genFileName())
        path.writeBytes(byteArray)
        return path.relativeTo(imageDirectory).toString()
    }

    fun resolve(path: String): Path {
        return imageDirectory().resolve(path)
    }

    private fun imageDirectory(): Path {
        val path = configRepository.loadSettings().dataDirectoryPath.resolve("features/imagegen")
        path.toFile().mkdirs()
        return path
    }

    private fun genFileName(): String {
        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val charset = ('a'..'z') + ('0'..'9')
        val randomStr = (1..4).map { charset.random() }.joinToString("")
        return "$dateStr-$randomStr.jpg"
    }
}
