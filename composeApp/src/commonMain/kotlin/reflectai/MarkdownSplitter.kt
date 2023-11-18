package reflectai

enum class MarkdownBlocKType {
    TEXT,
    CODE
}

class MarkdownBlock(val type: MarkdownBlocKType, val text: String, val lang: String? = null)

fun splitIntoMarkdownBlocks(inputText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val codeBlockPattern = """```([a-z0-9A-Z]+)?\n(.*?)\n```""".toRegex(RegexOption.DOT_MATCHES_ALL)
    var lastEnd = 0

    codeBlockPattern.findAll(inputText).forEach { matchResult ->
        // Add the text block before this code block (if any)
        if (matchResult.range.first != lastEnd) {
            val textBlock =
                MarkdownBlock(MarkdownBlocKType.TEXT, inputText.substring(lastEnd, matchResult.range.first))
            blocks.add(textBlock)
        }

        // Add this code block
        val codeBlock = MarkdownBlock(
            MarkdownBlocKType.CODE,
            matchResult.groupValues[2],
            matchResult.groupValues[1]
        )
        blocks.add(codeBlock)

        lastEnd = matchResult.range.last + 1
    }

    // Add the remaining text block (if any)
    if (lastEnd != inputText.length) {
        val textBlock = MarkdownBlock(MarkdownBlocKType.TEXT, inputText.substring(lastEnd))
        blocks.add(textBlock)
    }

    return blocks
}
