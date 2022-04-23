package com.example.captcha.datamanagement.objectmetadata

import com.example.captcha.Utils.selectRandom
import com.example.captcha.datamanagement.dto.LabelGroupCreateDTO
import com.example.captcha.datamanagement.objectmetadata.dto.UrlObjectCreateDTO
import com.example.captcha.datamanagement.objectstorage.ObjectService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ObjectMetadataService(private val objectMetadataRepo: ObjectMetadataRepository,
                            private val objectService: ObjectService) {
    fun getAll(): List<ObjectMetadata> {
        return objectMetadataRepo.getAll()
    }

    fun getById(id: String): ObjectMetadata? {
        return objectMetadataRepo.getById(id)
    }

    fun getLabelGroups(): List<LabelGroup> {
        return objectMetadataRepo.getLabelGroups()
    }

    fun getAllAccessible(currentUser: String): List<ObjectMetadata> {
        return getAll().filter { !it.tags.contains("private") || it.user == currentUser }
    }

    fun getFiltered(currentUser: String, tags: List<String>, owners: List<String>): List<ObjectMetadata> {
        return getAllAccessible(currentUser)
            .filter { (owners.isEmpty() || owners.contains(it.user)) && it.tags.containsAll(tags) }
    }

    fun getFiltered(currentUser: String, tags: List<String>, owners: List<String>, objectType: ObjectTypeEnum): List<ObjectMetadata> {
        return getFiltered(currentUser, tags, owners).filter { it.objectType.type() == objectType }
    }

    fun getRandomWithLabel(objects: List<ObjectMetadata>, labelGroup: String, label: Label, count: Int): List<ObjectMetadata> {
        return selectRandom(getFilesWithLabel(objects, labelGroup, label), count)
    }

    fun getRandomWithoutLabel(objects: List<ObjectMetadata>, labelGroup: String, label: Label, count: Int): List<ObjectMetadata> {
        return selectRandom(getFilesWithoutLabel(objects, labelGroup, label), count)
    }

    fun getRandomNotKnowingLabel(objects: List<ObjectMetadata>, labelGroup: String, label: Label, count: Int): List<ObjectMetadata> {
        return selectRandom(getImagesNotKnowingLabel(objects, labelGroup, label), count)
    }

    fun getFilesWithLabel(objects: List<ObjectMetadata>, labelGroupName: String, label: Label): List<ObjectMetadata> {
        return objects.filter { it.containsLabel(labelGroupName, label) }
    }

    fun getFilesWithoutLabel(objects: List<ObjectMetadata>, labelGroup: String, label: Label): List<ObjectMetadata> {
        return objectMetadataRepo.getAll().filter {
            it.containsNegativeLabel(labelGroup, label)
        }
    }

    fun getImagesNotKnowingLabel(objects: List<ObjectMetadata>, labelGroup: String, label: Label): List<ObjectMetadata> {
        return objectMetadataRepo.getAll().filter {
            it.containsUnresolvedLabel(labelGroup, label)
        }
    }

    fun getLabelGroup(labelGroupName: String): LabelGroup? {
        return objectMetadataRepo.getLabelGroupByName(labelGroupName)
    }

    fun getLimitedLabelGroup(labelGroupName: String): LabelGroupLimited? {
        return getLabelGroup(labelGroupName)?.let { labelGroup ->
            when(labelGroup) {
                is LabelGroupLimited -> labelGroup
                else -> null
            }
        }
    }

    fun createLabelGroup(labelGroupCreate: LabelGroupCreateDTO) {
        if (labelGroupCreate.maxCardinality <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group max cardinality must be positive number, found ${labelGroupCreate.maxCardinality}")
        }
        val labelGroup = if (labelGroupCreate.labels.isEmpty()) {
            labelGroupCreate.toUnlimitedLabelGroup()
        } else {
            labelGroupCreate.toLimitedLabelGroup()
        }

        if (labelGroupCreate.maxCardinality <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group max cardinality must be positive number, found ${labelGroupCreate.maxCardinality}")
        }

        if (!objectMetadataRepo.add(labelGroup)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group with name ${labelGroupCreate.name} already exists")
        }
    }

    fun labelObject(objectId: String, labelGroupName: String, label: Label, positiveLabel: Boolean = true) {
        // first check validity
        val labelGroup = objectMetadataRepo.getLabelGroupByName(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group with name $labelGroupName does not exist")
        require(labelGroup.rangeContainsLabel(label)) // system issue

        val metadata = objectMetadataRepo.getById(objectId) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Image with fileId $objectId not found")
        metadata.label(label, labelGroupName, positiveLabel, labelGroup.maxCardinality, labelGroup.rangeSize())

        objectMetadataRepo.updateFile(metadata)
    }

    fun addUrlObject(urlObjectCreateDto: UrlObjectCreateDTO, user: String): String {
        val fileId = objectService.saveURLFile(user, urlObjectCreateDto.url)

        val labels = urlObjectCreateDto.metadata.labels.mapValues { (labelGroupName, labels) ->
            val labelGroup =  objectMetadataRepo.getLabelGroupByName(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group name  with name $labelGroupName does not exist")
            Labeling(labels.map { labelValue ->
                val label = Label(labelValue)
                if (!labelGroup.rangeContainsLabel(label))
                    throw throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label $labelValue does not belong to labelgroup $labelGroup range")
                label
            })
        }.toMutableMap()
        val fileType = urlObjectCreateDto.fileType.toDomain()

        val metadata = ObjectMetadata(fileId, user, fileType, labels, urlObjectCreateDto.metadata.tags)
        objectMetadataRepo.saveObject(metadata)

        return fileId
    }
}
