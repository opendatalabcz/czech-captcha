package cz.opendatalab.captcha.objectdetection

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.util.FileSystemUtils
import java.nio.file.Paths
import javax.imageio.ImageIO

internal class ObjectDetectorKotlinDLTest {

    private val objectDetector = ObjectDetectorKotlinDL(cachePath.toAbsolutePath().toString())

    @Test
    fun `detect person detected`() {
        Thread.currentThread().contextClassLoader.getResourceAsStream(testImageName).use {
            val image = ImageIO.read(it)
            val detected = objectDetector.detect(image)
            assertTrue(detected.isNotEmpty())
            assertTrue(detected.stream().anyMatch{ obj -> obj.label.equals("person") })
        }
    }

    @Test
    fun `getSupportedLabels not empty`() {
        assertTrue(objectDetector.getSupportedLabels().isNotEmpty())
    }

    companion object {
        private val cachePath = Paths.get(System.getProperty("java.io.tmpdir"), "czech-captcha")
        private const val testImageName = "test_image.jpg"

        @AfterAll
        @JvmStatic
        private fun removeCache() {
            FileSystemUtils.deleteRecursively(cachePath)
        }
    }
}