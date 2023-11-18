
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import model.ChatLog
import model.ChatLogMessage
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ChatLogRepository(private val objectMapper: ObjectMapper, private val zoneId: ZoneId, private val configRepository: ConfigRepository) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun saveConversations(logs: List<ChatLogMessage>, targetTime: Instant = Instant.now()) {
        val targetDateTime = targetTime.atZone(zoneId)
        val dateStr = targetDate(targetDateTime).toString()
        val directoryPath = configRepository.loadSettings().dataDirectoryPath
        val filePath = directoryPath.resolve("$dateStr.json")

        // ディレクトリが存在しない場合は作成
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath)
        }

        val json = objectMapper
            .writeValueAsBytes(ChatLog(logs = filterChatLogMessages(logs, targetDateTime)))

        // ファイルが小さくなるケースのときはエラーっぽいので諦める。
        // 基本的には起きないと思うんだよな。こうなったときは再起動したら治る。
        if (Files.exists(filePath)) {
            val currentSize = Files.size(filePath)
            if (currentSize > json.size) {
                logger.info("File size is too small: $filePath. Skipping the save operation: $currentSize < ${json.size}")
                showAlert("File size is too small. Skipping the save operation: $currentSize < ${json.size}")
                return
            }
        }

        // 対話をJSON形式でファイルに保存
        Files.write(filePath, json)
    }

    fun loadConversations(date: LocalDate = targetDate(ZonedDateTime.now())): ChatLog {
        println("loadConversations: $date")

        val filePath = configRepository.loadSettings().dataDirectoryPath.resolve("${date}.json")

        return try {
            // ファイルが存在しない場合はnullを返す
            if (!Files.exists(filePath)) {
                return ChatLog()
            }

            // ファイルの内容を読み込み、JSONからList<String>にデシリアライズ
            val content = String(Files.readAllBytes(filePath))
            objectMapper.readValue<ChatLog>(content)
        } catch (e: Exception) {
            println("Error reading the conversations: ${e.message}")
            return ChatLog()
        }
    }

    private fun filterChatLogMessages(
        chatLogMessages: List<ChatLogMessage>,
        targetDateTime: ZonedDateTime
    ): List<ChatLogMessage> {
        val (start, end) = if (targetDateTime.hour < 5) {
            // 深夜帯は前日扱い
            val start = targetDateTime.minusDays(1).withHour(5).toInstant()
            val end = targetDateTime.withHour(5).toInstant()
            start to end
        } else {
            val start = targetDateTime.withHour(5).toInstant()
            val end = targetDateTime.plusDays(1).withHour(5).toInstant()
            start to end
        }

        return chatLogMessages.filter {
            start <= it.timestamp && it.timestamp < end
        }
    }

    private fun targetDate(targetDateTime: ZonedDateTime): LocalDate {
        // 深夜帯は前日扱い
        if (targetDateTime.hour in 0..4) {
            return targetDateTime.toLocalDate().minusDays(1)
        }

        return targetDateTime.toLocalDate()
    }
}
