import chatgpt.ChatGPTService
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import feature.FunctionRepository
import java.time.ZoneId

class Container {
    val zoneId = ZoneId.systemDefault()
    val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
    val chatLogRepository = ChatLogRepository(objectMapper, zoneId)
    val chatGPTService = ChatGPTService()
    val configRepository = ConfigRepository()
    val functionRepository = FunctionRepository()
}
