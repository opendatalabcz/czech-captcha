package cz.opendatalab.captcha.datamanagement.objectdetection

import cz.opendatalab.captcha.TestImages
import mu.KotlinLogging
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
        private val logger = KotlinLogging.logger {}

        @AfterAll
        @JvmStatic
        private fun removeCache() {
            logger.info("Deleting directory with object detection model after tests.")
            try {
                FileSystemUtils.deleteRecursively(cachePath)
            } catch (e: Exception) {
                logger.warn("Could not delete directory with OD cache.", e)
            }
        }
    }
}