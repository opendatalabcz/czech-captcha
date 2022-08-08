package cz.opendatalab.captcha.objectdetection

import cz.opendatalab.captcha.TestConfiguration
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
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
        DetectedObject(person, probability1, BoundingBox(10, 10, 10, 10)),
        DetectedObject(car, probability2, BoundingBox(20, 20, 20, 20))
    )

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

        assertThrows(IllegalStateException::class.java) {
            objectDetectionService.detectObjects(fileId, jpg, user, emptyList())
        }

        verify { objectService.getById(fileId) }
    }

    @Test
    fun `detectObjects without filter`() {
        val id1 = "id1"
        val id2 = "id2"

        every { objectService.getById(fileId) } returns
                Thread.currentThread().contextClassLoader.getResourceAsStream(TestConfiguration.TEST_IMAGE)
        every { objectDetector.detect(any()) } returns detectedObjects
        every { objectService.saveImageFile(any(), jpg, user) } returns id1 andThen id2

        val expected = listOf(
            DetectedImage(id1, mapOf(person to probability1)),
            DetectedImage(id2, mapOf(car to probability2))
        )

        assertEquals(expected, objectDetectionService.detectObjects(fileId, jpg, user, listOf(person, car)))

        verify { objectService.getById(fileId) }
        verify { objectDetector.detect(any()) }
        verify(exactly = 2) { objectService.saveImageFile(any(), jpg, user) }
    }

    @Test
    fun `detectObjects with filter`() {
        val id = "id"

        every { objectService.getById(fileId) } returns
                Thread.currentThread().contextClassLoader.getResourceAsStream(TestConfiguration.TEST_IMAGE)
        every { objectDetector.detect(any()) } returns detectedObjects
        every { objectService.saveImageFile(any(), jpg, user) } returns id

        val expected = listOf(
            DetectedImage(id, mapOf(car to probability2))
        )

        assertEquals(expected, objectDetectionService.detectObjects(fileId, jpg, user, listOf(car)))

        verify { objectService.getById(fileId) }
        verify { objectDetector.detect(any()) }
        verify(exactly = 1) { objectService.saveImageFile(any(), jpg, user) }
    }
}