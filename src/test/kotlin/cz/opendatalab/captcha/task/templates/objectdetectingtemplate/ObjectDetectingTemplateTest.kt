package cz.opendatalab.captcha.task.templates.objectdetectingtemplate

import cz.opendatalab.captcha.TestConfiguration
import cz.opendatalab.captcha.datamanagement.objectmetadata.*
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.objectdetection.AbsoluteBoundingBox
import cz.opendatalab.captcha.objectdetection.RelativeBoundingBox
import cz.opendatalab.captcha.task.templates.ObjectDetectingGenerationConfig
import cz.opendatalab.captcha.task.templates.TemplateUtils
import cz.opendatalab.captcha.verification.entities.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream

internal class ObjectDetectingTemplateTest {
    private val objectService: ObjectService = mockk()
    private val objectMetadataService: ObjectMetadataService = mockk()

    private val objectDetectingTemplate = ObjectDetectingTemplate(objectMetadataService, objectService)

    private val user = "user"
    private val labelgroup = "labelgroup"
    private val label = "label"
    private val id1 = "1"
    private val id2 = "2"
    private val id3 = "3"
    private val jpg = "jpg"
    private val testImage1Size = ImageSize(682, 1023)
    private val testImage2Size = ImageSize(612, 408)
    private val expectedResult = mutableListOf(
        // ground truth boxes
        AbsoluteBoundingBox(213, 898, 158, 83).toRelativeBoundingBox(testImage1Size),
        AbsoluteBoundingBox(494, 921, 151, 60).toRelativeBoundingBox(testImage1Size)
    )
    private val objectsDetectingDataFinished =
        ObjectsDetectingData(mutableMapOf(labelgroup to mutableMapOf(label to ObjectDetectingData(expectedResult))))
    private val objectsDetectingDataNotFinished = ObjectsDetectingData(
        mutableMapOf(
            labelgroup to mutableMapOf(
                label to ObjectDetectingData(
                    false, mutableListOf(), mutableListOf(
                        // answer 1
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                            AbsoluteBoundingBox(222, 170, 147, 112).toRelativeBoundingBox(testImage2Size), // object 2
                            AbsoluteBoundingBox(55, 105, 116, 122).toRelativeBoundingBox(testImage2Size), // object 3
                        ),
                        // answer 2
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                            AbsoluteBoundingBox(218, 172, 148, 112).toRelativeBoundingBox(testImage2Size), // object 2
                            AbsoluteBoundingBox(50, 100, 120, 125).toRelativeBoundingBox(testImage2Size), // object 3

                        ),
                        // answer 3
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                            AbsoluteBoundingBox(228, 170, 150, 120).toRelativeBoundingBox(testImage2Size), // object 2
                            AbsoluteBoundingBox(60, 102, 110, 115).toRelativeBoundingBox(testImage2Size), // object 3

                        ),
                        // answer 4
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                            AbsoluteBoundingBox(210, 160, 140, 108).toRelativeBoundingBox(testImage2Size), // object 2
                            AbsoluteBoundingBox(51, 108, 122, 122).toRelativeBoundingBox(testImage2Size), // object 3

                        ),
                        // answer 5
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                            AbsoluteBoundingBox(221, 168, 150, 120).toRelativeBoundingBox(testImage2Size), // object 2
                            AbsoluteBoundingBox(55, 100, 120, 130).toRelativeBoundingBox(testImage2Size), // object 3

                        ),
                        // answer 6
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                            AbsoluteBoundingBox(215, 165, 160, 120).toRelativeBoundingBox(testImage2Size), // object 2
                            AbsoluteBoundingBox(52, 204, 118, 90).toRelativeBoundingBox(testImage2Size), // no object
                        ),
                        // answer 7
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                        ),
                        // answer 8
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                        ),
                        // answer 9
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                        ),
                        // answer 10
                        listOf(
                            AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
                        ),
                    )
                )
            )
        )
    )
    private val config = ObjectDetectingGenerationConfig(emptyList(), emptyList())
    private val metadata = listOf(
        ObjectMetadata(id1, user, ImageObjectType(jpg)),
        ObjectMetadata(
            id2,
            user,
            ImageObjectType(jpg),
            mutableMapOf(),
            mutableMapOf(ObjectDetectingConstants.TEMPLATE_DATA_NAME to objectsDetectingDataFinished),
            emptyList()
        ),
        ObjectMetadata(
            id3,
            user,
            ImageObjectType(jpg),
            mutableMapOf(),
            mutableMapOf(ObjectDetectingConstants.TEMPLATE_DATA_NAME to objectsDetectingDataNotFinished),
            emptyList()
        )
    )

    @BeforeEach
    fun initTaskGeneration() {
        every {
            objectMetadataService.getFiltered(
                user,
                config.tags,
                config.owners,
                ObjectTypeEnum.IMAGE
            )
        } returns metadata
        every { objectService.getById(id2) } returns loadImage(TestConfiguration.TEST_IMAGE_1)
        every { objectService.getById(id3) } answers { loadImage(TestConfiguration.TEST_IMAGE_2) }
    }

    @Test
    fun generateTaskTestSuccessful() {
        val (description, taskData, answerSheet) = objectDetectingTemplate.generateTask(config, user)

        // assert description
        assertEquals(description.text, "Mark all instances of $label with a rectangle.")

        // assert taskData
        assertTrue(taskData is ImagesWithBoundingBoxes)
        taskData as ImagesWithBoundingBoxes
        assertEquals(labelgroup, taskData.labelGroup)
        assertEquals(label, taskData.label)
        assertEquals(id3, taskData.unknownImageId)
        assertEquals(testImage1Size, taskData.knownImageSize)
        assertEquals(expectedResult, taskData.expectedResult)

        // assert answerSheet
        assertEquals(AnswerType.MultipleBoundingBox, answerSheet.answerType)
        assertTrue(answerSheet.displayData is ListDisplayData)
        val displayData = answerSheet.displayData as ListDisplayData
        assertEquals(2, displayData.listData.size)
        assertTrue(displayData.listData[0] is ImageDisplayData)
        assertTrue(displayData.listData[1] is ImageDisplayData)
        val testImage1 = loadImage(TestConfiguration.TEST_IMAGE_1).use {
            TemplateUtils.toBase64Image(it.readAllBytes(), jpg)
        }
        val testImage2 = loadImage(TestConfiguration.TEST_IMAGE_2).use {
            TemplateUtils.toBase64Image(it.readAllBytes(), jpg)
        }
        assertEquals(testImage1, (displayData.listData[0] as ImageDisplayData).base64ImageString)
        assertEquals(testImage2, (displayData.listData[1] as ImageDisplayData).base64ImageString)

        verify { objectMetadataService.getFiltered(user, config.tags, config.owners, ObjectTypeEnum.IMAGE) }
        verify { objectService.getById(id2) }
        verify { objectService.getById(id3) }
    }

    @Test
    fun generateTaskNotEnoughImagesWithObjectDetectingData() {
        every {
            objectMetadataService.getFiltered(
                user,
                config.tags,
                config.owners,
                ObjectTypeEnum.IMAGE
            )
        } returns listOf(
            ObjectMetadata(id1, user, ImageObjectType(jpg)),
            ObjectMetadata(id2, user, ImageObjectType(jpg)),
            ObjectMetadata(id3, user, ImageObjectType(jpg))
        )

        assertThrows(IllegalArgumentException::class.java) {
            objectDetectingTemplate.generateTask(config, user)
        }
    }

    @Test
    fun evaluateTaskWithoutSufficientScoreToRecordAnswer() {
        val (_, taskData, _) = objectDetectingTemplate.generateTask(config, user)

        val answer = BoundingBoxesAnswer(listOf(), listOf())

        val result = objectDetectingTemplate.evaluateTask(taskData, answer)

        assertEquals(0.0, result.evaluation)

        verify(exactly = 0) { objectMetadataService.getById(any()) }
        verify(exactly = 0) { objectMetadataService.updateMetadata(any()) }
    }

    @Test
    fun evaluateTaskSuccessfulWithoutDetectingEvaluation() {
        // remove answers from metadata
        val metadataCopy = metadata.toMutableList()
        val objectsDetectingData =
            (metadataCopy[2].templateData[ObjectDetectingConstants.TEMPLATE_DATA_NAME] as ObjectsDetectingData).objects[labelgroup]?.get(
                label
            )!!
        objectsDetectingData.answers.clear()
        every {
            objectMetadataService.getFiltered(
                user,
                config.tags,
                config.owners,
                ObjectTypeEnum.IMAGE
            )
        } returns metadataCopy
        every { objectMetadataService.getById(id3) } returns metadataCopy[2]
        every { objectMetadataService.updateMetadata(any()) } returns metadataCopy[2]

        val (_, taskData, _) = objectDetectingTemplate.generateTask(config, user)

        val answer = BoundingBoxesAnswer(
            listOf(
                AbsoluteBoundingBox(213, 898, 158, 83).toRelativeBoundingBox(testImage1Size),
                AbsoluteBoundingBox(494, 921, 151, 60).toRelativeBoundingBox(testImage1Size)
            ),
            listOf(
                RelativeBoundingBox(0.5, 0.5, 0.5, 0.5)
            )
        )

        val result = objectDetectingTemplate.evaluateTask(taskData, answer)

        assertEquals(1.0, result.evaluation)
        assertFalse(objectsDetectingData.isLocalized)
        assertTrue(objectsDetectingData.result.isEmpty())
        assertEquals(1, objectsDetectingData.answers.size)

        verify(exactly = 1) { objectMetadataService.getById(id3) }
        verify(exactly = 1) { objectMetadataService.updateMetadata(any()) }
    }

    @Test
    fun evaluateTaskSuccessfulWithDetectingEvaluation() {
        every { objectMetadataService.getById(id3) } returns metadata[2]
        every { objectMetadataService.updateMetadata(any()) } returns metadata[2]
        val objectsDetectingData =
            (metadata[2].templateData[ObjectDetectingConstants.TEMPLATE_DATA_NAME] as ObjectsDetectingData).objects[labelgroup]?.get(
                label
            )!!

        val (_, taskData, _) = objectDetectingTemplate.generateTask(config, user)

        val answer = BoundingBoxesAnswer(
            listOf(
                AbsoluteBoundingBox(213, 898, 158, 83).toRelativeBoundingBox(testImage1Size),
                AbsoluteBoundingBox(494, 921, 151, 60).toRelativeBoundingBox(testImage1Size),
                AbsoluteBoundingBox(50, 50, 50, 50).toRelativeBoundingBox(testImage1Size)
            ),
            listOf(
                AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(testImage2Size), // object 1
            )
        )

        val result = objectDetectingTemplate.evaluateTask(taskData, answer)

        assertEquals(0.8986787711761368, result.evaluation)
        assertTrue(objectsDetectingData.isLocalized)
        assertEquals(2, objectsDetectingData.result.size)
        assertTrue(
            objectsDetectingData.result.contains(
                AbsoluteBoundingBox(408, 240, 101, 139).toRelativeBoundingBox(
                    testImage2Size
                )
            )
        )
        assertEquals(11, objectsDetectingData.answers.size)

        verify(exactly = 1) { objectMetadataService.getById(id3) }
        verify(exactly = 1) { objectMetadataService.updateMetadata(any()) }
    }

    @Test
    fun evaluateTaskBoundingBoxOutOfImage() {
        val (_, taskData, _) = objectDetectingTemplate.generateTask(config, user)

        val answer = BoundingBoxesAnswer(
            listOf(
                AbsoluteBoundingBox(213, 897, 500, 600).toRelativeBoundingBox(testImage1Size),
            ),
            listOf()
        )

        assertThrows(IllegalArgumentException::class.java) {
            objectDetectingTemplate.evaluateTask(taskData, answer)
        }
    }

    private fun loadImage(name: String): InputStream {
        return Thread.currentThread().contextClassLoader.getResourceAsStream(name)!!
    }
}