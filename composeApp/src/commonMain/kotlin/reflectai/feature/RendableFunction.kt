package reflectai.feature

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import reflectai.model.ChatLogMessage

interface RendableFunction {
    @Composable
    fun render(item: ChatLogMessage, snackbarHostState: SnackbarHostState)
}
