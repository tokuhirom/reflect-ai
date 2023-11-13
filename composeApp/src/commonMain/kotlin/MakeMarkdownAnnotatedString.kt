import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

fun makeMarkdownAnnotatedString(inputText: String): AnnotatedString {
    val pattern = """(https?://[^\s\]]+)|(\*\*.*?\*\*)""".toRegex()
    val matches = pattern.findAll(inputText).toList()

    val annotatedText = buildAnnotatedString {
        matches.fold(0, { lastEnd, matchResult ->
            val matchStart = matchResult.range.first
            val matchEnd = matchResult.range.last

            val urlGroup = matchResult.groups[1]
            val boldGroup = matchResult.groups[2]
            println("CURRENT: $urlGroup, $boldGroup")

            append(inputText.substring(lastEnd, matchStart))

            if (urlGroup != null) {
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
            } else {
                pushStringAnnotation(
                    tag = "BOLD",
                    annotation = inputText.substring(matchStart, matchEnd + 1)
                )
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                    )
                ) {
                    append(inputText.substring(matchStart + 2, matchEnd + 1 - 2))
                }
            }

            pop()

            matchEnd + 1
        })
            .let { append(inputText.substring(it)) }  // Add the rest of the text after the last match
    }
    return annotatedText
}
