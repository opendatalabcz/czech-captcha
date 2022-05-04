package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.datamanagement.dto.LabelGroupCreateDTO
import cz.opendatalab.captcha.datamanagement.dto.ObjectMetadataCreateDTO
import cz.opendatalab.captcha.datamanagement.dto.TextFileTypeDTO
import cz.opendatalab.captcha.datamanagement.dto.UrlObjectCreateDTO
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectCatalogue
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.web.server.ResponseStatusException


@SpringBootTest
internal class ObjectMetadataServiceIntTest(@Autowired val objectMetadataService: ObjectMetadataService,
                                            @MockBean @Autowired val objectCatalogue: ObjectCatalogue,
                                            @MockBean @Autowired val objectMetadataRepo: ObjectMetadataRepository,
                                            @MockBean @Autowired val labelRepo: LabelGroupRepository,
                                         ) {
    val user1 = "user1"
    val user2 = "user2"
    val user3 = "user3"

    val label = "label"
    val labelGroupName = "labelGroupName"
    val labelingWithLabel = Labeling(listOf(label))
    val labelingWithoutLabel = Labeling(emptyList())

    val labelGroupLabelingWithLabel = Pair(labelGroupName, labelingWithLabel)
    val labelGroupLabelingWithoutLabel = Pair(labelGroupName, labelingWithoutLabel)
    // with 1, 3; without 6; not knowing 2,4,5

    private val metadata1 = ObjectMetadata("1", user1, ImageObjectType("png"), labelGroupLabelingWithLabel)
    private val metadata2 = ObjectMetadata("2", user1, ImageObjectType("png"), mutableMapOf(), listOf("animal", "winter"))
    private val metadata3 = ObjectMetadata("3", user2, ImageObjectType("png"), labelGroupLabelingWithLabel)
    private val metadata4 = ObjectMetadata("4", user2, ImageObjectType("png"), mutableMapOf(), listOf("private"))
    private val metadata5 = ObjectMetadata("5", user2, TextObjectType)
    private val metadata6 = ObjectMetadata("6", user3, ImageObjectType("png"), labelGroupLabelingWithoutLabel)



    val storedConfigs = listOf(metadata1, metadata2, metadata3, metadata4, metadata5, metadata6)

    @Test
    fun getAllAccessible() {
        `when`(objectMetadataRepo.findAll()).thenReturn(storedConfigs)

        val result = objectMetadataService.getAllAccessible(user1)

        assertThat(result).isEqualTo(listOf(metadata1, metadata2, metadata3, metadata5, metadata6))
    }

    @Test
    fun `getFiltered full`() {
        `when`(objectMetadataRepo.findAll()).thenReturn(storedConfigs)

        val result = objectMetadataService.getFiltered(user1, listOf(), listOf(user1, user2), ObjectTypeEnum.IMAGE)

        assertThat(result).isEqualTo(listOf(metadata1, metadata2, metadata3))
    }

    @Test
    fun `getFiltered full animal tag`() {
        `when`(objectMetadataRepo.findAll()).thenReturn(storedConfigs)

        val result = objectMetadataService.getFiltered(user1, listOf("animal", "winter"), listOf(user1, user2), ObjectTypeEnum.IMAGE)

        assertThat(result).isEqualTo(listOf(metadata2))
    }

    @Test
    fun addUrlObject() {
        val user = "user"
        val objectDTO = UrlObjectCreateDTO("url", TextFileTypeDTO, ObjectMetadataCreateDTO(emptyMap(), emptyList()))

        objectMetadataService.addUrlObject(objectDTO, user)

        verify(objectCatalogue).insert(any(ObjectStorageInfo::class.java))
        verify(objectMetadataRepo).insert(any(ObjectMetadata::class.java))
    }

    @Test
    fun labelObject() {
        val objectId = "2"

        val maxCardinality = 1
        val labelGroup = LabelGroup(labelGroupName, maxCardinality)

        val expectedLabeling = Labeling().recordLabel(true, label, maxCardinality, Int.MAX_VALUE)
        val expectedLabels = mutableMapOf(Pair(labelGroupName, expectedLabeling))
        val expected = metadata2.copy(labels = expectedLabels)

        `when`(objectMetadataRepo.findByObjectId(objectId)).thenReturn(metadata2)
        `when`(labelRepo.findByName(labelGroupName)).thenReturn(labelGroup)

        objectMetadataService.labelObject(objectId, labelGroupName, label)

        verify(objectMetadataRepo).save(expected)
    }

    @Test
    fun `createLabelGroup success`() {
        val labelGroupName = "labelGroupName"
        val maxCardinality = 1

        val expected = LabelGroup(labelGroupName, maxCardinality)

        val labelGroupCreateDTO = LabelGroupCreateDTO(labelGroupName, emptyList(), maxCardinality)
        `when`(labelRepo.existsByName(labelGroupName)).thenReturn(false)


        objectMetadataService.createLabelGroup(labelGroupCreateDTO)

        verify(labelRepo).insert(expected)
    }

    @Test
    fun `createLabelGroup fail maxcardinality`() {
        val labelGroupName = "labelGroupName"
        val maxCardinality = 0

        val labelGroupCreateDTO = LabelGroupCreateDTO(labelGroupName, emptyList(), maxCardinality)
        `when`(labelRepo.existsByName(labelGroupName)).thenReturn(false)

        assertThrows<ResponseStatusException> {
            objectMetadataService.createLabelGroup(labelGroupCreateDTO)
        }
    }

    @Test
    fun `createLabelGroup fail already exists`() {
        val labelGroupName = "labelGroupName"
        val maxCardinality = 1

        val labelGroupCreateDTO = LabelGroupCreateDTO(labelGroupName, emptyList(), maxCardinality)
        `when`(labelRepo.existsByName(labelGroupName)).thenReturn(true)

        assertThrows<ResponseStatusException> {
            objectMetadataService.createLabelGroup(labelGroupCreateDTO)
        }
    }
    // with 1, 3; without 6; not knowing 2,4,5
    @Test
    fun `getObjectsNotKnowingLabel`() {

        val expected = listOf(metadata2, metadata4, metadata5)

        val result = objectMetadataService.getObjectsNotKnowingLabel(storedConfigs, labelGroupName, label)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getObjectsWithoutLabel`() {
        val expected = listOf(metadata6)

        val result = objectMetadataService.getObjectsWithoutLabel(storedConfigs, labelGroupName, label)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getObjectsWithLabel`() {

        val expected = listOf(metadata1, metadata3)

        val result = objectMetadataService.getObjectsWithLabel(storedConfigs, labelGroupName, label)

        assertThat(result).isEqualTo(expected)
    }
}
