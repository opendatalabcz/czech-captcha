package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test



internal class ObjectMetadataServiceTest {

    private val objectMetadataRepo: ObjectMetadataRepository = mockk()
    private val labegroupRepo: LabelGroupRepository = mockk()
    private val objectService: ObjectService = mockk()
    val service = ObjectMetadataService(objectMetadataRepo, objectService, labegroupRepo)


    @Test
    fun `getLimitedLabelGroup non empty`() {
        val labelGroupName = "labelGroupName"
        every { labegroupRepo.findByName(labelGroupName) } returns LabelGroupLimited("name", emptyList(), 2)

        val result = service.getLimitedLabelGroup(labelGroupName)

        assertNotNull(result)
    }

    @Test
    fun `getLimitedLabelGroup empty`() {
        val labelGroupName = "labelGroupNameNonExistent"
        every { labegroupRepo.findByName(labelGroupName) } returns null

        val result = service.getLimitedLabelGroup(labelGroupName)

        assertNull(result)
    }

    @Test
    fun `getLimitedLabelGroup not limited`() {
        val labelGroupName = "labelGroupName"
        every { labegroupRepo.findByName(labelGroupName) } returns LabelGroup("name", 2)

        val result = service.getLimitedLabelGroup(labelGroupName)

        assertNull(result)
    }
}
