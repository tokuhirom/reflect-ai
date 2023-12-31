package reflectai.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import reflectai.markdown.MarkdownBlocKType
import reflectai.markdown.MarkdownBlock
import reflectai.feature.FunctionRepository
import reflectai.feature.RendableFunction
import reflectai.markdown.makeMarkdownAnnotatedString
import reflectai.model.ChatLogMessage
import reflectai.model.ChatLogRole
import reflectai.markdown.splitIntoMarkdownBlocks
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.ConversationComponent(
    chatViewModel: ChatViewModel,
    funcitonRepository: FunctionRepository,
    snackbarHostState: SnackbarHostState,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter?
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(chatViewModel.conversation) {
        delay(100) // conversation のレンダリングが終わってからスクロール位置を調整すべき
        lazyListState.scrollToEnd()
    }

    // no scrollbar support on it. https://stackoverflow.com/questions/66341823/jetpack-compose-scrollbars
    // https://developer.android.com/jetpack/androidx/compose-roadmap?hl=en
    LazyColumn(modifier = Modifier.weight(1f), state = lazyListState) {
        items(chatViewModel.conversation) { item ->
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

                        if (chatViewModel.targetAiModel.countTokenAvailable()) {
                            Text(text = "${chatViewModel.targetAiModel.countToken(item.message)} tokens",
                                modifier = Modifier.padding(end = 8.dp),
                                style = TextStyle(
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                ))
                        }

                        Text(text = "Copy", modifier = Modifier.onClick {
                            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                            val stringSelection = StringSelection(item.message)
                            clipboard.setContents(stringSelection, stringSelection)
                        }, style = TextStyle(
                            fontSize = 12.sp,
                            color = Color.Gray
                        ))
                    }

                    if (chatViewModel.progressIndicator.text.isNotEmpty() && item.inProgress) {
                        Text(chatViewModel.progressIndicator.text, color = Color.Gray)
                    }

                    renderTextBlock(item)
                }
            }
            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Gray))
        }
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
