package reflectai.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp

@Composable
fun MessageInputComponent(chatViewModel: ChatViewModel) {
    Row {
        TextField(
            value = chatViewModel.message,
            onValueChange = { chatViewModel.message = it },
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color.Gray)
                .onKeyEvent { keyEvent ->
                    when {
                        // Enterのみが押された場合、メッセージを送信
                        keyEvent.key == Key.Enter && keyEvent.isMetaPressed -> {
                            chatViewModel.sendMessage()
                            true
                        }

                        else -> false
                    }
                }
        )

        Button(onClick = {
            chatViewModel.sendMessage()
        }) {
            Text("Submit")
        }
    }
}
