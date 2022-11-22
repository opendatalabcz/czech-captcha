package cz.opendatalab.captcha.datamanagement

import cz.opendatalab.captcha.TestImages
import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.imageio.ImageIO

internal class ImageUtilsTest {

    private val image = TestImages.IMAGE_1

    private fun getInputStream(): InputStream {
        return TestImages.getInputStream1()
    }

    @Test
    fun getImageFromInputStream_invalidInputStream_shouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageUtils.getImageFromInputStream(ByteArrayInputStream("invalid_image".toByteArray()))
        }
    }

    @Test
    fun getImageFromInputStream_successful() {
        val inputStream = getInputStream()
        val image = ImageUtils.getImageFromInputStream(inputStream)
        assertEquals(0, inputStream.available())
        assertEquals(682, image.width)
        assertEquals(1023, image.height)
    }

    @Test
    fun getInputStreamFromImage_unknownFormat_shouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageUtils.getInputStreamFromImage(image, "invalid-format")
        }
    }

    @Test
    fun getInputStreamFromImage_successful() {
        val inputStr = ImageUtils.getInputStreamFromImage(image, "jpg")
        assertTrue(0 < inputStr.available())
        inputStr.close()
    }

    @Test
    fun cropImage_successful() {
        val cropped = ImageUtils.cropImage(image, RelativeBoundingBox(0.0, 0.0, 0.5, 1.0))
        assertEquals(image.width / 2, cropped.width)
        assertEquals(image.height, cropped.height)
    }

    @Test
    fun cropImageToInputStream_unknownFormat_shouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageUtils.cropImageToInputStream(image, RelativeBoundingBox(0.0, 0.0, 0.5, 1.0), "invalid-format")
        }
    }

    @Test
    fun cropImageToInputStream_successful() {
        val inputStr = ImageUtils.cropImageToInputStream(image, RelativeBoundingBox(0.0, 0.0, 0.5, 1.0), "jpg")
        assertTrue(0 < inputStr.available())
        inputStr.close()
    }

    @Test
    fun padImageToSquare_successful() {
        val padded = ImageUtils.padImageToSquare(image)
        assertEquals(image.height, padded.width)
        assertEquals(image.height, padded.height)
    }

    @Test
    fun resizeInputStreamToMaxSize_noResizeNeeded_successful() {
        val resizedInputStream = ImageUtils.resizeInputStreamToMaxSize(getInputStream(), image.height, "jpg")
        val resized = resizedInputStream.use { ImageIO.read(it)!! }
        assertEquals(image.height, resized.height)
        assertEquals(image.width, resized.width)
    }

    @Test
    fun resizeInputStreamToMaxSize_resizeNeeded_successful() {
        val resizedInputStream = ImageUtils.resizeInputStreamToMaxSize(getInputStream(), image.height / 2, "jpg")
        val resized = resizedInputStream.use { ImageIO.read(it)!! }
        assertEquals(image.height / 2, resized.height)
        assertEquals(image.width / 2, resized.width)
    }
}