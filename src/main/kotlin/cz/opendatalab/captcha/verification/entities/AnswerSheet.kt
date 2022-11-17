package cz.opendatalab.captcha.verification.entities

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox
import kotlin.reflect.KClass

data class AnswerSheet(val displayData: DisplayData, val answerType: AnswerType)

sealed class DisplayData {
    val type = className(this::class)
}

object EmptyDisplayData: DisplayData()

data class ImageDisplayData(val base64ImageString: String): DisplayData()

data class ListDisplayData(val listData: List<DisplayData>): DisplayData()

enum class AnswerType(@JsonValue val type: String) {
    Text(className(TextAnswer::class)),
    MultipleText(className(TextListAnswer::class)),
    MultipleBoundingBox(className(BoundingBoxesAnswer::class))
}

// Needed for abstract type deserialization
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
sealed class Answer {
    val type = className(this::class)
}
// somehow bind this to the answersheet?
data class TextAnswer(val text: String): Answer()

data class TextListAnswer(val texts: List<String>): Answer()

data class BoundingBoxesAnswer(val known: List<RelativeBoundingBox>, val unknown: List<RelativeBoundingBox>): Answer()

fun <T : Any>className(clazz: KClass<T>): String {
    return clazz.java.simpleName
}
