package cz.opendatalab.captcha.datamanagement.objectdetection

import cz.opendatalab.captcha.TestImages
import cz.opendatalab.captcha.datamanagement.ImageUtils
import io.mockk.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

internal class ObjectDetectionServiceTest {

    private val objectDetector: ObjectDetector = mockk()

    private val objectDetectionService = ObjectDetectionService(objectDetector)

    private val image = TestImages.IMAGE_1
    private val person = "person"
    private val car = "car"
    private val labels = setOf(person, car)
    private val probability1 = 0.9
    private val probability2 = 0.1
    private val box1 = RelativeBoundingBox(0.1, 0.1, 0.1, 0.1)
    private val box2 = RelativeBoundingBox(0.2, 0.2, 0.2, 0.2)
    private val box3 = RelativeBoundingBox(0.2, 0.2, 0.4, 0.4)
    private val detectedObjectsWithNoOverlaps = listOf(
        DetectedObject(person, probability1, box1),
        DetectedObject(car, probability2, box2)
    )
    private val detectedObjectsWithOverlapDifferentLabel = listOf(
        DetectedObject(person, probability1, box2),
        DetectedObject(car, probability2, box3)
    )
    private val detectedObjectsWithOverlapSameLabel = listOf(
        DetectedObject(car, probability1, box2),
        DetectedObject(car, probability2, box3)
    )

    companion object {
        @BeforeAll
        @JvmStatic
        fun mockImageUtils() {
            mockkObject(ImageUtils)
        }

        @AfterAll
        @JvmStatic
        fun unmockImageUtils() {
            unmockkObject(ImageUtils)
        }
    }

    @Test
    fun getSupportedLabels() {
        every { objectDetector.getSupportedLabels() } returns labels
        assertEquals(labels, objectDetectionService.getSupportedLabels())
        verify { objectDetector.getSupportedLabels() }
    }

    @Test
    fun detectObjects_withImage_successful() {
        every { objectDetector.detect(image) } returns detectedObjectsWithNoOverlaps

        val result = objectDetectionService.detectObjects(image, setOf(car))
        assertEquals(1, result.size)
        assertEquals(detectedObjectsWithNoOverlaps[1], result[0])

        verify { objectDetector.detect(image) }
    }

    @Test
    fun detectObjects_withInputStream_successful() {
        every { objectDetector.detect(image) } returns detectedObjectsWithNoOverlaps
        every { ImageUtils.getImageFromInputStream(any()) } returns image

        val result = objectDetectionService.detectObjects(ByteArrayInputStream(ByteArray(1)), setOf(car))
        assertEquals(1, result.size)
        assertEquals(detectedObjectsWithNoOverlaps[1], result[0])

        verify { ImageUtils.getImageFromInputStream(any()) }
        verify { objectDetector.detect(image) }
    }

    @Test
    fun detectObjectsWithOverlaps_noOverlaps_shouldHaveOnlyOriginalLabel() {
        every { objectDetector.detect(image) } returns detectedObjectsWithNoOverlaps

        val result = objectDetectionService.detectObjectsWithOverlaps(image, labels)
        assertEquals(2, result.size)
        assertEquals(DetectedObjectWithOverlappingLabels(mapOf(person to probability1), box1), result[0])
        assertEquals(DetectedObjectWithOverlappingLabels(mapOf(car to probability2), box2), result[1])

        verify { objectDetector.detect(image) }
    }

    @Test
    fun detectObjectsWithOverlaps_withOverlapsDifferentLabel_shouldHaveBothLabels() {
        every { objectDetector.detect(image) } returns detectedObjectsWithOverlapDifferentLabel

        val result = objectDetectionService.detectObjectsWithOverlaps(image, labels)
        assertEquals(2, result.size)
        assertEquals(box2, result[0].relativeBoundingBox)
        assertEquals(box3, result[1].relativeBoundingBox)
        assertEquals(2, result[0].labelsWithProbability.size)
        assertEquals(2, result[1].labelsWithProbability.size)
        assertEquals(probability1, result[0].labelsWithProbability[person])
        assertEquals(probability2, result[0].labelsWithProbability[car])
        assertEquals(probability2, result[1].labelsWithProbability[car])
        assertEquals(probability1 * 0.25, result[1].labelsWithProbability[person])

        verify { objectDetector.detect(image) }
    }

    @Test
    fun detectObjectsWithOverlaps_withOverlapsSameLabel_shouldHaveOneLabel() {
        every { objectDetector.detect(image) } returns detectedObjectsWithOverlapSameLabel

        val result = objectDetectionService.detectObjectsWithOverlaps(image, labels)
        assertEquals(2, result.size)
        assertEquals(box2, result[0].relativeBoundingBox)
        assertEquals(box3, result[1].relativeBoundingBox)
        assertEquals(1, result[0].labelsWithProbability.size)
        assertEquals(1, result[1].labelsWithProbability.size)
        assertEquals(probability1, result[0].labelsWithProbability[car])
        assertEquals(probability1 * 0.25, result[1].labelsWithProbability[car])

        verify { objectDetector.detect(image) }
    }

    @Test
    fun detectObjectsWithOverlaps_withInputStreamAndNoOverlaps_shouldHaveOnlyOriginalLabel() {
        every { objectDetector.detect(image) } returns detectedObjectsWithNoOverlaps
        every { ImageUtils.getImageFromInputStream(any()) } returns image

        val result = objectDetectionService.detectObjectsWithOverlaps(ByteArrayInputStream(ByteArray(1)), labels)
        assertEquals(2, result.size)
        assertEquals(DetectedObjectWithOverlappingLabels(mapOf(person to probability1), box1), result[0])
        assertEquals(DetectedObjectWithOverlappingLabels(mapOf(car to probability2), box2), result[1])

        verify { ImageUtils.getImageFromInputStream(any()) }
        verify { objectDetector.detect(image) }
    }
}