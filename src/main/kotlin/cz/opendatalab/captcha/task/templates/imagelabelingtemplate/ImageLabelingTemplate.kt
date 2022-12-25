package cz.opendatalab.captcha.task.templates.imagelabelingtemplate

import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadata
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadataService
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectTypeEnum
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.task.templates.GenerationConfig
import cz.opendatalab.captcha.task.templates.ImageLabelingGenerationConfig
import cz.opendatalab.captcha.task.templates.TaskTemplate
import cz.opendatalab.captcha.verification.entities.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.random.Random

@Service("Image Labeling")
class ImageLabelingTemplate(
    val objectMetadataService: ObjectMetadataService,
    val objectService: ObjectService,
    val properties: ImageLabelingTemplateProperties
): TaskTemplate {
    override fun generateTask(generationConfig: GenerationConfig, currentUser: String): Triple<Description, TaskData, AnswerSheet> {
        val config = generationConfig as ImageLabelingGenerationConfig
        val filteredImages = objectMetadataService.getFiltered(currentUser, config.tags, config.owners, ObjectTypeEnum.IMAGE)
        val labelGroupName = config.labelGroup
        val labelGroup = objectMetadataService.getLimitedLabelGroup(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "LabelGroup not found: $labelGroupName")
        val chosenLabel = labelGroup.labelRange.random()

        val chosenImages = selectImages(filteredImages, labelGroupName, chosenLabel)

        val expectedResults = chosenImages.map { Pair(it.first.id, it.second) }
        val displayableImages = toDisplayImages(chosenImages)

        val description = descriptionTemplate(chosenLabel)
        val taskData = ObjectsWithLabels(chosenLabel, labelGroupName, expectedResults)
        val answerSheet = AnswerSheet(ListDisplayData(displayableImages), AnswerType.MultipleText)

        return Triple(description, taskData, answerSheet)
    }

    override fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult {
        val imageLabelingAnswer = answer as TextListAnswer
        val data = taskData as ObjectsWithLabels

        val verificationResult = evaluateAnswer(imageLabelingAnswer, data.expectedResults)

        if (verificationResult > properties.labelUnknownThreshold) {
            labelUnknown(data.expectedResults, data.label, data.labelGroup, imageLabelingAnswer)
        }

        return EvaluationResult(verificationResult)
    }

    private fun selectImages(objects: List<ObjectMetadata>, labelGroupName: String, label: String): List<Pair<ObjectMetadata, ExpectedResult>> {
        val totalImagesCount = properties.totalImagesCount
        val unknownLabelCount = properties.unknownLabelCount
        val minWithLabel = properties.minWithLabel
        val withLabelCount = Random.nextInt(minWithLabel, totalImagesCount - unknownLabelCount - minWithLabel + 1)
        val withoutLabelCount = totalImagesCount - unknownLabelCount - withLabelCount

        val withLabel = objectMetadataService.getRandomWithLabel(objects, labelGroupName, label, withLabelCount)
        val withoutLabel = objectMetadataService.getRandomWithoutLabel(objects, labelGroupName, label, withoutLabelCount)
        val unknownLabel = objectMetadataService.getRandomNotKnowingLabel(objects, labelGroupName, label, unknownLabelCount)

        val chosenImages = ArrayList<Pair<ObjectMetadata, ExpectedResult>>(totalImagesCount)
        chosenImages.addAll(withLabel.map { Pair(it, ExpectedResult.CORRECT) } )
        chosenImages.addAll(withoutLabel.map { Pair(it, ExpectedResult.INCORRECT) } )
        chosenImages.addAll(unknownLabel.map { Pair(it, ExpectedResult.UNKNOWN) } )

        chosenImages.shuffle()

        return chosenImages
    }

    fun toDisplayImages(images: List<Pair<ObjectMetadata, ExpectedResult>>): List<ImageDisplayData> {
        return images.map { (metadata, _) ->
            ImageDisplayData(objectService.getImageBase64StringById(metadata.id))
        }
    }

    fun evaluateAnswer(answer: TextListAnswer, expectedResults: List<Pair<String, ExpectedResult>>): Double {
        val verifyingResultsSize = expectedResults.filter { it.second != ExpectedResult.UNKNOWN}.size
        val correctAnswers = expectedResults.withIndex().count { (index, value) ->
            val (_, expectedResult) = value

            val userAnswer = answer.texts.contains(index.toString())

            evaluateUserAnswer(userAnswer, expectedResult)
        }

        return correctAnswers.toDouble() / verifyingResultsSize
    }

    fun evaluateUserAnswer(userAnswer: Boolean, expectedAnswer: ExpectedResult): Boolean {
        return when(expectedAnswer) {
            ExpectedResult.CORRECT -> userAnswer
            ExpectedResult.INCORRECT -> !userAnswer
            ExpectedResult.UNKNOWN -> false
        }
    }

    fun labelUnknown(expectedResults: List<Pair<String, ExpectedResult>>, label: String, labelGroupName: String, answer: TextListAnswer) {
        expectedResults.withIndex().forEach { (index, result) ->
                val (objectId, expectedResult) = result
                if (expectedResult == ExpectedResult.UNKNOWN) {
                    objectMetadataService.labelObject(objectId, labelGroupName, label, answer.texts.contains(index.toString()))
                }
            }
    }

    fun descriptionTemplate(label: String): Description {
        return Description("Select all images that could be labeled with $label")
    }
}

