package reflectai

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.plugins.logging.*
import reflectai.engine.llama.LlamaEngine
import reflectai.engine.llama.LlamaModelRepository
import reflectai.engine.openai.OpenAIEngine
import reflectai.engine.openai.OpenAIModelRepository
import reflectai.engine.openai.OpenAIProvider
import reflectai.feature.FunctionRepository
import reflectai.feature.fetchurl.FetchURLFunction
import reflectai.feature.googlesearch.GoogleSearchFunction
import reflectai.feature.imagegen.ImageGenFunction
import reflectai.feature.imagegen.ImageRepository
import reflectai.feature.termdefinition.FetchTermDefinitionFunction
import reflectai.feature.termdefinition.RegisterTermDefinitionFunction
import reflectai.feature.termdefinition.TermDefinitionRepository
import reflectai.ui.ChatViewModel
import java.time.ZoneId

class Container {
    val zoneId: ZoneId = ZoneId.systemDefault()
    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
    val configRepository = ConfigRepository()
    private val chatLogRepository = ChatLogRepository(objectMapper, zoneId, configRepository)
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
    private val openaiProvider = OpenAIProvider(configRepository)
    val openAIEngine = OpenAIEngine(functionRepository, configRepository, openaiProvider)

    val llamaModelRepository = LlamaModelRepository(configRepository)
    val openAIModelRepository = OpenAIModelRepository(configRepository)
    val llamaEngine = LlamaEngine(configRepository, llamaModelRepository)

    val modelRepositories = listOf(
        openAIModelRepository,
        llamaModelRepository,
    )

    val chatViewModel = ChatViewModel(
        openAIEngine, chatLogRepository, configRepository, modelRepositories,
        llamaEngine)
}
