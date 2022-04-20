package com.example.captcha.datamanagement.objectmetadata

import com.example.captcha.datamanagement.objectstorage.ObjectService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test



internal class ObjectMetadataServiceTest {

    private val repo: ObjectMetadataRepository = mockk()
    private val objectService: ObjectService = mockk()
    val service = ObjectMetadataService(repo, objectService)


    @Test
    fun `getLimitedLabelGroup non empty`() {
        val labelGroupName = "labelGroupName"
        every { repo.getLabelGroupByName(labelGroupName) } returns LabelGroupLimited("name", emptyList(), 2)

        val result = service.getLimitedLabelGroup(labelGroupName)

        assertNotNull(result)
    }

    @Test
    fun `getLimitedLabelGroup empty`() {
        val labelGroupName = "labelGroupNameNonExistent"
        every { repo.getLabelGroupByName(labelGroupName) } returns null

        val result = service.getLimitedLabelGroup(labelGroupName)

        assertNull(result)
    }

    @Test
    fun `getLimitedLabelGroup not limited`() {
        val labelGroupName = "labelGroupName"
        every { repo.getLabelGroupByName(labelGroupName) } returns LabelGroup("name", 2)

        val result = service.getLimitedLabelGroup(labelGroupName)

        assertNull(result)
    }
}
