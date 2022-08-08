package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.Utils.selectRandom
import cz.opendatalab.captcha.datamanagement.dto.*
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.objectdetection.ObjectDetectionConstants
import cz.opendatalab.captcha.objectdetection.ObjectDetectionService
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectDetectingConstants
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectDetectingData
import cz.opendatalab.captcha.task.templates.objectdetectingtemplate.ObjectLocalizationData
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import kotlin.math.abs

@Service
class ObjectMetadataService(private val objectMetadataRepo: ObjectMetadataRepository,
                            private val objectService: ObjectService,
                            private val labelGroupRepo: LabelGroupRepository,
                            private val objectDetectionService: ObjectDetectionService
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

        val metadata = createObjectMetadata(fileId, user, urlObjectCreateDto.fileType.toDomain(), urlObjectCreateDto.metadata.labels, urlObjectCreateDto.metadata.tags)
        objectMetadataRepo.insert(metadata)

        return fileId
    }

    fun addFileObject(file: MultipartFile, fileObjectCreateDTO: FileObjectCreateDTO, user: String): String {
        val fileId = objectService.saveFile(user, file, fileObjectCreateDTO.fileType.toDomain())

        val metadata = createObjectMetadata(fileId, user, fileObjectCreateDTO.fileType.toDomain(), fileObjectCreateDTO.metadata.labels, fileObjectCreateDTO.metadata.tags)
        objectMetadataRepo.insert(metadata)

        return fileId
    }

    fun addUrlImage(urlImageCreateDTO: UrlImageCreateDTO, user: String): List<String> {
        checkObjectDetectionParameters(urlImageCreateDTO.objectDetection)

        val imageFileTypeDTO = urlImageCreateDTO.fileType
        val tags = urlImageCreateDTO.metadata.tags

        val parentId = objectService.saveURLFile(user, urlImageCreateDTO.url)
        val parentMetadata = createObjectMetadata(parentId, user, imageFileTypeDTO.toDomain(), urlImageCreateDTO.metadata.labels, tags)
        objectMetadataRepo.insert(parentMetadata)

        return processWantedLabels(urlImageCreateDTO.objectDetection, imageFileTypeDTO, user, tags, parentMetadata)
    }

    fun addFileImage(file: MultipartFile, fileImageCreateDTO: FileImageCreateDTO, user: String): List<String> {
        checkObjectDetectionParameters(fileImageCreateDTO.objectDetection)

        val imageFileTypeDTO = fileImageCreateDTO.fileType
        val tags = fileImageCreateDTO.metadata.tags

        val parentId = objectService.saveFile(user, file, fileImageCreateDTO.fileType.toDomain())
        val parentMetadata = createObjectMetadata(parentId, user, imageFileTypeDTO.toDomain(), fileImageCreateDTO.metadata.labels, tags)
        objectMetadataRepo.insert(parentMetadata)

        return processWantedLabels(fileImageCreateDTO.objectDetection, imageFileTypeDTO, user, tags, parentMetadata)
    }

    private fun checkObjectDetectionParameters(objectDetectionParametersDTO: ObjectDetectionParametersDTO) {
        checkThresholds(objectDetectionParametersDTO.thresholdOneVote, objectDetectionParametersDTO.thresholdTwoVotes)
        checkLabelsExist(objectDetectionParametersDTO.wantedLabels)
    }

    private fun checkThresholds(thresholdOneVote: Double, thresholdTwoVotes: Double) {
        if (thresholdOneVote < 0.0 || thresholdOneVote > 1.0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,
                "thresholdOneVote must be between 0 and 1. Currently set: $thresholdOneVote")
        }
        if (thresholdTwoVotes < 0.0 || thresholdTwoVotes > 1.0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,
                "thresholdTwoVotes must be between 0 and 1. Currently set: $thresholdTwoVotes")
        }
        if (thresholdTwoVotes < thresholdOneVote) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,
                "thresholdTwoVotes cannot be smaller than thresholdOneVote")
        }
    }

    private fun checkLabelsExist(labels: Map<String, List<String>>) {
        for ((labelGroupName, labelList) in labels) {
            val labelGroup = getLabelGroup(labelGroupName) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Label group $labelGroupName not found")
            for (label in labelList) {
                if (!labelGroup.rangeContainsLabel(label)) {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Label $label not found in label group $labelGroupName")
                }
            }
        }
    }

    private fun processWantedLabels(
        objectDetectionParametersDTO: ObjectDetectionParametersDTO,
        imageFileTypeDTO: ImageFileTypeDTO,
        user: String,
        tags: List<String>,
        parentMetadata: ObjectMetadata
    ): List<String> {
        val ids = mutableListOf<String>()
        for ((wantedLabelGroup, wantedLabels) in objectDetectionParametersDTO.wantedLabels) {
            if (wantedLabelGroup == ObjectDetectionConstants.LABEL_GROUP) {
                val detectedIds = detectObjects(
                    parentMetadata,
                    imageFileTypeDTO,
                    user,
                    wantedLabels,
                    objectDetectionParametersDTO.thresholdOneVote,
                    objectDetectionParametersDTO.thresholdTwoVotes,
                    tags
                )
                ids.addAll(detectedIds)
            } else {
                addDataForObjectDetectingTask(parentMetadata, wantedLabelGroup, wantedLabels)
            }
        }
        objectMetadataRepo.save(parentMetadata)
        ids.add(parentMetadata.objectId)
        return ids
    }

    private fun detectObjects(
        parentMetadata: ObjectMetadata,
        imageFileTypeDTO: ImageFileTypeDTO,
        user: String,
        wantedLabels: List<String>,
        thresholdOneVote: Double,
        thresholdTwoVotes: Double,
        tags: List<String>
    ): List<String> {
        val detectedImages = objectDetectionService.detectObjects(
            parentMetadata.objectId, imageFileTypeDTO.format, user, wantedLabels
        )
        for (detectedImage in detectedImages) {
            val labeling = calculateLabeling(
                detectedImage.labels,
                thresholdOneVote, thresholdTwoVotes
            )
            val childMetadata = ObjectMetadata(
                detectedImage.id, user, imageFileTypeDTO.toDomain(),
                mutableMapOf(ObjectDetectionConstants.LABEL_GROUP to labeling),
                mutableMapOf(PARENT_FILE_TEMPLATE_NAME to ParentFile(parentMetadata.objectId)), tags
            )
            objectMetadataRepo.insert(childMetadata)
        }
        val ids = detectedImages.map { it.id }
        if (detectedImages.isNotEmpty()) {
            parentMetadata.templateData[CHILDREN_FILES_TEMPLATE_NAME] = ChildrenFiles(ids.toMutableList())
        }
        return ids
    }

    private fun addDataForObjectDetectingTask(
        parentMetadata: ObjectMetadata,
        labelGroupName: String,
        labelList: List<String>
    ) {
        parentMetadata.templateData.putIfAbsent(
            ObjectDetectingConstants.TEMPLATE_DATA_NAME,
            ObjectDetectingData(mutableMapOf())
        )
        val objectDetectingData =
            parentMetadata.templateData[ObjectDetectingConstants.TEMPLATE_DATA_NAME]!! as ObjectDetectingData
        objectDetectingData.objects[labelGroupName] =
            mutableMapOf(*labelList.map { it to ObjectLocalizationData(false, emptyList(), mutableListOf()) }
                .toTypedArray())
    }

    private fun calculateLabeling(labels: Map<String, Double>, thresholdOneVote: Double, thresholdTwoVotes: Double): Labeling {
        val labelStatistics = LabelStatistics()
        for (label in objectDetectionService.getSupportedLabels()) {
            val probability = labels[label] ?: -1.0
            val statisticValue = if (probability > thresholdTwoVotes) {
                2
            } else if (probability > thresholdOneVote) {
                1
            } else if (probability > 0.0) {
                0
            } else {
                -1
            }
            labelStatistics.statistics[label] = LabelStatistic(statisticValue, abs(statisticValue))
        }
        return Labeling(false, emptyList(), emptyList(), labelStatistics)
    }

    private fun createObjectMetadata(fileId: String, user: String, objectType: ObjectType, labelStrings: Map<String, List<String>>, tags: List<String>): ObjectMetadata {
        val labels = labelStrings.mapValues { (labelGroupName, labels) ->
            val labelGroup =  labelGroupRepo.findByName(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label group name  with name $labelGroupName does not exist")
            Labeling(labels.map { label ->
                if (!labelGroup.rangeContainsLabel(label))
                    throw throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Label $label does not belong to labelgroup $labelGroup range")
                label
            })
        }.toMutableMap()

        return ObjectMetadata(fileId, user, objectType, labels, tags)
    }

    companion object {
        const val PARENT_FILE_TEMPLATE_NAME = "parent-file"
        const val CHILDREN_FILES_TEMPLATE_NAME = "children-files"
    }
}
