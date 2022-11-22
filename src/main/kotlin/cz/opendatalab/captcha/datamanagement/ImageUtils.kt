package cz.opendatalab.captcha.datamanagement

import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox
import cz.opendatalab.captcha.verification.entities.ImageSize
import org.imgscalr.Scalr
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.math.max

object ImageUtils {
    fun getImageFromInputStream(content: InputStream): BufferedImage {
        return content.use { ImageIO.read(it)
            ?: throw IllegalArgumentException("Cannot read image from the input stream.") }
    }

    fun getInputStreamFromImage(image: BufferedImage, format: String): InputStream {
        ByteArrayOutputStream().use {
            if (!ImageIO.write(image, format, it)) {
                throw IllegalArgumentException("Cannot write image in format $format.")
            }
            return ByteArrayInputStream(it.toByteArray())
        }
    }

    fun cropImage(image: BufferedImage, boxToCrop: RelativeBoundingBox): BufferedImage {
        val absoluteBoundingBox = boxToCrop.toAbsoluteBoundingBox(ImageSize(image.width, image.height))
        return Scalr.crop(
            image,
            absoluteBoundingBox.x,
            absoluteBoundingBox.y,
            absoluteBoundingBox.width,
            absoluteBoundingBox.height
        )
    }

    fun cropImageToInputStream(image: BufferedImage, boxToCrop: RelativeBoundingBox, format: String): InputStream {
        return getInputStreamFromImage(cropImage(image, boxToCrop), format)
    }

    fun padImageToSquare(image: BufferedImage): BufferedImage {
        val biggerSide = max(image.width, image.height)
        val outputImage = BufferedImage(biggerSide, biggerSide, BufferedImage.TYPE_INT_BGR)
        val g2d = outputImage.createGraphics()
        g2d.color = Color.BLACK
        g2d.fillRect(0, 0, biggerSide, biggerSide)
        g2d.drawImage(image, 0, 0, null)
        g2d.dispose()
        return outputImage
    }

    fun resizeInputStreamToMaxSize(inputStream: InputStream, maxSize: Int, format: String): InputStream {
        val image = getImageFromInputStream(inputStream)
        val resizedImage = Scalr.resize(image, maxSize)
        return getInputStreamFromImage(resizedImage, format)
    }
}