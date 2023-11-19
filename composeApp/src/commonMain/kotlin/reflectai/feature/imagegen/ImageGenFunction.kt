package reflectai.feature.imagegen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.res.loadImageBitmap
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.Parameters
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.image.imageCreation
import com.aallam.openai.client.OpenAI
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import reflectai.ConfigRepository
import reflectai.feature.OpenAIFunction
import reflectai.feature.RendableFunction
import reflectai.model.ChatLogMessage
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.inputStream

data class ImageGenArgument(val prompt: String, val n: Int?, val size: String?)

class ImageGenFunction(private val objectMapper: ObjectMapper, private val configRepository: ConfigRepository, private val imageRepository: ImageRepository) : OpenAIFunction,
    RendableFunction {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val name = "image_gen"

    override fun dontSendToOpenAIAgain(): Boolean = true

    override val definition = ChatCompletionFunction(
        name,
        """Generate image using OpenAI's API.""".trimMargin(),
        Parameters.buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("prompt") {
                    put("type", "string")
                    put("description",
                        """Prompt to generate image.""".trimMargin()
                    )
                }
                putJsonObject("size") {
                    put("type", "string")
                    putJsonArray("enum") {
                        add("256x256")
                        add("512x512")
                        add("1024x1024")
                    }
                    put("description", """Size of images.""")
                }
                putJsonObject("n") {
                    put("type", "integer")
                    put("minimum", 1)
                    put("maximum", 10)
                    put("description", """Number of images.""")
                }
            }
            putJsonArray("required") {
                add("prompt")
            }
        }
    )

    @OptIn(BetaOpenAI::class)
    override suspend fun callFunction(
        argumentJson: String,
        remainTokens: Int,
    ): ChatMessage {
        val args = objectMapper.readValue<ImageGenArgument>(argumentJson)

        val apiToken = configRepository.loadSettings().apiToken
        val openai = OpenAI(apiToken!!)
        val images = openai.imageJSON(
            imageCreation {
                prompt = args.prompt
                // model selection is not supported by openai-kotlin 3.5.1
                // 3.6 may support it...
//                model = ModelId("dall-e-3")
                n = args.n ?: 1
                size = ImageSize(args.size ?: "1024x1024")
            }
        )
        val imagePaths = images.map {
            val image = Base64.getDecoder().decode(it.b64JSON)
            imageRepository.save(image)
        }

        return ChatMessage(
            role = ChatRole.Function,
            name = name,
            content = objectMapper.writeValueAsString(imagePaths)
        )
    }

    @OptIn(ExperimentalFoundationApi::class, DelicateCoroutinesApi::class)
    @Composable
    override fun render(item: ChatLogMessage, snackbarHostState: SnackbarHostState) {
        val images = try {
            objectMapper.readValue<List<String>>(item.message)
        } catch (e: Exception) {
            Text("Failed to render image: ${e.message}")
            return
        }
        images.forEach { imageFile ->
            logger.info("Reading $imageFile")
            val path = imageRepository.resolve(imageFile)
            path.inputStream().use { inputStream ->
                val bitmap = loadImageBitmap(inputStream)
                // https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Mouse_Events
                Image(
                    bitmap = bitmap, contentDescription = null,
                    modifier = androidx.compose.ui.Modifier
                        .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                            val img = ImageIO.read(path.toFile())
                            val transferableImage = TransferableImage(img)
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(transferableImage, null)

                            GlobalScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Image copied to clipboard!",
                                    actionLabel = "Dismiss"
                                )
                            }
                        }
                )
            }
        }
    }
}

private class TransferableImage(val img: BufferedImage) : Transferable {
    override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor

    override fun getTransferData(flavor: DataFlavor) =
        if (flavor == DataFlavor.imageFlavor && isDataFlavorSupported(flavor)) img
        else throw UnsupportedOperationException("Not supported flavor")
}

