import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aallam.openai.api.exception.InvalidRequestException
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import io.ktor.client.plugins.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import model.ChatLogMessage
import model.ChatLogRole
import org.slf4j.LoggerFactory
import javax.swing.JOptionPane


fun showAlert(message: String) {
    JOptionPane.showMessageDialog(null, message, "Alert", JOptionPane.WARNING_MESSAGE)
}



@Composable
fun App(chatGPTService: ChatGPTService, chatLogRepository: ChatLogRepository) {
    val logger = LoggerFactory.getLogger("App")
    val initialConversation = chatLogRepository.loadConversations().logs

    MaterialTheme {
        var message by remember { mutableStateOf("") }
        var conversation by remember { mutableStateOf(initialConversation) }
        val lazyListState = rememberLazyListState()

        fun sendMessage() {
            if (message.isNotEmpty()) {
                conversation += ChatLogMessage(ChatLogRole.User, message)
                message = ""

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        chatGPTService.sendMessage(conversation.toList().map { it.toChatMessage() }).let {
                            conversation += ChatLogMessage(ChatLogRole.AI, it)
                            chatLogRepository.saveConversations(conversation)
                        }
                    } catch (e: InvalidRequestException) {
                        showAlert("Error: ${e.message}")
                        chatLogRepository.saveConversations(conversation)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LaunchedEffect(Unit) {
                lazyListState.animateScrollToItem(maxOf(conversation.size - 1, 0))
            }

            LazyColumn(modifier = Modifier.weight(1f), state=lazyListState) {
                items(conversation) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when (item.role) {
                                    ChatLogRole.User -> Color.White
                                    ChatLogRole.AI -> Color.LightGray
                                }
                            )
                            .padding(16.dp)
                    ) {
                        SelectionContainer {
                            RichText(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Markdown(item.message)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Gray))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.Gray)
                        .onKeyEvent { keyEvent ->
                            when {
                                // Enterのみが押された場合、メッセージを送信
                                keyEvent.key == Key.Enter && keyEvent.isMetaPressed -> {
                                    sendMessage()
                                    true
                                }
                                else -> false
                            }
                        }
                )

                Button(onClick = {
                    sendMessage()
                }) {
                    Text("POST")
                }
            }
        }
    }
}
