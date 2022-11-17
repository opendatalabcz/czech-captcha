package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.datamanagement.dto.*
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.datamanagement.objectdetection.DetectedImage
import cz.opendatalab.captcha.datamanagement.objectdetection.ObjectDetectionConstants
import cz.opendatalab.captcha.datamanagement.objectdetection.ObjectDetectionService
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectDetectingConstants
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectDetectingData
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectsDetectingData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.server.ResponseStatusException


internal class ObjectMetadataServiceTest {

    private val objectMetadataRepo: ObjectMetadataRepository = mockk()
    private val labelGroupRepository: LabelGroupRepository = mockk()
    private val objectService: ObjectService = mockk()
    private val objectDetectionService: ObjectDetectionService = mockk()
    val objectMetadataService = ObjectMetadataService(objectMetadataRepo, objectService, labelGroupRepository, objectDetectionService)

    private val user = "user"
    private val url = "url"
    private val jpg = "jpg"
    private val labelGroup = "labelGroup"
    private val objectMetadataCreateDTO = ObjectMetadataCreateDTO(emptyMap(), emptyList())
    private val uuid = "123e4567-e89b-12d3-a456-426614174000"

    @Test
    fun `getLimitedLabelGroup non empty`() {
        every { labelGroupRepository.findByName(labelGroup) } returns LabelGroupLimited(labelGroup, emptyList(), 2)

        assertNotNull(objectMetadataService.getLimitedLabelGroup(labelGroup))

        verify { labelGroupRepository.findByName(labelGroup) }
    }

    @Test
    fun `getLimitedLabelGroup empty`() {
        every { labelGroupRepository.findByName(labelGroup) } returns null

        assertNull(objectMetadataService.getLimitedLabelGroup(labelGroup))

        verify { labelGroupRepository.findByName(labelGroup) }
    }

    @Test
    fun `getLimitedLabelGroup not limited`() {
        every { labelGroupRepository.findByName(labelGroup) } returns LabelGroup(labelGroup, 2)

        assertNull(objectMetadataService.getLimitedLabelGroup(labelGroup))

        verify { labelGroupRepository.findByName(labelGroup) }
    }

    @Test
    fun `addUrlImage threshold1 out of range`() {
        assertThrows(ResponseStatusException::class.java) {
            objectMetadataService.addUrlImage(UrlImageCreateDTO(url, ImageFileTypeDTO(jpg),
                objectMetadataCreateDTO,
                ObjectDetectionParametersDTO(emptyMap(), 1.2, 0.7)
            ), user)
        }
    }

    @Test
    fun `addUrlImage threshold2 out of range`() {
        assertThrows(ResponseStatusException::class.java) {
            objectMetadataService.addUrlImage(UrlImageCreateDTO(url, ImageFileTypeDTO(jpg),
                objectMetadataCreateDTO,
                ObjectDetectionParametersDTO(emptyMap(), 0.5, -0.7)
            ), user)
        }
    }

    @Test
    fun `addUrlImage thresholds not increasing`() {
        assertThrows(ResponseStatusException::class.java) {
            objectMetadataService.addUrlImage(UrlImageCreateDTO(url, ImageFileTypeDTO(jpg),
                objectMetadataCreateDTO,
                ObjectDetectionParametersDTO(emptyMap(), 0.8, 0.7)
            ), user)
        }
    }

    @Test
    fun `addUrlImage wrong label group`() {
        every { labelGroupRepository.findByName(labelGroup) } returns null

        assertThrows(ResponseStatusException::class.java) {
            objectMetadataService.addUrlImage(UrlImageCreateDTO(url, ImageFileTypeDTO(jpg),
                objectMetadataCreateDTO,
                ObjectDetectionParametersDTO(mapOf(labelGroup to listOf("label")), 0.8, 0.9)
            ), user)
        }

        verify { labelGroupRepository.findByName(labelGroup) }
    }

    @Test
    fun `addUrlImage wrong label`() {
        every { labelGroupRepository.findByName(labelGroup) } returns
                LabelGroupLimited("name", listOf("one", "two"), 1)

        assertThrows(ResponseStatusException::class.java) {
            objectMetadataService.addUrlImage(UrlImageCreateDTO(url, ImageFileTypeDTO(jpg),
                objectMetadataCreateDTO,
                ObjectDetectionParametersDTO(mapOf(labelGroup to listOf("label")), 0.8, 0.9)
            ), user)
        }

        verify { labelGroupRepository.findByName(labelGroup) }
    }

    @Test
    fun addUrlImage() {
        addImage(true)
    }

    @Test
    fun addFileImage() {
        addImage(false)
    }

    private fun addImage(urlImage: Boolean) {
        val filename = "file.jpg"
        val file = MockMultipartFile(filename, filename, MediaType.IMAGE_JPEG_VALUE, "image".toByteArray())
        val knownLabelGroup = "knownLabelGroup"
        val knownLabel1 = "knownLabel1"
        val knownLabel2 = "knownLabel2"
        val allKnownLabels = listOf(knownLabel1, knownLabel2)
        val knownLabels = listOf(knownLabel1)
        val tags = listOf("tag1")
        val childId1 = "childId1"
        val odLabel1 = "odLabel1"
        val odLabel2 = "odLabel2"
        val odLabels = listOf(odLabel1, odLabel2)
        val nonOdLabelGroup = "nonOdLabelGroup"
        val nonOdLabel = "nonOdLabel"
        val odWantedLabels = listOf(odLabel1)
        val nonOdWantedLabels = listOf(nonOdLabel)
        val parentMetadata = ObjectMetadata(uuid, user, ImageObjectType(jpg),
            mutableMapOf(knownLabelGroup to Labeling(knownLabels)), tags)
        val parentMetadataWithChild = ObjectMetadata(uuid, user, ImageObjectType(jpg),
            mutableMapOf(knownLabelGroup to Labeling(knownLabels)),
            mutableMapOf(
                ObjectMetadataService.CHILDREN_FILES_TEMPLATE_NAME to ChildrenFiles(mutableListOf(childId1)),
                ObjectDetectingConstants.TEMPLATE_DATA_NAME to ObjectsDetectingData(
                    mutableMapOf(nonOdLabelGroup to mutableMapOf(nonOdLabel to ObjectDetectingData(false, mutableListOf(), mutableListOf())))
                )), tags)
        val childLabeling = Labeling(false, emptyList(), emptyList(), LabelStatistics(
            mutableMapOf(odLabel1 to LabelStatistic(1, 1), odLabel2 to LabelStatistic(-1, 1))
        ))
        val childMetadata = ObjectMetadata(childId1, user, ImageObjectType(jpg),
            mutableMapOf(ObjectDetectionConstants.LABEL_GROUP to childLabeling),
            mutableMapOf(ObjectMetadataService.PARENT_FILE_TEMPLATE_NAME to ParentFile(uuid)), tags
        )

        every { labelGroupRepository.findByName(ObjectDetectionConstants.LABEL_GROUP) } returns
                LabelGroupLimited(ObjectDetectionConstants.LABEL_GROUP, odLabels, 1)
        every { labelGroupRepository.findByName(nonOdLabelGroup) } returns
                LabelGroupLimited(nonOdLabelGroup, nonOdWantedLabels, 1)
        every { objectService.saveFile(user, file, ImageObjectType(jpg)) } returns
                uuid
        every { objectService.saveURLFile(user, url) } returns
                uuid
        every { labelGroupRepository.findByName(knownLabelGroup) } returns
                LabelGroupLimited(knownLabelGroup, allKnownLabels, 1)
        every { objectMetadataRepo.insert(parentMetadata) } returns
                parentMetadata
        every { objectDetectionService.detectObjects(uuid, jpg, user, odWantedLabels) } returns
                listOf(DetectedImage(childId1, mapOf(odLabel1 to 0.81)))
        every { objectDetectionService.getSupportedLabels() } returns
                odLabels.toSet()
        every { objectMetadataRepo.insert(childMetadata) } returns
                childMetadata
        every { objectMetadataRepo.save(parentMetadataWithChild) } returns
                parentMetadataWithChild

        val result = if (urlImage) {
            objectMetadataService.addUrlImage(UrlImageCreateDTO(url, ImageFileTypeDTO(jpg),
                ObjectMetadataCreateDTO(mapOf(knownLabelGroup to knownLabels), tags),
                ObjectDetectionParametersDTO(mapOf(
                    ObjectDetectionConstants.LABEL_GROUP to odWantedLabels,
                    nonOdLabelGroup to nonOdWantedLabels), 0.8, 0.9)
            ), user)
        } else {
            objectMetadataService.addFileImage(
                file, FileImageCreateDTO(
                    ImageFileTypeDTO(jpg),
                    ObjectMetadataCreateDTO(mapOf(knownLabelGroup to knownLabels), tags),
                    ObjectDetectionParametersDTO(mapOf(
                        ObjectDetectionConstants.LABEL_GROUP to odWantedLabels,
                        nonOdLabelGroup to nonOdWantedLabels), 0.8, 0.9)
                ), user)
        }
        assertEquals(listOf(childId1, uuid), result)

        if (urlImage) {
            verify { objectService.saveURLFile(user, url) }
        } else {
            verify { objectService.saveFile(user, file, ImageObjectType(jpg)) }
        }
        verify { labelGroupRepository.findByName(ObjectDetectionConstants.LABEL_GROUP) }
        verify { labelGroupRepository.findByName(nonOdLabelGroup) }
        verify { objectMetadataRepo.insert(parentMetadataWithChild) } // metadata is changed after insertion
        verify { objectDetectionService.detectObjects(uuid, jpg, user, odWantedLabels) }
        verify { objectDetectionService.getSupportedLabels() }
        verify { objectMetadataRepo.insert(childMetadata) }
        verify { objectMetadataRepo.save(parentMetadataWithChild) }
    }
}
