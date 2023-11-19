package reflectai
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.kherud.llama.LlamaModel
import reflectai.ui.App

fun main() = application {
    val container = Container()
    val config = container.configRepository.loadSettings()

    var showSettingsDialog by remember { mutableStateOf(false) }

    LlamaModel.setLogger {  level, message ->
        println("[$level] $message")
    }

    Window(onCloseRequest = ::exitApplication, title = "ReflectAI") {
        App(
            container.zoneId,
            container.configRepository,
            container.functionRepository,
            container.chatViewModel,
            container.modelRepositories,
        )

        if (showSettingsDialog) {
            ConfigurationDialog(
                config,
                onSave = { prompt, apiToken, googleApiKey, googleSearchEngineId ->
                    config.prompt = prompt
                    config.apiToken = apiToken
                    config.googleSearchConfig.apiKey = googleApiKey
                    config.googleSearchConfig.searchEngineId = googleSearchEngineId
                    container.configRepository.saveSettings(config)
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
