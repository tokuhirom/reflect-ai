import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import chatgpt.ChatGPTService
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.ZoneId

fun main() = application {
    val zoneId = ZoneId.systemDefault()
    println("ZoneId: $zoneId")
    val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
    val chatLogRepository = ChatLogRepository(objectMapper, zoneId)
    val chatGPTService = ChatGPTService()
    val configRepository = ConfigRepository()
    val config = configRepository.loadSettings()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Window(onCloseRequest = ::exitApplication, title = "ReflectAI") {
        App(chatGPTService, chatLogRepository, zoneId, configRepository)

        if (showSettingsDialog) {
            ConfigurationDialog(
                config,
                onSave = { prompt, apiToken, googleApiKey, googleSearchEngineId ->
                    config.prompt = prompt
                    config.apiToken = apiToken
                    config.googleSearchConfig.apiKey = googleApiKey
                    config.googleSearchConfig.searchEngineId = googleSearchEngineId
                    configRepository.saveSettings(config)
                },
                onDialogClose = {
                    showSettingsDialog = false
                })
        }

        MenuBar {
            this.Menu("Misc") {
                Item("Configuration", shortcut = KeyShortcut(Key.Comma, meta = true), onClick = {
                    println("Clicked...")
                    showSettingsDialog = true
                })
            }
        }
    }
}
