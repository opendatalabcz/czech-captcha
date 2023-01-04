package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.datamanagement.ImageUtils
import cz.opendatalab.captcha.Utils
import cz.opendatalab.captcha.Utils.getBytesFromInputStream
import cz.opendatalab.captcha.datamanagement.dto.*
import cz.opendatalab.captcha.datamanagement.objectdetection.DetectedObjectWithOverlappingLabels
import cz.opendatalab.captcha.datamanagement.objectdetection.ObjectDetectionConstants
import cz.opendatalab.captcha.datamanagement.objectdetection.ObjectDetectionService
import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.InputStream
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
        return objectMetadataRepo.findByIdOrNull(objectId)
    }

    fun getLabelGroups(): List<LabelGroup> {
        return labelGroupRepo.findAll()
    }

    fun getAllAccessible(currentUser: String): List<ObjectMetadata> {
        return getAll().filter { !it.tags.contains("private") || it.owner == currentUser }
    }

    fun getFiltered(currentUser: String, tags: Set<String>, owners: Set<String>): List<ObjectMetadata> {
        return getAllAccessible(currentUser)
            .filter { (owners.isEmpty() || owners.contains(it.owner)) && it.tags.containsAll(tags) }
    }

    fun getFiltered(
        currentUser: String,
        tags: Set<String>,
        owners: Set<String>,
        objectTypeEnum: ObjectTypeEnum
    ): List<ObjectMetadata> {
        return getFiltered(currentUser, tags, owners).filter { it.objectType.type == objectTypeEnum }
    }

    fun getRandomWithLabel(
        objects: List<ObjectMetadata>,
        labelGroup: String,
        label: String,
        count: Int
    ): List<ObjectMetadata> {
        return selectRandom(getObjectsWithLabel(objects, labelGroup, label), count)
    }

    fun getRandomWithoutLabel(
        objects: List<ObjectMetadata>,
        labelGroup: String,
        label: String,
        count: Int
    ): List<ObjectMetadata> {
        return selectRandom(getObjectsWithoutLabel(objects, labelGroup, label), count)
    }

    fun getRandomNotKnowingLabel(
        objects: List<ObjectMetadata>,
        labelGroup: String,
        label: String,
        count: Int
    ): List<ObjectMetadata> {
        return selectRandom(getObjectsNotKnowingLabel(objects, labelGroup, label), count)
    }

    fun getObjectsWithLabel(
        objects: List<ObjectMetadata>,
        labelGroupName: String,
        label: String
    ): List<ObjectMetadata> {
        return objects.filter { it.containsLabel(labelGroupName, label) }
    }

    fun getObjectsWithoutLabel(objects: List<ObjectMetadata>, labelGroup: String, label: String): List<ObjectMetadata> {
        return objects.filter {
            it.containsNegativeLabel(labelGroup, label)
        }
    }

    fun getObjectsNotKnowingLabel(
        objects: List<ObjectMetadata>,
        labelGroup: String,
        label: String
    ): List<ObjectMetadata> {
        return objects.filter {
            it.containsUnresolvedLabel(labelGroup, label)
        }
    }

    private fun <T> selectRandom(list: List<T>, count: Int): List<T> {
        return list.shuffled().take(count)
    }

    fun getLabelGroup(labelGroupName: String): LabelGroup? {
        return labelGroupRepo.findByName(labelGroupName)
    }

    fun getLimitedLabelGroup(labelGroupName: String): LabelGroupLimited? {
        return getLabelGroup(labelGroupName)?.let { labelGroup ->
            when (labelGroup) {
                is LabelGroupLimited -> labelGroup
                else -> null
            }
        }
    }

    fun createLabelGroup(labelGroupCreate: LabelGroupCreateDTO): LabelGroup {
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
        return labelGroupRepo.insert(labelGroup)
    }

    fun labelObject(objectId: String, labelGroupName: String, label: String, positiveLabel: Boolean = true) {
        // first check validity
        val labelGroup = labelGroupRepo.findByName(labelGroupName) ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Label group with name $labelGroupName does not exist"
        )
        require(labelGroup.rangeContainsLabel(label))

        val metadata = objectMetadataRepo.findByIdOrNull(objectId) ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Object with fileId $objectId not found"
        )
        metadata.label(label, labelGroupName, positiveLabel, labelGroup.maxCardinality, labelGroup.rangeSize())

        objectMetadataRepo.save(metadata)
    }

    fun addUrlObject(urlObjectCreateDto: UrlObjectCreateDTO, user: String): ObjectMetadata {
        checkLabelsExist(urlObjectCreateDto.metadata.knownLabels)
        val objectStorageInfo = objectService.saveUrlObject(urlObjectCreateDto.url)
        return createAndSaveObjectMetadata(objectStorageInfo, user, urlObjectCreateDto.metadata)
    }

    fun addFileObject(
        content: InputStream,
        originalFilename: String,
        fileObjectCreateDTO: FileObjectCreateDTO,
        user: String
    ): ObjectMetadata {
        checkLabelsExist(fileObjectCreateDTO.metadata.knownLabels)
        val objectStorageInfo = objectService.saveFileObject(content, originalFilename)
        return createAndSaveObjectMetadata(objectStorageInfo, user, fileObjectCreateDTO.metadata)
    }

    private fun createAndSaveObjectMetadata(
        storage: ObjectStorageInfo,
        user: String,
        metadataDTO: ObjectMetadataCreateDTO
    ): ObjectMetadata {
        val metadata = ObjectMetadata(
            storage.id,
            user,
            Utils.getFileExtension(storage.originalName),
            metadataDTO.tags,
            mapKnownLabelsToLabelings(metadataDTO.knownLabels)
        )
        return objectMetadataRepo.insert(metadata)
    }

    private fun mapKnownLabelsToLabelings(allLabels: Map<String, Set<String>>): Map<String, Labeling> {
        return allLabels.mapValues { (_, labels) -> Labeling(labels) }
    }

    fun updateMetadata(metadata: ObjectMetadata): ObjectMetadata {
        return objectMetadataRepo.save(metadata)
    }

    fun addUrlImageWithOD(urlImageCreateDTO: UrlImageCreateDTO, user: String): List<ObjectMetadata> {
        val (doObjectDetection, addAnnotations) = getOperationsToDoAndCheckTheirParameters(urlImageCreateDTO.objectDetection)
        val parentMetadata = addUrlObject(UrlObjectCreateDTO.fromUrlImageCreateDTO(urlImageCreateDTO), user)
        val allAddedObjects = mutableListOf(parentMetadata)
        if (doObjectDetection) {
            val detectedObjects = doODOrPrepareForODTask(
                objectService.getImageById(parentMetadata.id),
                urlImageCreateDTO.objectDetection.objectDetectionParameters!!,
                parentMetadata
            )
            allAddedObjects.addAll(detectedObjects)
        }
        if (addAnnotations) {
            addAnnotations(parentMetadata, urlImageCreateDTO.objectDetection.annotations!!)
        }
        objectMetadataRepo.save(parentMetadata)
        return allAddedObjects
    }

    fun addFileImageWithOD(
        content: InputStream,
        originalFilename: String,
        fileImageCreateDTO: FileImageCreateDTO,
        user: String
    ): List<ObjectMetadata> {
        val (doObjectDetection, addAnnotations) = getOperationsToDoAndCheckTheirParameters(fileImageCreateDTO.objectDetection)

        val contentBytes = getBytesFromInputStream(content)

        val parentMetadata = addFileObject(
            ByteArrayInputStream(contentBytes),
            originalFilename,
            FileObjectCreateDTO.fromFileImageCreateDTO(fileImageCreateDTO),
            user
        )
        val allAddedObjects = mutableListOf(parentMetadata)
        if (doObjectDetection) {
            val detectedObjects = doODOrPrepareForODTask(
                ImageUtils.getImageFromInputStream(ByteArrayInputStream(contentBytes)),
                fileImageCreateDTO.objectDetection.objectDetectionParameters!!,
                parentMetadata
            )
            allAddedObjects.addAll(detectedObjects)
        }
        if (addAnnotations) {
            addAnnotations(parentMetadata, fileImageCreateDTO.objectDetection.annotations!!)
        }
        objectMetadataRepo.save(parentMetadata)
        return allAddedObjects
    }

    private fun getOperationsToDoAndCheckTheirParameters(objectDetectionDTO: ObjectDetectionDTO): Pair<Boolean, Boolean> {
        var doObjectDetection = false
        var addAnnotations = false
        val odParams = objectDetectionDTO.objectDetectionParameters
        if (odParams != null && odParams.wantedLabels.isNotEmpty()) {
            checkODParameters(objectDetectionDTO.objectDetectionParameters)
            doObjectDetection = true
        }
        val annotations = objectDetectionDTO.annotations
        if (annotations != null && annotations.isNotEmpty()) {
            checkAnnotations(objectDetectionDTO.annotations)
            addAnnotations = true
        }
        return Pair(doObjectDetection, addAnnotations)
    }

    private fun checkODParameters(params: ObjectDetectionParametersDTO) {
        checkThresholds(params.thresholdOneVote, params.thresholdTwoVotes)
        checkLabelsExist(params.wantedLabels)
    }

    private fun checkThresholds(thresholdOneVote: Double, thresholdTwoVotes: Double) {
        if (thresholdOneVote < 0.0 || thresholdOneVote > 1.0) {
            throw IllegalArgumentException("thresholdOneVote must be between 0 and 1. Currently set: $thresholdOneVote")
        }
        if (thresholdTwoVotes < 0.0 || thresholdTwoVotes > 1.0) {
            throw IllegalArgumentException("thresholdTwoVotes must be between 0 and 1. Currently set: $thresholdTwoVotes")
        }
        if (thresholdTwoVotes < thresholdOneVote) {
            throw IllegalArgumentException("thresholdTwoVotes cannot be smaller than thresholdOneVote")
        }
    }

    private fun checkAnnotations(annotations: List<AnnotationDTO>) {
        annotations.forEach { annotation -> checkLabelsExist(mapOf(annotation.labelGroup to setOf(annotation.label))) }
    }

    private fun checkLabelsExist(labels: Map<String, Set<String>>) {
        for ((labelGroupName, labelList) in labels) {
            val labelGroup =
                getLabelGroup(labelGroupName) ?: throw IllegalArgumentException("Label group $labelGroupName not found")
            for (label in labelList) {
                if (!labelGroup.rangeContainsLabel(label)) {
                    throw IllegalArgumentException("Label $label not found in label group $labelGroupName")
                }
            }
        }
    }

    private fun doODOrPrepareForODTask(
        image: BufferedImage,
        params: ObjectDetectionParametersDTO,
        parent: ObjectMetadata
    ): List<ObjectMetadata> {
        val detectedObjects = mutableListOf<ObjectMetadata>()
        for ((labelGroup, labels) in params.wantedLabels) {
            if (labelGroup == ObjectDetectionConstants.LABEL_GROUP) {
                detectedObjects.addAll(detectAndSaveObjects(image, parent, params))
            } else {
                addDataForODTask(parent, labelGroup, labels)
            }
        }
        return detectedObjects
    }

    private fun detectAndSaveObjects(
        image: BufferedImage,
        parent: ObjectMetadata,
        params: ObjectDetectionParametersDTO
    ): List<ObjectMetadata> {
        val detectedObjects = objectDetectionService.detectObjectsWithOverlaps(
            image,
            params.wantedLabels[ObjectDetectionConstants.LABEL_GROUP]!!
        )
        val detectedMetadata = mutableListOf<ObjectMetadata>()
        val childrenImages = mutableListOf<ChildImage>()
        for ((i, obj) in detectedObjects.withIndex()) {
            val metadata = addDetectedObject(image, obj, i, parent, params)
            detectedMetadata.add(metadata)
            childrenImages.add(ChildImage(metadata.id, obj.relativeBoundingBox))
        }
        addChildrenImagesToParent(parent, childrenImages)
        return detectedMetadata
    }

    private fun addChildrenImagesToParent(parent: ObjectMetadata, children: MutableList<ChildImage>) {
        if (children.isNotEmpty()) {
            parent.otherMetadata[ChildrenImages.OTHER_METADATA_NAME] = ChildrenImages(children)
        }
    }

    private fun addDetectedObject(
        image: BufferedImage,
        obj: DetectedObjectWithOverlappingLabels,
        index: Int,
        parent: ObjectMetadata,
        params: ObjectDetectionParametersDTO
    ): ObjectMetadata {
        val storage = saveDetectedObject(image, obj.relativeBoundingBox, index, parent)
        val labeling = calculateLabelingForDetectedObject(obj, params.thresholdOneVote, params.thresholdTwoVotes)
        val metadata = ObjectMetadata(
            storage.id,
            parent.owner,
            parent.objectType.format,
            parent.tags,
            mutableMapOf(ObjectDetectionConstants.LABEL_GROUP to labeling),
            mutableMapOf(ParentImage.OTHER_METADATA_NAME to ParentImage(parent.id))
        )
        return objectMetadataRepo.insert(metadata)
    }

    private fun saveDetectedObject(
        image: BufferedImage,
        box: RelativeBoundingBox,
        index: Int,
        parent: ObjectMetadata
    ): ObjectStorageInfo {
        val originalFilename = "${parent.id}-detected$index.${parent.objectType.format}"
        val content = ImageUtils.cropImageToInputStream(image, box, parent.objectType.format)
        return objectService.saveFileObject(content, originalFilename)
    }

    private fun calculateLabelingForDetectedObject(
        obj: DetectedObjectWithOverlappingLabels,
        oneVote: Double,
        twoVotes: Double
    ): Labeling {
        val labelStatistics = LabelStatistics()
        for ((label, probability) in obj.labelsWithProbability.entries) {
            val votes = calculateVotesFromProbability(probability, twoVotes, oneVote)
            if (votes > 0) {
                labelStatistics.statistics[label] = LabelStatistic(votes, abs(votes))
            }
        }
        return Labeling(false, emptySet(), emptySet(), labelStatistics)
    }

    private fun calculateVotesFromProbability(probability: Double, thresholdTwoVotes: Double, thresholdOneVote: Double): Int {
        return if (probability > thresholdTwoVotes) {
            2
        } else if (probability > thresholdOneVote) {
            1
        } else {
            0
        }
    }

    private fun addDataForODTask(
        metadata: ObjectMetadata,
        labelGroupName: String,
        labels: Set<String>
    ) {
        val objectsDetectingData = getOrCreateObjectsDetectingData(metadata.otherMetadata)
        objectsDetectingData.objects[labelGroupName] =
            mutableMapOf(*labels.map { it to ObjectDetectionData() }
                .toTypedArray())
    }

    private fun getOrCreateObjectsDetectingData(otherMetadata: MutableMap<String, OtherMetadataType>): ObjectsDetectionData {
        otherMetadata.putIfAbsent(
            ObjectsDetectionData.OTHER_METADATA_NAME,
            ObjectsDetectionData()
        )
        val objectsDetectingData = otherMetadata[ObjectsDetectionData.OTHER_METADATA_NAME]
        if (objectsDetectingData !is ObjectsDetectionData) {
            throw IllegalStateException("Other metadata with name ${ObjectsDetectionData.OTHER_METADATA_NAME} is not in format for object detection.")
        }
        return objectsDetectingData
    }

    private fun addAnnotations(metadata: ObjectMetadata, annotations: List<AnnotationDTO>) {
        val objectsDetectionData = getOrCreateObjectsDetectingData(metadata.otherMetadata)
        for (annotation in annotations) {
            objectsDetectionData.getOrCreateODData(annotation.labelGroup, annotation.label).result.add(annotation.boundingBox)
        }
    }
}
