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

internal class ObjectMetadataServiceTest {
    private val objectMetadataRepo: ObjectMetadataRepository = mockk()
    private val labelGroupRepository: LabelGroupRepository = mockk()
    private val objectService: ObjectService = mockk()
    private val objectDetectionService: ObjectDetectionService = mockk()

    private val objectMetadataService =
        ObjectMetadataService(objectMetadataRepo, objectService, labelGroupRepository, objectDetectionService)

    private val user = "user"
    private val jpg = "jpg"
    private val uuid = "123e4567-e89b-12d3-a456-426614174000"
    private val originalFilename = "originalFilename.$jpg"
    private val url = "http://www.some-page.com/$originalFilename"
    private val label = "label"
    private val labelGroupName = "labelGroup"
    private val labels = setOf(label)
    private val tag = "tag"
    private val tags = setOf(tag)
    private val emptyObjectMetadataCreateDTO = ObjectMetadataCreateDTO(emptyMap(), emptySet())
    private val urlStorageInfo = ObjectStorageInfo(uuid, originalFilename, url, ObjectRepositoryType.URL)
    private val fileStorageInfo = ObjectStorageInfo(uuid, originalFilename, "$uuid.$jpg", ObjectRepositoryType.FILESYSTEM)
    private val metadata = ObjectMetadata(
        uuid, user, jpg, tags, mapOf(labelGroupName to Labeling(labels))
    )
    private val metadataCopy = ObjectMetadata(
        uuid, user, jpg, tags, mapOf(labelGroupName to Labeling(labels))
    )

    private val relativeBoundingBox = RelativeBoundingBox(0.1, 0.1, 0.1, 0.1)
    private val metadataWithAnnotations = ObjectMetadata(
        uuid, user, jpg, tags, mapOf(labelGroupName to Labeling(labels)), mapOf(
            ObjectDetectingConstants.TEMPLATE_DATA_NAME to ObjectsDetectingData(
                mutableMapOf(
                    labelGroupName to mutableMapOf(
                        label to ObjectDetectingData(mutableListOf(relativeBoundingBox))
                    )
                )
            )
        )
    )

    private val childUuid = "987e4567-e89b-12d3-a456-426614174111"
    private val odLabel1 = "odLabel1"
    private val odLabel2 = "odLabel2"
    private val odLabels = setOf(odLabel1, odLabel2)
    private val odWantedLabels = setOf(odLabel1)
    private val imageContent = TestImages.getInputStream1()
    private val childOriginalName = "${uuid}-detected0.$jpg"
    private val parentMetadata = ObjectMetadata(uuid, user, jpg, tags, emptyMap())
    private val parentMetadataCopy = ObjectMetadata(uuid, user, jpg, tags, emptyMap())
    private val parentMetadataWithChild = ObjectMetadata(
        uuid, user, jpg, tags, emptyMap(),
        mapOf(
            ChildrenImages.OTHER_METADATA_NAME to ChildrenImages(listOf(ChildImage(childUuid, relativeBoundingBox))),
            ObjectDetectingConstants.TEMPLATE_DATA_NAME to ObjectsDetectingData(labelGroupName, label)
        )
    )
    private val childLabeling = Labeling(LabelStatistics(mutableMapOf(
        odLabel1 to LabelStatistic(1, 1),
        odLabel2 to LabelStatistic(-1, 1))
    ))
    private val childMetadata = ObjectMetadata(
        childUuid, user, jpg, tags,
        mapOf(ObjectDetectionConstants.LABEL_GROUP to childLabeling),
        mapOf(ParentImage.OTHER_METADATA_NAME to ParentImage(uuid))
    )
    private val expectedODOutput = listOf(
        parentMetadataWithChild,
        childMetadata
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
                    ObjectMetadataCreateDTO(mapOf(labelGroupName to labels), tags)
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
                    ObjectMetadataCreateDTO(mapOf(labelGroupName to labels), tags)
                ), user
            )
        }

        verify { labelGroupRepository.findByName(labelGroupName) }
    }

    @Test
    fun addUrlObject_successful() {
        every { labelGroupRepository.findByName(labelGroupName) } returns LabelGroupLimited(labelGroupName, labels, 1)
        every { objectService.saveUrlObject(url) } returns urlStorageInfo
        every { objectMetadataRepo.insert(metadata) } returns metadata

        assertEquals(metadata, objectMetadataService.addUrlObject(
                UrlObjectCreateDTO(
                    url,
                    ObjectMetadataCreateDTO(mapOf(labelGroupName to labels), tags)
                ), user
            ))

        verify { labelGroupRepository.findByName(labelGroupName) }
        verify { objectService.saveUrlObject(url) }
        verify { objectMetadataRepo.insert(metadata) }
    }

    @Test
    fun addFileObject_successful() {
        every { labelGroupRepository.findByName(labelGroupName) } returns LabelGroupLimited(labelGroupName, labels, 1)
        every { objectService.saveFileObject(imageContent, originalFilename) } returns fileStorageInfo
        every { objectMetadataRepo.insert(metadata) } returns metadata

        assertEquals(metadata, objectMetadataService.addFileObject(
            imageContent,
            originalFilename,
            FileObjectCreateDTO(
                ObjectMetadataCreateDTO(mapOf(labelGroupName to labels), tags)
            ), user
        ))

        verify { labelGroupRepository.findByName(labelGroupName) }
        verify { objectService.saveFileObject(imageContent, originalFilename) }
        verify { objectMetadataRepo.insert(metadata) }
    }

    @Test
    fun addUrlImageWithOD_doODThreshold1OutOfRange_shouldThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            objectMetadataService.addUrlImageWithOD(
                UrlImageCreateDTO(
                    url,
                    emptyObjectMetadataCreateDTO,
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
                    emptyObjectMetadataCreateDTO,
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
                    emptyObjectMetadataCreateDTO,
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
                    emptyObjectMetadataCreateDTO,
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
                    emptyObjectMetadataCreateDTO,
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
    fun addUrlImageWithOD_addAnnotations_successful() {
        every { labelGroupRepository.findByName(labelGroupName) } returns LabelGroupLimited(labelGroupName, labels, 1)
        every { objectService.saveUrlObject(url) } returns urlStorageInfo
        every { objectMetadataRepo.insert(metadata) } returns metadata
        every { objectMetadataRepo.save(metadataWithAnnotations) } returns metadataWithAnnotations

        assertEquals(
            listOf(metadataWithAnnotations), objectMetadataService.addUrlImageWithOD(
            UrlImageCreateDTO(
                url,
                ObjectMetadataCreateDTO(mapOf(labelGroupName to labels), tags),
                ObjectDetectionDTO(null, listOf(AnnotationDTO(labelGroupName, label, relativeBoundingBox)))
            ), user
        ))

        verify { labelGroupRepository.findByName(labelGroupName) }
        verify { objectService.saveUrlObject(url) }
        verify { objectMetadataRepo.insert(metadataCopy) }
        verify { objectMetadataRepo.save(metadataWithAnnotations) }
    }

    @Test
    fun addFileImageWithOD_addAnnotations_successful() {
        every { labelGroupRepository.findByName(labelGroupName) } returns LabelGroupLimited(labelGroupName, labels, 1)
        every { objectService.saveFileObject(any(), originalFilename) } returns fileStorageInfo
        every { objectMetadataRepo.insert(metadata) } returns metadata
        every { objectMetadataRepo.save(metadataWithAnnotations) } returns metadataWithAnnotations

        assertEquals(
            listOf(metadataWithAnnotations), objectMetadataService.addFileImageWithOD(
                imageContent, originalFilename,
                FileImageCreateDTO(
                    ObjectMetadataCreateDTO(mapOf(labelGroupName to labels), tags),
                    ObjectDetectionDTO(null, listOf(AnnotationDTO(labelGroupName, label, relativeBoundingBox)))
                ), user
            ))

        verify { labelGroupRepository.findByName(labelGroupName) }
        verify { objectService.saveFileObject(any(), originalFilename) }
        verify { objectMetadataRepo.insert(metadataCopy) }
        verify { objectMetadataRepo.save(metadataWithAnnotations) }
    }

    @Test
    fun addUrlImageWithOD_doOD_successful() {
        every { objectService.saveUrlObject(url) } returns urlStorageInfo
        every { objectService.getImageById(parentMetadata.id) } returns TestImages.IMAGE_1
        initODMocks()

        val result = callAddUrlImageWithOD()
        assertEquals(expectedODOutput, result)

        verifyODMocks()
        verify { objectService.saveUrlObject(url) }
        verify { objectService.getImageById(parentMetadata.id) }
    }

    private fun callAddUrlImageWithOD(): List<ObjectMetadata> {
        return objectMetadataService.addUrlImageWithOD(
            UrlImageCreateDTO(
                url,
                ObjectMetadataCreateDTO(emptyMap(), tags),
                ObjectDetectionDTO(
                    ObjectDetectionParametersDTO(
                        mapOf(
                            ObjectDetectionConstants.LABEL_GROUP to odWantedLabels,
                            labelGroupName to labels
                        ), 0.8, 0.9
                    ), null
                )
            ), user
        )
    }

    @Test
    fun addFileImageWithOD_doOD_successful() {
        every { objectService.saveFileObject(any(), originalFilename) } returns fileStorageInfo
        initODMocks()

        val result = callAddFileImageWithOD()
        assertEquals(expectedODOutput, result)

        verifyODMocks()
        verify { objectService.saveFileObject(any(), originalFilename) }
    }

    private fun callAddFileImageWithOD(): List<ObjectMetadata> {
        return objectMetadataService.addFileImageWithOD(
            imageContent, originalFilename, FileImageCreateDTO(
                ObjectMetadataCreateDTO(emptyMap(), tags),
                ObjectDetectionDTO(
                    ObjectDetectionParametersDTO(
                        mapOf(
                            ObjectDetectionConstants.LABEL_GROUP to odWantedLabels,
                            labelGroupName to labels
                        ), 0.8, 0.9
                    ), null
                )
            ), user
        )
    }

    private fun initODMocks() {
        every { labelGroupRepository.findByName(ObjectDetectionConstants.LABEL_GROUP) } returns
                LabelGroupLimited(ObjectDetectionConstants.LABEL_GROUP, odLabels, 1)
        every { labelGroupRepository.findByName(labelGroupName) } returns
                LabelGroupLimited(labelGroupName, labels, 1)
        every { objectMetadataRepo.insert(parentMetadata) } returns
                parentMetadata
        every { objectService.saveFileObject(any(), childOriginalName) } returns
                ObjectStorageInfo(childUuid, childOriginalName, "$childUuid.$jpg", ObjectRepositoryType.FILESYSTEM)
        every { objectDetectionService.detectObjectsWithOverlaps(any<BufferedImage>(), odWantedLabels) } returns
                listOf(DetectedObjectWithOverlappingLabels(mapOf(odLabel1 to 0.81), relativeBoundingBox))
        every { objectDetectionService.getSupportedLabels() } returns
                odLabels.toSet()
        every { objectMetadataRepo.insert(childMetadata) } returns
                childMetadata
        every { objectMetadataRepo.save(parentMetadataWithChild) } returns
                parentMetadataWithChild
    }

    private fun verifyODMocks() {
        verify { labelGroupRepository.findByName(ObjectDetectionConstants.LABEL_GROUP) }
        verify { labelGroupRepository.findByName(labelGroupName) }
        verify { objectMetadataRepo.insert(parentMetadataCopy) }
        verify { objectService.saveFileObject(any(), childOriginalName) }
        verify { objectDetectionService.detectObjectsWithOverlaps(any<BufferedImage>(), odWantedLabels) }
        verify { objectDetectionService.getSupportedLabels() }
        verify { objectMetadataRepo.insert(childMetadata) }
        verify { objectMetadataRepo.save(parentMetadataWithChild) }
    }
}
