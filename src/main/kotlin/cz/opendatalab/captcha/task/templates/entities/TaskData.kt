package cz.opendatalab.captcha.verification.entities

import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox

interface TaskData {
}

data class TextData(val text: String): TaskData

// file id,
data class ObjectsWithLabels(
    val label: String,
    val labelGroup: String,
    val expectedResults: List<Pair<String, ExpectedResult>>
): TaskData

data class ImagesWithBoundingBoxes(
    val labelGroup: String,
    val label: String,
    val unknownImageId: String,
    val knownImageSize: ImageSize,
    val expectedResult: List<RelativeBoundingBox>,
    val isKnownImageFirst: Boolean
): TaskData

data class ImageSize(val width: Int, val height: Int)

enum class ExpectedResult {
    CORRECT, INCORRECT, UNKNOWN
}
