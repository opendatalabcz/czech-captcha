package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.Utils.selectRandom
import cz.opendatalab.captcha.datamanagement.dto.FileObjectCreateDTO
import cz.opendatalab.captcha.datamanagement.dto.FileTypeDTO
import cz.opendatalab.captcha.datamanagement.dto.LabelGroupCreateDTO
import cz.opendatalab.captcha.datamanagement.dto.UrlObjectCreateDTO
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
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
        return selectRandom(getObjectsWithLabel(objects, labelGroup, label), count)
    }

    fun getRandomWithoutLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String, count: Int): List<ObjectMetadata> {
        return selectRandom(getObjectsWithoutLabel(objects, labelGroup, label), count)
    }

    fun getRandomNotKnowingLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String, count: Int): List<ObjectMetadata> {
        return selectRandom(getObjectsNotKnowingLabel(objects, labelGroup, label), count)
    }

    fun getObjectsWithLabel(objects: List<ObjectMetadata>, labelGroupName: String, label: String): List<ObjectMetadata> {
        return objects.filter { it.containsLabel(labelGroupName, label) }
    }

    fun getObjectsWithoutLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String): List<ObjectMetadata> {
        return objects.filter {
            it.containsNegativeLabel(labelGroup, label)
        }
    }

    fun getObjectsNotKnowingLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String): List<ObjectMetadata> {
        return objects.filter {
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

        if (labelGroupRepo.existsByName(labelGroup.name)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group with name ${labelGroupCreate.name} already exists")
        }
        labelGroupRepo.insert(labelGroup)
    }

    fun labelObject(objectId: String, labelGroupName: String, label: String, positiveLabel: Boolean = true) {
        // first check validity
        val labelGroup = labelGroupRepo.findByName(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group with name $labelGroupName does not exist")
        require(labelGroup.rangeContainsLabel(label))

        val metadata = objectMetadataRepo.findByObjectId(objectId) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Object with fileId $objectId not found")
        metadata.label(label, labelGroupName, positiveLabel, labelGroup.maxCardinality, labelGroup.rangeSize())

        objectMetadataRepo.save(metadata)
    }

    fun addUrlObject(urlObjectCreateDto: UrlObjectCreateDTO, user: String): String {
        val fileId = objectService.saveURLFile(user, urlObjectCreateDto.url)

        val metadata = createObjectMetadata(fileId, user, urlObjectCreateDto.fileType, urlObjectCreateDto.metadata.labels, urlObjectCreateDto.metadata.tags)
        objectMetadataRepo.insert(metadata)

        return fileId
    }

    fun addFileObject(file: MultipartFile, fileObjectCreateDTO: FileObjectCreateDTO, user: String): String {
        val fileId = objectService.saveFile(user, file, fileObjectCreateDTO.fileType.toDomain())

        val metadata = createObjectMetadata(fileId, user, fileObjectCreateDTO.fileType, fileObjectCreateDTO.metadata.labels, fileObjectCreateDTO.metadata.tags)
        objectMetadataRepo.insert(metadata)

        return fileId
    }

    private fun createObjectMetadata(fileId: String, user: String, fileType: FileTypeDTO, labelStrings: Map<String, List<String>>, tags: List<String>): ObjectMetadata {
        val labels = labelStrings.mapValues { (labelGroupName, labels) ->
            val labelGroup =  labelGroupRepo.findByName(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group name  with name $labelGroupName does not exist")
            Labeling(labels.map { label ->
                if (!labelGroup.rangeContainsLabel(label))
                    throw throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label $label does not belong to labelgroup $labelGroup range")
                label
            })
        }.toMutableMap()

        return ObjectMetadata(fileId, user, fileType.toDomain(), labels, tags)
    }
}
