package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.TestImages
import cz.opendatalab.captcha.datamanagement.dto.*
import cz.opendatalab.captcha.datamanagement.objectdetection.*
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectRepositoryType
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectDetectingConstants
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectDetectingData
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectsDetectingData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream


internal class ObjectMetadataServiceTest {

    private val objectMetadataRepo: ObjectMetadataRepository = mockk()
    private val labelGroupRepository: LabelGroupRepository = mockk()
    private val objectService: ObjectService = mockk()
    private val objectDetectionService: ObjectDetectionService = mockk()

    private val objectMetadataService =
        ObjectMetadataService(objectMetadataRepo, objectService, labelGroupRepository, objectDetectionService)

    private val user = "user"
    private val originalFilenameTxt = "originalFilename.txt"
    private val url = "http://www.some-url.com/$originalFilenameTxt"
    private val jpg = "jpg"
    private val label = "label"
    private val labelGroupName = "labelGroup"
    private val labelGroup = LabelGroupLimited(labelGroupName, setOf(label), 1)
    private val tag = "tag"
    private val objectMetadataCreateDTO = ObjectMetadataCreateDTO(emptyMap(), emptySet())
    private val uuid = "123e4567-e89b-12d3-a456-426614174000"
    private val urlStorageInfo = ObjectStorageInfo(uuid, originalFilenameTxt, url, ObjectRepositoryType.URL)
    private val fileStorageInfo = ObjectStorageInfo(uuid, originalFilenameTxt, "$uuid.txt", ObjectRepositoryType.FILESYSTEM)
    private val metadataTxt = ObjectMetadata(
        uuid, user, "txt", setOf(tag), mapOf(labelGroupName to Labeling(setOf(label)))
    )

    @Test
    fun `getLimitedLabelGroup non empty`() {
        every { labelGroupRepository.findByName(labelGroupName) } returns LabelGroupLimited(labelGroupName, emptySet(), 2)

        assertNotNull(objectMetadataService.getLimitedLabelGroup(labelGroupName))

        verify { labelGroupRepository.findByName(labelGroupName) }
    }

    @Test
    fun `getLimitedLabelGroup empty`() {
        every { labelGroupRepository.findByName(labelGroupName) } returns null

        assertNull(objectMetadataService.getLimitedLabelGroup(labelGroupName))

        verify { labelGroupRepository.findByName(labelGroupName) }
    }

    @Test
    fun `getLimitedLabelGroup not limited`() {
        every { labelGroupRepository.findByName(labelGroupName) } returns LabelGroup(labelGroupName, 2)

        assertNull(objectMetadataService.getLimitedLabelGroup(labelGroupName))

        verify { labelGroupRepository.findByName(labelGroupName) }
    }

    @Test
    fun addUrlObject_invalidLabelGroup_shouldThrow() {
        every { labelGroupRepository.findByName(labelGroupName) } returns null

        assertThrows(IllegalArgumentException::class.java) {
            objectMetadataService.addUrlObject(
                UrlObjectCreateDTO(
                    url,
                    ObjectMetadataCreateDTO(mapOf(labelGroupName to setOf(label)), setOf(tag))
                ), user
            )
        }

        verify { labelGroupRepository.findByName(labelGroupName) }
    }

    @Test
    fun addUrlObject_invalidLabel_shouldThrow() {
        every { labelGroupRepository.findByName(labelGroupName) } returns
                LabelGroupLimited(labelGroupName, setOf("one", "two"), 1)

        assertThrows(IllegalArgumentException::class.java) {
            objectMetadataService.addUrlObject(
                UrlObjectCreateDTO(
                    url,
                    ObjectMetadataCreateDTO(mapOf(labelGroupName to setOf(label)), setOf(tag))
                ), user
            )
        }

        verify { labelGroupRepository.findByName(labelGroupName) }
    }

    @Test
    fun addUrlObject_successful() {
        every { labelGroupRepository.findByName(labelGroupName) } returns labelGroup
        every { objectService.saveUrlObject(url) } returns urlStorageInfo
        every { objectMetadataRepo.insert(metadataTxt) } returns metadataTxt

        assertEquals(metadataTxt, objectMetadataService.addUrlObject(
                UrlObjectCreateDTO(
                    url,
                    ObjectMetadataCreateDTO(mapOf(labelGroupName to setOf(label)), setOf(tag))
                ), user
            ))

        verify { labelGroupRepository.findByName(labelGroupName) }
        verify { objectService.saveUrlObject(url) }
        verify { objectMetadataRepo.insert(metadataTxt) }
    }

    @Test
    fun addFileObject_successful() {
        val content = ByteArrayInputStream("test-content".toByteArray())
        every { labelGroupRepository.findByName(labelGroupName) } returns labelGroup
        every { objectService.saveFileObject(content, originalFilenameTxt) } returns fileStorageInfo
        every { objectMetadataRepo.insert(metadataTxt) } returns metadataTxt

        assertEquals(metadataTxt, objectMetadataService.addFileObject(
            content,
            originalFilenameTxt,
            FileObjectCreateDTO(
                ObjectMetadataCreateDTO(mapOf(labelGroupName to setOf(label)), setOf(tag))
            ), user
        ))

        verify { labelGroupRepository.findByName(labelGroupName) }
        verify { objectService.saveFileObject(content, originalFilenameTxt) }
        verify { objectMetadataRepo.insert(metadataTxt) }
    }

    @Test
    fun addUrlImageWithOD_doODThreshold1OutOfRange_shouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            objectMetadataService.addUrlImageWithOD(
                UrlImageCreateDTO(
                    url,
                    objectMetadataCreateDTO,
                    ObjectDetectionDTO(
                        ObjectDetectionParametersDTO(emptyMap(), 1.2, 0.7),
                        null
                    )
                ), user
            )
        }
    }

    @Test
    fun addUrlImageWithOD_doODThreshold2OutOfRange_shouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            objectMetadataService.addUrlImageWithOD(
                UrlImageCreateDTO(
                    url,
                    objectMetadataCreateDTO,
                    ObjectDetectionDTO(
                        ObjectDetectionParametersDTO(emptyMap(), 1.2, -0.7),
                        null
                    )
                ), user
            )
        }
    }

    @Test
    fun addUrlImageWithOD_doODThresholdsNotIncreasing_shouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            objectMetadataService.addUrlImageWithOD(
                UrlImageCreateDTO(
                    url,
                    objectMetadataCreateDTO,
                    ObjectDetectionDTO(
                        ObjectDetectionParametersDTO(emptyMap(), 0.8, 0.7),
                        null
                    )
                ), user
            )
        }
    }

    @Test
    fun addUrlImageWithOD_doODInvalidLabelGroup_shouldThrow() {
        every { labelGroupRepository.findByName(labelGroupName) } returns null

        assertThrows(IllegalArgumentException::class.java) {
            objectMetadataService.addUrlImageWithOD(
                UrlImageCreateDTO(
                    url,
                    objectMetadataCreateDTO,
                    ObjectDetectionDTO(
                        ObjectDetectionParametersDTO(mapOf(labelGroupName to setOf("label")), 0.5, 0.7),
                        null
                    )
                ), user
            )
        }

        verify { labelGroupRepository.findByName(labelGroupName) }
    }

    @Test
    fun addUrlImageWithOD_doODInvalidLabel_shouldThrow() {
        every { labelGroupRepository.findByName(labelGroupName) } returns
                LabelGroupLimited(labelGroupName, setOf("one", "two"), 1)

        assertThrows(IllegalArgumentException::class.java) {
            objectMetadataService.addUrlImageWithOD(
                UrlImageCreateDTO(
                    url,
                    objectMetadataCreateDTO,
                    ObjectDetectionDTO(
                        ObjectDetectionParametersDTO(mapOf(labelGroupName to setOf("label")), 0.5, 0.7),
                        null
                    )
                ), user
            )
        }

        verify { labelGroupRepository.findByName(labelGroupName) }
    }

    @Test
    fun addUrlImageWithOD_doOD_successful() {
        addImage(true)
    }

    @Test
    fun addFileImageWithOD_doOD_successful() {
        addImage(false)
    }

    private fun addImage(urlImage: Boolean) {
        val content = TestImages.getInputStream1()
        val originalFilename = "file.jpg"
        val childOriginalName = "${uuid}-detected0.jpg"
        val url = "http://www.some-page.com/$originalFilename"
        val tags = setOf("tag1")
        val childId1 = "childId1"
        val odLabel1 = "odLabel1"
        val odLabel2 = "odLabel2"
        val odLabels = setOf(odLabel1, odLabel2)
        val nonOdLabelGroup = "nonOdLabelGroup"
        val nonOdLabel = "nonOdLabel"
        val odWantedLabels = setOf(odLabel1)
        val nonOdWantedLabels = setOf(nonOdLabel)
        val relativeBoundingBox = RelativeBoundingBox(0.1, 0.1, 0.1, 0.1)
        val parentMetadata = ObjectMetadata(uuid, user, jpg, tags, emptyMap())
        val parentMetadataCopy = ObjectMetadata(uuid, user, jpg, tags, emptyMap())
        val parentMetadataWithChild = ObjectMetadata(
            uuid, user, jpg, tags, emptyMap(),
            mapOf(
                ChildrenImages.OTHER_METADATA_NAME to ChildrenImages(listOf(ChildImage(childId1, relativeBoundingBox))),
                ObjectDetectingConstants.TEMPLATE_DATA_NAME to ObjectsDetectingData(
                    mutableMapOf(
                        nonOdLabelGroup to mutableMapOf(
                            nonOdLabel to ObjectDetectingData()
                        )
                    )
                )
            )
        )
        val childLabeling = Labeling(LabelStatistics(mutableMapOf(
            odLabel1 to LabelStatistic(1, 1),
            odLabel2 to LabelStatistic(-1, 1))
        ))
        val childMetadata = ObjectMetadata(
            childId1, user, jpg, tags,
            mapOf(ObjectDetectionConstants.LABEL_GROUP to childLabeling),
            mapOf(ParentImage.OTHER_METADATA_NAME to ParentImage(uuid))
        )

        every { labelGroupRepository.findByName(ObjectDetectionConstants.LABEL_GROUP) } returns
                LabelGroupLimited(ObjectDetectionConstants.LABEL_GROUP, odLabels, 1)
        every { labelGroupRepository.findByName(nonOdLabelGroup) } returns
                LabelGroupLimited(nonOdLabelGroup, nonOdWantedLabels, 1)
        every { objectService.saveFileObject(any(), originalFilename) } returns
                ObjectStorageInfo(uuid, originalFilename, "$uuid.jpg", ObjectRepositoryType.FILESYSTEM)
        every { objectService.saveUrlObject(url) } returns
                ObjectStorageInfo(uuid, originalFilename, url, ObjectRepositoryType.URL)
        every { objectMetadataRepo.insert(parentMetadata) } returns
                parentMetadata
        every { objectService.getImageById(parentMetadata.id) } returns
                TestImages.IMAGE_1
        every { objectService.saveFileObject(any(), childOriginalName) } returns
                ObjectStorageInfo(childId1, childOriginalName, "$childId1.jpg", ObjectRepositoryType.FILESYSTEM)
        every { objectDetectionService.detectObjectsWithOverlaps(any<BufferedImage>(), odWantedLabels) } returns
                listOf(DetectedObjectWithOverlappingLabels(mapOf(odLabel1 to 0.81), relativeBoundingBox))
        every { objectDetectionService.getSupportedLabels() } returns
                odLabels.toSet()
        every { objectMetadataRepo.insert(childMetadata) } returns
                childMetadata
        every { objectMetadataRepo.save(parentMetadataWithChild) } returns
                parentMetadataWithChild

        val result = if (urlImage) {
            objectMetadataService.addUrlImageWithOD(
                UrlImageCreateDTO(
                    url,
                    ObjectMetadataCreateDTO(emptyMap(), tags),
                    ObjectDetectionDTO(
                        ObjectDetectionParametersDTO(
                            mapOf(
                                ObjectDetectionConstants.LABEL_GROUP to odWantedLabels,
                                nonOdLabelGroup to nonOdWantedLabels
                            ), 0.8, 0.9
                        ), null
                    )
                ), user
            )
        } else {
            objectMetadataService.addFileImageWithOD(
                content, originalFilename, FileImageCreateDTO(
                    ObjectMetadataCreateDTO(emptyMap(), tags),
                    ObjectDetectionDTO(
                        ObjectDetectionParametersDTO(
                            mapOf(
                                ObjectDetectionConstants.LABEL_GROUP to odWantedLabels,
                                nonOdLabelGroup to nonOdWantedLabels
                            ), 0.8, 0.9
                        ), null
                    )
                ), user
            )
        }
        val expected = listOf(
            parentMetadataWithChild,
            childMetadata
        )

        assertEquals(expected, result)

        if (urlImage) {
            verify { objectService.saveUrlObject(url) }
            verify { objectService.getImageById(parentMetadata.id) }
        } else {
            verify { objectService.saveFileObject(any(), originalFilename) }
        }
        verify { labelGroupRepository.findByName(ObjectDetectionConstants.LABEL_GROUP) }
        verify { labelGroupRepository.findByName(nonOdLabelGroup) }
        verify { objectService.saveFileObject(any(), childOriginalName) }
        verify { objectDetectionService.detectObjectsWithOverlaps(any<BufferedImage>(), odWantedLabels) }
        verify { objectDetectionService.getSupportedLabels() }
        verify { objectMetadataRepo.insert(childMetadata) }
        verify { objectMetadataRepo.save(parentMetadataWithChild) }
        verify { objectMetadataRepo.insert(parentMetadataCopy) }
        content.close()
    }
}
