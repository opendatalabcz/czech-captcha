package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.Utils.selectRandom
import cz.opendatalab.captcha.datamanagement.dto.LabelGroupCreateDTO
import cz.opendatalab.captcha.datamanagement.dto.UrlObjectCreateDTO
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ObjectMetadataService(private val objectMetadataRepo: ObjectMetadataRepository,
                            private val objectService: ObjectService,
                            private val labelGroupRepo: LabelGroupRepository
                            ) {
    fun getAll(): List<ObjectMetadata> {
        return objectMetadataRepo.findAll()
    }

    fun getById(objectId: String): ObjectMetadata? {
        return objectMetadataRepo.findByObjectId(objectId)
    }

    fun getLabelGroups(): List<LabelGroup> {
        return labelGroupRepo.findAll()
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

    fun getRandomWithLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String, count: Int): List<ObjectMetadata> {
        return selectRandom(getFilesWithLabel(objects, labelGroup, label), count)
    }

    fun getRandomWithoutLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String, count: Int): List<ObjectMetadata> {
        return selectRandom(getFilesWithoutLabel(objects, labelGroup, label), count)
    }

    fun getRandomNotKnowingLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String, count: Int): List<ObjectMetadata> {
        return selectRandom(getObjectsNotKnowingLabel(objects, labelGroup, label), count)
    }

    fun getFilesWithLabel(objects: List<ObjectMetadata>, labelGroupName: String, label: String): List<ObjectMetadata> {
        return objects.filter { it.containsLabel(labelGroupName, label) }
    }

    fun getFilesWithoutLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String): List<ObjectMetadata> {
        return objectMetadataRepo.findAll().filter {
            it.containsNegativeLabel(labelGroup, label)
        }
    }

    fun getObjectsNotKnowingLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String): List<ObjectMetadata> {
        return objectMetadataRepo.findAll().filter {
            it.containsUnresolvedLabel(labelGroup, label)
        }
    }

    fun getLabelGroup(labelGroupName: String): LabelGroup? {
        return labelGroupRepo.findByName(labelGroupName)
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

        if (!labelGroupRepo.existsByName(labelGroup.name)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group with name ${labelGroupCreate.name} already exists")
        }
        labelGroupRepo.insert(labelGroup)
    }

    fun labelObject(objectId: String, labelGroupName: String, label: String, positiveLabel: Boolean = true) {
        // first check validity
        val labelGroup = labelGroupRepo.findByName(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group with name $labelGroupName does not exist")
        require(labelGroup.rangeContainsLabel(label)) // system issue

        val metadata = objectMetadataRepo.findByObjectId(objectId) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Object with fileId $objectId not found")
        metadata.label(label, labelGroupName, positiveLabel, labelGroup.maxCardinality, labelGroup.rangeSize())

        objectMetadataRepo.save(metadata)
    }

    fun addUrlObject(urlObjectCreateDto: UrlObjectCreateDTO, user: String): String {
        val fileId = objectService.saveURLFile(user, urlObjectCreateDto.url)

        val labels = urlObjectCreateDto.metadata.labels.mapValues { (labelGroupName, labels) ->
            val labelGroup =  labelGroupRepo.findByName(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group name  with name $labelGroupName does not exist")
            Labeling(labels.map { label ->
                if (!labelGroup.rangeContainsLabel(label))
                    throw throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label $label does not belong to labelgroup $labelGroup range")
                label
            })
        }.toMutableMap()
        val fileType = urlObjectCreateDto.fileType.toDomain()

        val metadata = ObjectMetadata(fileId, user, fileType, labels, urlObjectCreateDto.metadata.tags)
        objectMetadataRepo.insert(metadata)

        return fileId
    }
}
