import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.ZoneId

fun main() = application {
    val apiKey = System.getenv("OPENAI_API_KEY") ?: throw RuntimeException("OPENAI_API_KEY is not set.")
    val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(JavaTimeModule())
    val chatLogRepository = ChatLogRepository(objectMapper, ZoneId.systemDefault())
    val chatGPTService = ChatGPTService(apiKey,
        "You are a assistant to help the Java/Kotlin developers and/or engineering manager." +
                "あなたはチャットの一参加者なのではあなたは明示的に質問された場合ととても良いアドバイスが思いついた場合以外は返答する必要はありません" +
                "どちらでもない場合は *nods* とだけ答えてください。" +
                "The developer prefers Japanese. You must answer in Japanese.")

    Window(onCloseRequest = ::exitApplication, title = "ReflectAI") {
        App(chatGPTService, chatLogRepository)
    }
}

//@Preview
//@Composable
//fun AppDesktopPreview() {
//    App()
//}
