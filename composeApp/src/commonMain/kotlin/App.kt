import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.ChatLogMessage
import model.ChatLogRole
import model.aiModels
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.text.NumberFormat
import java.time.ZoneId
import java.util.*
import javax.swing.JOptionPane

fun String.truncateAt(maxLength: Int): String {
    if (this.length <= maxLength) return this
    return this.take(maxLength) + "..."
}

fun showAlert(message: String) {
    JOptionPane.showMessageDialog(null, message, "Alert", JOptionPane.WARNING_MESSAGE)
}


fun openUrl(url: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}

suspend fun LazyListState.scrollToEnd() {
    // スクロールする必要があるアイテムがない場合は何もしない
    if (layoutInfo.totalItemsCount == 0) {
        return
    }

    // リストの最後のアイテムのインデックスを取得
    val lastIndex = layoutInfo.totalItemsCount - 1

    // リストの最後のアイテムまでアニメーション付きでスクロールする
    animateScrollToItem(index = lastIndex)

    // 最後のアイテムのオフセットを計算し、さらにスクロールして末尾に到達させる
    val lastItemInfo = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastIndex }
    lastItemInfo?.let {
        val scrollOffset = it.size - (layoutInfo.viewportEndOffset - it.offset)
        if (scrollOffset > 0) {
            animateScrollBy(scrollOffset.toFloat())
        }
    }
}

fun extractCodeBlocks(mkdn: String): List<Pair<String?, String>> {
    val pattern = Regex("```([a-zA-Z0-9]+\n)?(.*?)(?<=\\n)```", RegexOption.DOT_MATCHES_ALL)
    return pattern.findAll(mkdn).map { it.groupValues[1] to it.groupValues[2] }.toList()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(
    chatGPTService: ChatGPTService,
    chatLogRepository: ChatLogRepository,
    zoneId: ZoneId,
    configRepository: ConfigRepository
) {
    val initialConversation = chatLogRepository.loadConversations().logs
    val config = configRepository.loadSettings()
    var targetAiModel = aiModels.firstOrNull { it.name == config.defaultModelName }
        ?: aiModels.first()
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    MaterialTheme {
        var message by remember { mutableStateOf(TextFieldValue("")) }
        var conversation by remember { mutableStateOf(initialConversation) }
        val lazyListState = rememberLazyListState()

        fun sendMessage() {
            if (message.text.isNotEmpty()) {
                conversation += ChatLogMessage(ChatLogRole.User, message.text)
                message = TextFieldValue("")

                CoroutineScope(Dispatchers.Main).launch {
                    var current = ChatLogMessage(ChatLogRole.AI, "")
                    conversation += current

                    fun updateMessage(msg: String, role: ChatLogRole) {
                        current = ChatLogMessage(
                            role,
                            current.message + msg,
                            current.id,
                            current.timestamp
                        )
                        conversation = conversation.filter { it.id != current.id } + current
                    }

                    try {
                        chatGPTService.sendMessage(
                            config.apiToken,
                            targetAiModel,
                            config.prompt,
                            conversation.toList()
                                .filter { it.role != ChatLogRole.Error }
                                .map { it.toChatMessage() }
                        ).collect {
                            updateMessage(it, ChatLogRole.AI)
                            chatLogRepository.saveConversations(conversation)
                        }
                    } catch (e: Exception) {
                        updateMessage(e.message ?: "Got an error : $e", ChatLogRole.Error)
                        chatLogRepository.saveConversations(conversation)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LaunchedEffect(conversation) {
                delay(100) // conversation のレンダリングが終わってからスクロール位置を調整すべき
                lazyListState.scrollToEnd()
            }

            Row {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)) {
                    Text(
                        targetAiModel.name + " (${targetAiModel.maxTokens} max tokens)",
                        modifier = Modifier.fillMaxWidth().clickable(onClick = { expanded = true }).background(
                            Color.Gray
                        )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        aiModels.forEachIndexed { index, aiModel ->
                            DropdownMenuItem(onClick = {
                                targetAiModel = aiModel
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

            LazyColumn(modifier = Modifier.weight(1f), state = lazyListState) {
                items(conversation) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when (item.role) {
                                    ChatLogRole.User -> Color.White
                                    ChatLogRole.AI -> Color.LightGray
                                    ChatLogRole.Error -> Color.LightGray
                                }
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.timestamp.atZone(zoneId).toString())
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        // Copy text to clipboard
                                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                        val stringSelection = StringSelection(item.message)
                                        clipboard.setContents(stringSelection, stringSelection)
                                    }, colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color.Gray,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("\uD83D\uDCCB")
                                }
                                extractCodeBlocks(item.message).forEachIndexed { idx, (lang, codeBlock) ->
                                    Button(
                                        onClick = {
                                            // Copy text to clipboard
                                            val clipboard: Clipboard =
                                                Toolkit.getDefaultToolkit().systemClipboard
                                            val stringSelection = StringSelection(codeBlock)
                                            clipboard.setContents(stringSelection, stringSelection)
                                        }, colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color.Gray,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("\uD83D\uDCCB(${lang ?: "Code"}($idx)")
                                    }
                                }
                            }

                            if (true) {
                                val annotatedText = makeMarkdownAnnotatedString(item.message)

                                // https://github.com/JetBrains/compose-multiplatform/issues/1450
                                var lastLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }
                                BasicText(
                                    text = annotatedText,
                                    modifier = Modifier.padding(16.dp)
                                        .onPointerEvent(PointerEventType.Release) {
                                            val offset =
                                                lastLayoutResult?.getOffsetForPosition(it.changes.first().position)
                                                    ?: 0
                                            annotatedText.getStringAnnotations(
                                                tag = "URL",
                                                start = offset,
                                                end = offset
                                            )
                                                .firstOrNull()?.let { annotation ->
                                                    openUrl(annotation.item)
                                                }
                                        },
                                    style = TextStyle(
                                        color = when (item.role) {
                                            ChatLogRole.User -> Color.Black
                                            ChatLogRole.AI -> Color.Black
                                            ChatLogRole.Error -> Color(0xa0003000)
                                        }
                                    ),
                                    onTextLayout = { layoutResult ->
                                        lastLayoutResult = layoutResult
                                    }
                                )
                            } else {
                                SelectionContainer {
                                    RichText(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Markdown(item.message)
                                    }
                                }
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

private fun makeMarkdownAnnotatedString(inputText: String): AnnotatedString {
    val urlPattern = """https?://[^\s\]]+""".toRegex()
    val urlMatches = urlPattern.findAll(inputText).toList()

    val annotatedText = buildAnnotatedString {
        urlMatches.fold(0, { lastEnd, matchResult ->
            val matchStart = matchResult.range.first
            val matchEnd = matchResult.range.last

            append(inputText.substring(lastEnd, matchStart))

            pushStringAnnotation(
                tag = "URL",
                annotation = inputText.substring(matchStart, matchEnd + 1)
            )

            withStyle(
                style = SpanStyle(
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(inputText.substring(matchStart, matchEnd + 1))
            }

            pop()

            matchEnd + 1
        })
            .let { append(inputText.substring(it)) }  // Add the rest of the text after the last match
    }
    return annotatedText
}
