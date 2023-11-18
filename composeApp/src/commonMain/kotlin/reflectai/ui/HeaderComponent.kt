package reflectai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import reflectai.ConfigRepository
import reflectai.model.Config
import reflectai.model.aiModels
import java.text.NumberFormat

@Composable
fun HeaderComponent(
    chatViewModel: ChatViewModel,
    config: Config,
    configRepository: ConfigRepository,
    numberFormat: NumberFormat
) {
    Row {
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)) {
            Text(
                chatViewModel.targetAiModel.name + " (${chatViewModel.targetAiModel.maxTokens} max tokens)",
                modifier = Modifier.fillMaxWidth().clickable(onClick = { expanded = true }).background(
                    Color.Gray
                )
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                aiModels.forEach { aiModel ->
                    DropdownMenuItem(onClick = {
                        chatViewModel.targetAiModel = aiModel
                        expanded = false
                        config.defaultModelName = aiModel.name
                        configRepository.saveSettings(config)
                    }) {
                        Text(text = aiModel.name + " (" + numberFormat.format(aiModel.maxTokens) + " max tokens)")
                    }
                }
            }
        }
    }
}
