package com.example.captcha.task.templates.imagelabelingtemplate

import com.example.captcha.datamanagement.objectmetadata.*
import com.example.captcha.datamanagement.objectstorage.ObjectService
import com.example.captcha.verification.*
import com.example.captcha.verification.entities.*
import com.example.captcha.task.templates.TaskTemplate
import com.example.captcha.task.templates.TemplateUtils.toBase64Image
import com.example.captcha.task.templates.TemplateUtils.toBase64String
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service("IMAGE_LABELING")
class ImageLabelingTemplate(val objectMetadataService: ObjectMetadataService,
                            val objectMapper: ObjectMapper,
                            val objectService: ObjectService
): TaskTemplate {
    override fun generateTask(generationConfig: JsonNode, userName: String): Triple<Description, TaskData, AnswerSheet> {
        val config = objectMapper.treeToValue(generationConfig, ImageLabelingGenerationConfig::class.java)
        val filteredImages = config.owner?.let { objectMetadataService.getFiltered(userName, config.tags, it, ObjectTypeEnum.IMAGE) } ?: objectMetadataService.getFiltered(userName, config.tags)
        val labelGroupName = config.labelGroup
        val labelGroup = objectMetadataService.getLimitedLabelGroup(labelGroupName) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "LabelGroup not found: $labelGroupName")
        val chosenLabel = labelGroup.labelRange.random()

        val chosenImages = selectImages(filteredImages, labelGroupName, chosenLabel)

        val expectedResults = chosenImages.map { Pair(it.first.objectId, it.second) }
        val displayableImages = toDisplayImages(chosenImages)

        val description = descriptionTemplate(chosenLabel.label)
        val taskData = ObjectsWithLabels(chosenLabel, labelGroupName, expectedResults)
        val answerSheet = AnswerSheet(ListDisplayData(displayableImages), AnswerType.MultipleText)

        return Triple(description, taskData, answerSheet)
    }

    override fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult {
        val imageLabelingAnswer = answer as TextListAnswer
        val data = taskData as ObjectsWithLabels

        val verificationResult = evaluateAnswer(imageLabelingAnswer, data.expectedResults)

        if (verificationResult > 0.95) {
            labelUnknown(data.expectedResults, data.label, data.labelGroup, imageLabelingAnswer)
        }

        return EvaluationResult(verificationResult)
    }

    private fun selectImages(objects: List<ObjectMetadata>, labelGroupName: String, label: Label): List<Pair<ObjectMetadata, ExpectedResult>> {
        val withLabel = objectMetadataService.getRandomWithLabel(objects, labelGroupName, label, 2)
        val withoutLabel = objectMetadataService.getRandomWithoutLabel(objects, labelGroupName, label, 2)
        val unknownLabel = objectMetadataService.getRandomNotKnowingLabel(objects, labelGroupName, label, 2)

        val chosenImages = ArrayList<Pair<ObjectMetadata, ExpectedResult>>(6)
        chosenImages.addAll(withLabel.map{ Pair(it, ExpectedResult.CORRECT)})
        chosenImages.addAll(withoutLabel.map{ Pair(it, ExpectedResult.INCORRECT)})
        chosenImages.addAll(unknownLabel.map{ Pair(it, ExpectedResult.UNKNOWN)})
        // todo create mapping
        chosenImages.shuffle()

        return chosenImages
    }

    fun toDisplayImages(images: List<Pair<ObjectMetadata, ExpectedResult>>): List<ImageDisplayData> {
        return images.map { (metadata, _) ->
            val objectId = metadata.objectId
            val format = (metadata.objectType as ImageObjectType).format

            val bytes = objectService.getById(objectId)?.readAllBytes()
            val base64ImageString =  bytes?.let { toBase64Image(toBase64String(it), format)}!! // todo handle possible error

            ImageDisplayData(base64ImageString)
        }
    }

    fun evaluateAnswer(answer: TextListAnswer, expectedResults: List<Pair<Long, ExpectedResult>>): Float {
        val verifyingResultsSize = expectedResults.filter { it.second != ExpectedResult.UNKNOWN}.size
        val correctAnswers = expectedResults.withIndex().count { (index, value) ->
            val (_, expectedResult) = value

            val userAnswer = answer.texts.contains(index.toString())

            evaluateUserAnswer(userAnswer, expectedResult)
        }

        return correctAnswers.toFloat() / verifyingResultsSize
    }

    fun evaluateUserAnswer(userAnswer: Boolean, expectedAnswer: ExpectedResult): Boolean {
        return when(expectedAnswer) {
            ExpectedResult.CORRECT -> userAnswer
            ExpectedResult.INCORRECT -> !userAnswer
            ExpectedResult.UNKNOWN -> false
        }
    }

    fun labelUnknown(expectedResults: List<Pair<Long, ExpectedResult>>, label: Label, labelGroupName: String, answer: TextListAnswer) {
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

data class ImageLabelingGenerationConfig(val labelGroup: String, val tags: List<String>, val owner: String?)
