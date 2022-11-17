package cz.opendatalab.captcha.datamanagement.objectdetection

import cz.opendatalab.captcha.TestConfiguration
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectRepositoryType
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

internal class ObjectDetectionServiceTest {

    private val objectDetector: ObjectDetector = mockk()
    private val objectService: ObjectService = mockk()

    private val objectDetectionService = ObjectDetectionService(objectService, objectDetector)

    private val fileId = "id"
    private val user = "user"
    private val jpg = "jpg"
    private val person = "person"
    private val car = "car"
    private val probability1 = 0.9
    private val probability2 = 0.7
    private val detectedObjects = listOf(
        DetectedObject(person, probability1, AbsoluteBoundingBox(10, 10, 10, 10)),
        DetectedObject(car, probability2, AbsoluteBoundingBox(20, 20, 20, 20))
    )
    private val originalName = "file.jpg"
    private val objectStorageInfo = ObjectStorageInfo(fileId, originalName, user, "path/to/$originalName", ObjectRepositoryType.FILESYSTEM)

    @Test
    fun getSupportedLabels() {
        val labels = setOf("person", "car")
        every { objectDetector.getSupportedLabels() } returns labels

        assertEquals(objectDetectionService.getSupportedLabels(), labels)

        verify { objectDetector.getSupportedLabels() }
    }

    @Test
    fun `detectObjects cannot get file`() {
        every { objectService.getById(fileId) } returns null

        assertThrows(IllegalArgumentException::class.java) {
            objectDetectionService.detectObjects(fileId, jpg, user, emptyList())
        }

        verify { objectService.getById(fileId) }
    }

    @Test
    fun `detectObjects invalid image`() {
        every { objectService.getById(fileId) } returns ByteArrayInputStream("invalid_image".toByteArray())
        every { objectService.getInfoById(fileId) } returns objectStorageInfo

        assertThrows(IllegalStateException::class.java) {
            objectDetectionService.detectObjects(fileId, jpg, user, emptyList())
        }

        verify { objectService.getById(fileId) }
        verify { objectService.getInfoById(fileId) }
    }

    @Test
    fun `detectObjects without filter`() {
        val id1 = "id1"
        val id2 = "id2"
        val capturedOriginalNames = mutableListOf<String>()

        every { objectService.getById(fileId) } returns
                Thread.currentThread().contextClassLoader.getResourceAsStream(TestConfiguration.TEST_IMAGE_1)
        every { objectService.getInfoById(fileId) } returns objectStorageInfo
        every { objectDetector.detect(any()) } returns detectedObjects
        every { objectService.saveImageFile(any(), jpg, capture(capturedOriginalNames), user) } returns id1 andThen id2

        val expected = listOf(
            DetectedImage(id1, mapOf(person to probability1)),
            DetectedImage(id2, mapOf(car to probability2))
        )

        assertEquals(expected, objectDetectionService.detectObjects(fileId, jpg, user, listOf(person, car)))
        assertEquals(2, capturedOriginalNames.size)
        assertEquals("$originalName-detected0.$jpg", capturedOriginalNames[0])
        assertEquals("$originalName-detected1.$jpg", capturedOriginalNames[1])

        verify { objectService.getById(fileId) }
        verify { objectService.getInfoById(fileId) }
        verify { objectDetector.detect(any()) }
        verify(exactly = 2) { objectService.saveImageFile(any(), jpg, any(), user) }
    }

    @Test
    fun `detectObjects with filter`() {
        val id = "id"

        every { objectService.getById(fileId) } returns
                Thread.currentThread().contextClassLoader.getResourceAsStream(TestConfiguration.TEST_IMAGE_1)
        every { objectService.getInfoById(fileId) } returns objectStorageInfo
        every { objectDetector.detect(any()) } returns detectedObjects
        every { objectService.saveImageFile(any(), jpg, "$originalName-detected0.$jpg", user) } returns id

        val expected = listOf(
            DetectedImage(id, mapOf(car to probability2))
        )

        assertEquals(expected, objectDetectionService.detectObjects(fileId, jpg, user, listOf(car)))

        verify { objectService.getById(fileId) }
        verify { objectService.getInfoById(fileId) }
        verify { objectDetector.detect(any()) }
        verify(exactly = 1) { objectService.saveImageFile(any(), jpg, "$originalName-detected0.$jpg", user) }
    }
}