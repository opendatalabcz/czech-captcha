package com.example.captcha.datamanagement.objectmetadata

import org.springframework.stereotype.Service

interface ObjectMetadataRepository {
    fun getLabelGroups(): List<LabelGroup>
    fun updateFile(newMetadata: ObjectMetadata)
    fun saveObject(metadata: ObjectMetadata)
    fun getAll(): List<ObjectMetadata>
    fun getById(objectId: Long): ObjectMetadata?
    fun add(labelGroup: LabelGroup): Boolean
    fun getLabelGroupByName(labelGroupName: String): LabelGroup?
}

@Service
class InMemoObjectMetadataRepo : ObjectMetadataRepository {
    private val files = mutableListOf(
        ObjectMetadata(1L, "system", ImageObjectType("png"), Pair("animals", Labeling(listOf(Label("cat"))))),
        ObjectMetadata(2L, "system", ImageObjectType("png"), Pair("animals", Labeling(listOf(Label("dog"))))),
    )

    private val labelGroups = mutableListOf<LabelGroup>(
        LabelGroupLimited("animals", listOf(Label("cat"), Label("dog")), 1)
    )

    override fun getLabelGroups(): List<LabelGroup> {
        return labelGroups
    }

    override fun updateFile(newMetadata: ObjectMetadata) {
        val index = files.indexOfFirst { it.objectId == newMetadata.objectId }
        files[index] = newMetadata
    }

    override fun saveObject(metadata: ObjectMetadata) {
        files.add(metadata)
    }

    override fun getAll(): List<ObjectMetadata> {
        return files
    }

    override fun getById(objectId: Long): ObjectMetadata? {
        return files.find { it.objectId == objectId }
    }

    override fun getLabelGroupByName(labelGroupName: String): LabelGroup? {
        return labelGroups.find { it.name == labelGroupName }
    }

    override fun add(labelGroup: LabelGroup): Boolean {
        if (labelGroups.any { it.name == labelGroup.name }) {
            return false
        }

        return labelGroups.add(labelGroup)
    }
}
