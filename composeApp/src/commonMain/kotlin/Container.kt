import chatgpt.ChatGPTService
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import feature.FunctionRepository
import feature.fetchurl.FetchURLFunction
import feature.googlesearch.GoogleSearchFunction
import feature.imagegen.ImageGenFunction
import feature.imagegen.ImageRepository
import feature.termdefinition.FetchTermDefinitionFunction
import feature.termdefinition.RegisterTermDefinitionFunction
import feature.termdefinition.TermDefinitionRepository
import io.ktor.client.plugins.logging.*
import java.time.ZoneId

class Container {
    val zoneId: ZoneId = ZoneId.systemDefault()
    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
    val configRepository = ConfigRepository()
    val chatLogRepository = ChatLogRepository(objectMapper, zoneId, configRepository)
    private val ktorClient = io.ktor.client.HttpClient() {
        install(Logging)
    }

    private val termDefinitionRepository = TermDefinitionRepository(configRepository)
    val functionRepository = FunctionRepository(
        listOf(
            FetchURLFunction(objectMapper, ktorClient),
            FetchTermDefinitionFunction(objectMapper, termDefinitionRepository),
            RegisterTermDefinitionFunction(objectMapper, termDefinitionRepository),
            GoogleSearchFunction(configRepository, objectMapper, ktorClient),
            ImageGenFunction(objectMapper, configRepository, ImageRepository(configRepository)),
        )
    )
    val chatGPTService = ChatGPTService(functionRepository)
}
