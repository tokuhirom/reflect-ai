import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import model.Config

@Composable
fun ConfigurationDialog(
    config: Config,
    onSave: (String, String, String, String) -> Unit,
    onDialogClose: () -> Unit
) {
    var prompt by remember { mutableStateOf(config.prompt) }
    var openAIApiToken by remember { mutableStateOf(config.apiToken) }
    var googleApiKey by remember { mutableStateOf(config.googleSearchConfig.apiKey ?: "") }
    var googleSearchEngineId by remember { mutableStateOf(config.googleSearchConfig.searchEngineId ?: "") }

    DialogWindow(
        onCloseRequest = onDialogClose,
        title = "Configuration",
        state = rememberDialogState(width = 640.dp, height = 640.dp),
        resizable = true,
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize()
        ) {
            Text("OpenAI API token:")
            TextField(
                value = openAIApiToken,
                onValueChange = {
                    openAIApiToken = it
                }
            )
            Text("Prompt：", modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Google API key:")
            TextField(
                value = googleApiKey,
                onValueChange = { googleApiKey = it }
            )
            Text("Google Search Engine ID:")
            TextField(
                value = googleSearchEngineId,
                onValueChange = { googleSearchEngineId = it }
            )

            Button(
                onClick = {
                    onSave(prompt, openAIApiToken, googleApiKey, googleSearchEngineId)
                    onDialogClose()
                }
            ) {
                Text("Save")
            }
        }
    }
}
