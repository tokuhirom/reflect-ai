import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

@Composable
fun SettingsDialog(
    initialPrompt: String,
    onSave: (String) -> Unit,
    onDialogClose: () -> Unit
) {
    var prompt by remember { mutableStateOf(initialPrompt) }

    DialogWindow(
        onCloseRequest = onDialogClose,
        title = "設定",
        state = rememberDialogState(width = 640.dp, height = 640.dp),
        resizable = true,
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize()
        ) {
            Text("ChatGPTのプロンプト設定：", modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
            )
            Button(
                onClick = {
                    onSave(prompt)
                    onDialogClose()
                }
            ) {
                Text("保存")
            }
        }
    }
}
