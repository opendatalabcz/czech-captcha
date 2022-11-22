package cz.opendatalab.captcha.datamanagement.objectdetection

import cz.opendatalab.captcha.TestImages
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.util.FileSystemUtils
import java.nio.file.Paths

internal class ObjectDetectorKotlinDLTest {

    private val objectDetector = ObjectDetectorKotlinDL(cachePath.toAbsolutePath().toString())

    @Test
    fun `detect person detected`() {
        val detected = objectDetector.detect(TestImages.IMAGE_1)
        assertTrue(detected.isNotEmpty())
        assertTrue(detected.stream().anyMatch{ obj -> obj.label.equals("person") })
    }

    @Test
    fun `getSupportedLabels not empty`() {
        assertTrue(objectDetector.getSupportedLabels().isNotEmpty())
    }

    companion object {
        private val cachePath = Paths.get(System.getProperty("java.io.tmpdir"), "czech-captcha")

        @AfterAll
        @JvmStatic
        private fun removeCache() {
            FileSystemUtils.deleteRecursively(cachePath)
        }
    }
}