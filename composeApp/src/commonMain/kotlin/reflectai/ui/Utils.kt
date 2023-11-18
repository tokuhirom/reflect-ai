package reflectai.ui

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.net.URI
import javax.swing.JOptionPane

fun openUrl(url: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}

fun copyToClipboard(text: String) {
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val stringSelection = StringSelection(text)
    clipboard.setContents(stringSelection, stringSelection)
}

fun String.truncateAt(maxLength: Int): String {
    if (this.length <= maxLength) return this
    return this.take(maxLength) + "..."
}

fun showAlert(message: String) {
    JOptionPane.showMessageDialog(null, message, "Alert", JOptionPane.WARNING_MESSAGE)
}
