package reflectai.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import reflectai.ConfigRepository
import reflectai.engine.ModelRepository
import reflectai.feature.FunctionRepository
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun App(
    zoneId: ZoneId,
    configRepository: ConfigRepository,
    funcitonRepository: FunctionRepository,
    chatViewModel: ChatViewModel,
    modelRepositories: List<ModelRepository>,
) {
    val config = configRepository.loadSettings()
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    MaterialTheme {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HeaderComponent(chatViewModel, config, configRepository, numberFormat, modelRepositories)

            val snackbarHostState = SnackbarHostState()
            SnackbarHost(hostState = snackbarHostState)

            ConversationComponent(
                chatViewModel,
                funcitonRepository,
                snackbarHostState,
                zoneId,
                dateTimeFormatter
            )

            Spacer(modifier = Modifier.height(8.dp))

            MessageInputComponent(chatViewModel)
        }
    }
}

