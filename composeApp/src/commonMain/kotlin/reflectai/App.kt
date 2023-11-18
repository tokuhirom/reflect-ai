package reflectai

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
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import reflectai.chatgpt.ChatGPTService
import reflectai.chatgpt.FunctionChatCompletionStreamItem
import reflectai.chatgpt.StringChatCompletionStreamItem
import reflectai.feature.FunctionRepository
import reflectai.feature.RendableFunction
import reflectai.model.ChatLogMessage
import reflectai.model.ChatLogRole
import reflectai.model.aiModels
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.JOptionPane

fun String.truncateAt(maxLength: Int): String {
    if (this.length <= maxLength) return this
    return this.take(maxLength) + "..."
}

fun showAlert(message: String) {
    JOptionPane.showMessageDialog(null, message, "Alert", JOptionPane.WARNING_MESSAGE)
}

fun copyToClipboard(text: String) {
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val stringSelection = StringSelection(text)
    clipboard.setContents(stringSelection, stringSelection)
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

@Composable
fun App(
    chatGPTService: ChatGPTService,
    chatLogRepository: ChatLogRepository,
    zoneId: ZoneId,
    configRepository: ConfigRepository,
    funcitonRepository: FunctionRepository,
) {
    val logger = LoggerFactory.getLogger("App")
    val initialConversation = chatLogRepository.loadConversations().logs
    val config = configRepository.loadSettings()
    var targetAiModel = aiModels.firstOrNull { it.name == config.defaultModelName }
        ?: aiModels.first()
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    MaterialTheme {
        var message by remember { mutableStateOf(TextFieldValue("")) }
        var conversation by remember { mutableStateOf(initialConversation) }
        var progressIndicator by remember { mutableStateOf(TextFieldValue("")) }
        val lazyListState = rememberLazyListState()

        fun sendMessage() {
            if (message.text.isNotEmpty()) {
                conversation += ChatLogMessage(ChatLogRole.User, message.text)
                message = TextFieldValue("")

                CoroutineScope(Dispatchers.Main).launch {
                    var current = ChatLogMessage(ChatLogRole.AI, "", inProgress = true)
                    conversation += current

                    fun updateMessage(msg: String, role: ChatLogRole, inProgress: Boolean = false) {
                        current = ChatLogMessage(
                            role,
                            current.message + msg,
                            current.id,
                            inProgress = inProgress,
                            timestamp = current.timestamp
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
                                .map { it.toChatMessage() },
                        ) {
                            progressIndicator = TextFieldValue(it)
                        }.onCompletion {
                            println("chatCompletions complete.")
                            updateMessage("", ChatLogRole.AI, false)
                            chatLogRepository.saveConversations(conversation)
                        }.collect {item ->
                            when (item) {
                                is StringChatCompletionStreamItem -> {
                                    updateMessage(item.content, ChatLogRole.AI, true)
                                }

                                is FunctionChatCompletionStreamItem -> {
                                    conversation = conversation.filter { it.id != current.id } + ChatLogMessage(
                                        ChatLogRole.Function,
                                        item.chatMessage.content!!,
                                        name = item.chatMessage.name)  + current
                                }
                            }
                            chatLogRepository.saveConversations(conversation)
                        }
                    } catch (e: Exception) {
                        logger.error("Got an error : $e", e)
                        updateMessage(if (e.message != null) {
                            "${e.javaClass.canonicalName} ${e.message}"
                        } else {
                            "Got an error : $e"
                                                                                }, ChatLogRole.Error)
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
                        aiModels.forEach { aiModel ->
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

            val snackbarHostState = SnackbarHostState()

            SnackbarHost(
                hostState = snackbarHostState,
            )

            LazyColumn(modifier = Modifier.weight(1f), state = lazyListState) {
                items(conversation) { item ->
                    if (item.role == ChatLogRole.Function) {
                        val function = funcitonRepository.getByName(item.name!!) ?: return@items
                        if (function is RendableFunction) {
                            function.render(item, snackbarHostState)
                        }
                        return@items
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (item.inProgress) {
                                    Color(0xFFccccEE)
                                } else {
                                    when (item.role) {
                                        ChatLogRole.User -> Color.White
                                        ChatLogRole.AI -> Color.LightGray
                                        ChatLogRole.Error -> Color.LightGray
                                        ChatLogRole.Function -> Color.LightGray
                                    }
                                }
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.timestamp.atZone(zoneId).format(dateTimeFormatter),
                                    style = TextStyle(
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                )
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
                            }

                            if (progressIndicator.text.isNotEmpty() && item.inProgress) {
                                Text(progressIndicator.text, color = Color.Gray)
                            }

                            renderTextBlock(item)
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

@Composable
private fun renderTextBlock(item: ChatLogMessage) {
    splitIntoMarkdownBlocks(item.message).forEach { block ->
        when (block.type) {
            MarkdownBlocKType.TEXT -> {
                renderTextBlock(block, item.role)
            }

            MarkdownBlocKType.CODE -> {
                renderCodeBlock(block)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun renderTextBlock(block: MarkdownBlock, role: ChatLogRole) {
    val annotatedText = makeMarkdownAnnotatedString(block.text)

    // https://github.com/JetBrains/compose-multiplatform/issues/1450
    SelectionContainer {
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
                    ).firstOrNull()?.let { annotation ->
                        openUrl(annotation.item)
                    }
                },
            style = TextStyle(
                color = when (role) {
                    ChatLogRole.User -> Color.Black
                    ChatLogRole.AI -> Color.Black
                    ChatLogRole.Function -> Color.Gray
                    ChatLogRole.Error -> Color(0xa0003000)
                }
            ),
            onTextLayout = { layoutResult ->
                lastLayoutResult = layoutResult
            }
        )
    }
}

@Composable
private fun renderCodeBlock(block: MarkdownBlock) {
    Column {
        Row(
            modifier = Modifier.background(Color(0xFF333333))
                .padding(8.dp)
        ) {
            Text(text = block.lang ?: "Code", color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    // Copy text to clipboard
                    val clipboard: Clipboard =
                        Toolkit.getDefaultToolkit().systemClipboard
                    val stringSelection = StringSelection(block.text)
                    clipboard.setContents(stringSelection, stringSelection)
                }, colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text("Copy")
            }
        }

        SelectionContainer(
            modifier = Modifier.background(Color.Black)
                .fillMaxWidth()
        ) {
            BasicText(
                text = block.text,
                modifier = Modifier.padding(16.dp),
                style = TextStyle(
                    color = Color.White,
                )
            )
        }
    }
}
