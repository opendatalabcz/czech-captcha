package cz.opendatalab.captcha.task.templates.objectdetectingtemplate

import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.datamanagement.objectdetection.AbsoluteBoundingBox
import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox
import cz.opendatalab.captcha.datamanagement.objectmetadata.*
import cz.opendatalab.captcha.task.templates.GenerationConfig
import cz.opendatalab.captcha.task.templates.ObjectDetectingGenerationConfig
import cz.opendatalab.captcha.task.templates.TaskTemplate
import cz.opendatalab.captcha.verification.entities.*
import org.springframework.stereotype.Service
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


@Service("OBJECT_DETECTING")
class ObjectDetectingTemplate(
    val objectMetadataService: ObjectMetadataService,
    val objectService: ObjectService
) : TaskTemplate {
    override fun generateTask(
        generationConfig: GenerationConfig,
        currentUser: String
    ): Triple<Description, TaskData, AnswerSheet> {
        val odTaskData = getImagesWithSameKnownAndUnknownLabel(currentUser, generationConfig as ObjectDetectingGenerationConfig)

        val labelGroup = odTaskData.labelGroup
        val label = odTaskData.label
        val knownImage = odTaskData.knownImage
        val unknownImage = odTaskData.unknownImage

        val expectedResult = knownImage.getOrCreateObjectsDetectionData().getOrCreateODData(labelGroup, label).result
        val isKnownImageFirst = Random.nextBoolean()

        val knownImageSize = getImageSize(knownImage.id)

        val description = Description("Mark all instances of $label with a rectangle.")
        val taskData = ImagesWithBoundingBoxes(labelGroup, label, unknownImage.id, knownImageSize, expectedResult, isKnownImageFirst)
        val answerSheet = AnswerSheet(
            ListDisplayData(listOf(
                ImageDisplayData(objectService.getImageBase64StringById(knownImage.id)),
                ImageDisplayData(objectService.getImageBase64StringById(unknownImage.id)))),
            AnswerType.MultipleBoundingBox
        )

        return Triple(description, taskData, answerSheet)
    }

    private fun getImagesWithSameKnownAndUnknownLabel(user: String, config: ObjectDetectingGenerationConfig): ODTaskData {
        val images =
            objectMetadataService.getFiltered(user, config.tags, config.owners, ObjectTypeEnum.IMAGE)
                .filter { it.containsObjectsDetectionData() }

        val imagesWithKnownLabels = mutableMapOf<String, MutableMap<String, ObjectMetadata>>()
        val imagesWithUnknownLabels = mutableMapOf<String, MutableMap<String, ObjectMetadata>>()

        for (metadata in images.shuffled()) {
            val objectsDetectionData = metadata.getObjectsDetectionData()!!
            for ((labelGroup, labels) in objectsDetectionData.objects.entries.shuffled()) {
                for ((label, odData) in labels.entries.shuffled()) {
                    if (odData.isDetected()) {
                        val unknownImage = imagesWithUnknownLabels[labelGroup]?.get(label)
                        if (unknownImage != null) {
                            return ODTaskData(labelGroup, label, metadata, unknownImage)
                        }
                        imagesWithKnownLabels.getOrPut(labelGroup) { mutableMapOf() }.putIfAbsent(label, metadata)
                    } else {
                        val knownImage = imagesWithKnownLabels[labelGroup]?.get(label)
                        if (knownImage != null) {
                            return ODTaskData(labelGroup, label, knownImage, metadata)
                        }
                        imagesWithUnknownLabels.getOrPut(labelGroup) { mutableMapOf() }.putIfAbsent(label, metadata)
                    }
                }
            }
        }
        throw IllegalArgumentException("Images does not contain any label both detected and undetected.")
    }

    override fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult {
        val objectDetectingAnswer = answer as BoundingBoxesAnswer
        val data = taskData as ImagesWithBoundingBoxes

        verifyBoundingBoxesInImage(answer)

        val verificationResult = evaluateAnswer(objectDetectingAnswer.known, data.expectedResult, data.knownImageSize)

        if (verificationResult > ADD_DETECTION_DATA_THRESHOLD) {
            addDetectionDataToUnknownImage(
                data.unknownImageId,
                data.label,
                data.labelGroup,
                objectDetectingAnswer.unknown
            )
        }

        return EvaluationResult(verificationResult)
    }

    private fun verifyBoundingBoxesInImage(answer: BoundingBoxesAnswer) {
        for (box in answer.known) {
            verifyBoundingBoxInImage(box)
        }
        for (box in answer.unknown) {
            verifyBoundingBoxInImage(box)
        }
    }

    private fun verifyBoundingBoxInImage(box: RelativeBoundingBox) {
        if (!box.isInImage()) {
            throw IllegalArgumentException(
                "Bounding box " +
                        "(x: ${box.x}, y: ${box.y}, w: ${box.width}, h: ${box.height})" +
                        " is not completely in the image."
            )
        }
    }

    private fun evaluateAnswer(
        answerRel: List<RelativeBoundingBox>,
        expectedRel: List<RelativeBoundingBox>,
        imageSize: ImageSize
    ): Double {
        val answer = answerRel.map { it.toAbsoluteBoundingBox(imageSize) }
        val expected = expectedRel.map { it.toAbsoluteBoundingBox(imageSize) }
        val clusters = createClusters(listOf(answer, expected))

        // calculate accumulated IoU between pairs
        var accumulatedIntersection = 0
        var accumulatedUnion = 0
        for (cluster in clusters) {
            if (cluster.size == 2) { // matched in pair
                accumulatedIntersection += calculateIntersection(cluster[0], cluster[1])
                accumulatedUnion += calculateUnion(cluster[0], cluster[1])
            } else { // not matched with any other box
                accumulatedUnion += cluster[0].width * cluster[0].height
            }
        }
        return accumulatedIntersection.toDouble() / accumulatedUnion.toDouble()
    }

    /**
     * Agglomerative hierarchical clustering based on Intersection over Union (IoU) as a distance metric.
     * Boxes from the same answer cannot be part of the same cluster.
     */
    private fun createClusters(allBoxes: List<List<AbsoluteBoundingBox>>): List<List<AbsoluteBoundingBox>> {
        val distances = calculateDistances(allBoxes)

        // offsets of boxes in individual answers
        val offsets = mutableListOf(0)

        // table showing which answers are already part of a cluster (cluster cannot contain boxes from the same answer)
        // row -> cluster number, column -> answer number, value -> order of the box in the answer
        val clusters = mutableListOf<MutableList<Int>>()

        // initialize offsets and clusters (each box is a cluster at the beginning)
        for ((y, answer) in allBoxes.withIndex()) {
            offsets.add(offsets[y] + answer.size)
            for (x in answer.indices) {
                val cluster = MutableList(allBoxes.size) { NO_BOX }
                cluster[y] = x
                clusters.add(cluster)
            }
        }

        for (distance in distances) {
            val box1Id = distance.b1.first + offsets[distance.b1.second]
            val box2Id = distance.b2.first + offsets[distance.b2.second]
            if (canClustersBeMerged(clusters[box1Id], clusters[box2Id])) {
                mergeClusters(clusters, offsets, box1Id, box2Id)
                clusters[box2Id] = clusters[box1Id]
            }
        }

        return getFinalClusters(clusters, allBoxes)
    }

    private fun canClustersBeMerged(cluster1: List<Int>, cluster2: List<Int>): Boolean {
        for (x in cluster1.indices) {
            if (cluster1[x] != NO_BOX && cluster2[x] != NO_BOX) {
                return false
            }
        }
        return true
    }

    private fun mergeClusters(clusters: MutableList<MutableList<Int>>, offsets: List<Int>, box1Id: Int, box2Id: Int) {
        val cluster1 = clusters[box1Id]
        val cluster2 = clusters[box2Id]
        for (x in cluster1.indices) {
            if (cluster1[x] == NO_BOX && cluster2[x] != NO_BOX) {
                cluster1[x] = cluster2[x]
                clusters[offsets[x] + cluster1[x]] = cluster1
            }
        }
    }

    private fun getFinalClusters(
        clusters: MutableList<MutableList<Int>>,
        allBoxes: List<List<AbsoluteBoundingBox>>
    ): List<List<AbsoluteBoundingBox>> {
        val finalClusters = mutableListOf<MutableList<AbsoluteBoundingBox>>()
        for (cluster in clusters) {
            if (cluster.isNotEmpty()) {
                val clusterToAdd = mutableListOf<AbsoluteBoundingBox>()
                for ((y, x) in cluster.withIndex()) {
                    if (x >= 0) {
                        clusterToAdd.add(allBoxes[y][x])
                    }
                }
                finalClusters.add(clusterToAdd)
                cluster.clear()
            }
        }

        return finalClusters
    }

    private fun addDetectionDataToUnknownImage(
        imageId: String,
        label: String,
        labelGroup: String,
        boxes: List<RelativeBoundingBox>
    ) {
        val imageMetadata = objectMetadataService.getById(imageId)
            ?: throw IllegalStateException("Metadata for file with id $imageId does not exist.")
        val objectsDetectionData = (imageMetadata.otherMetadata[ObjectsDetectionData.OTHER_METADATA_NAME]
            ?: throw IllegalStateException("Image with id ${imageMetadata.id} does not contain data for object detecting task"))
                as ObjectsDetectionData
        val objectDetectingData = objectsDetectionData.objects[labelGroup]?.get(label) ?: throw IllegalStateException(
            "Image with id ${imageMetadata.id} does not contain data for object detecting task for " +
                    "$label label from $labelGroup labelgroup."
        )
        if (objectDetectingData.isDetected()) {
            if (objectDetectingData.result.isNotEmpty()) {
                return
            }
        }
        objectDetectingData.answers.add(boxes)
        // if number of answers is lower than ANSWERS_NEEDED_FOR_EVALUATION, only record answer
        if (objectDetectingData.answers.size < ANSWERS_NEEDED_FOR_EVALUATION) {
            objectMetadataService.updateMetadata(imageMetadata)
            return
        }
        val imageSize = getImageSize(imageId)
        val absoluteAnswers =
            objectDetectingData.answers.map { answer -> answer.map { box -> box.toAbsoluteBoundingBox(imageSize) } }
        val clusters = createClusters(absoluteAnswers)
        for (cluster in clusters) {
            // if cluster has more than ANSWERS_NEEDED_FOR_EVALUATION/2 bounding boxes, make average of them and record it as a result
            if (cluster.size > ANSWERS_NEEDED_FOR_EVALUATION / 2) {
                objectDetectingData.result.add(getAverageBoundingBox(cluster).toRelativeBoundingBox(imageSize))
            }
        }
        objectMetadataService.updateMetadata(imageMetadata)
    }

    private fun getImageSize(imageId: String): ImageSize {
        val image = objectService.getImageById(imageId)
        return ImageSize(image.width, image.height)
    }

    private fun calculateDistances(allBoxes: List<List<AbsoluteBoundingBox>>): List<Distance> {
        val distances = mutableListOf<Distance>()
        // iterate over rows (answers)
        for (y1 in allBoxes.indices) {
            for (y2 in y1 + 1 until allBoxes.size) {
                // iterate over individual boxes in two different rows (answers)
                for ((x1, box1) in allBoxes[y1].withIndex()) {
                    for ((x2, box2) in allBoxes[y2].withIndex()) {
                        val iou = calculateIoU(box1, box2)
                        if (iou > SIMILARITY_THRESHOLD) { // do not include pairs that are not very similar
                            distances.add(Distance(iou, Pair(x1, y1), Pair(x2, y2)))
                        }
                    }
                }
            }
        }
        distances.sortByDescending { it.iou }
        return distances
    }

    private fun calculateIoU(b1: AbsoluteBoundingBox, b2: AbsoluteBoundingBox): Double {
        return calculateIntersection(b1, b2).toDouble() / calculateUnion(b1, b2).toDouble()
    }

    private fun calculateIntersection(b1: AbsoluteBoundingBox, b2: AbsoluteBoundingBox): Int {
        if (b1.x < 0 || b1.y < 0 || b1.width < 0 || b1.height < 0 ||
            b2.x < 0 || b2.y < 0 || b2.width < 0 || b2.height < 0
        ) {
            throw IllegalArgumentException("Coordinates and dimensions of a bounding box cannot be negative.")
        }
        val xIntersection = max(0, min(b1.x + b1.width, b2.x + b2.width) - max(b1.x, b2.x))
        val yIntersection = max(0, min(b1.y + b1.height, b2.y + b2.height) - max(b1.y, b2.y))
        return xIntersection * yIntersection
    }

    private fun calculateUnion(b1: AbsoluteBoundingBox, b2: AbsoluteBoundingBox): Int {
        return b1.width * b1.height + b2.width * b2.height - calculateIntersection(b1, b2)
    }

    private fun getAverageBoundingBox(boxes: List<AbsoluteBoundingBox>): AbsoluteBoundingBox {
        var x = 0
        var y = 0
        var w = 0
        var h = 0
        for (box in boxes) {
            x += box.x
            y += box.y
            w += box.width
            h += box.height
        }
        return AbsoluteBoundingBox(x / boxes.size, y / boxes.size, w / boxes.size, h / boxes.size)
    }

    companion object {
        const val NO_BOX = -1
        const val ANSWERS_NEEDED_FOR_EVALUATION = 11
        const val ADD_DETECTION_DATA_THRESHOLD = 0.6
        const val SIMILARITY_THRESHOLD = 0.2
    }
}

data class ODTaskData(
    val labelGroup: String,
    val label: String,
    val knownImage: ObjectMetadata,
    val unknownImage: ObjectMetadata
)

data class Distance(val iou: Double, val b1: Pair<Int, Int>, val b2: Pair<Int, Int>)