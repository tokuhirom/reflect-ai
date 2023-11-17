package feature

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import model.ChatLogMessage

interface RendableFunction {
    @Composable
    fun render(item: ChatLogMessage, snackbarHostState: SnackbarHostState)
}
