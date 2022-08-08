package cz.opendatalab.captcha.task.templates.objectdetectingtemplate

import cz.opendatalab.captcha.datamanagement.objectmetadata.OtherMetadataType
import cz.opendatalab.captcha.objectdetection.BoundingBox

// label group -> label -> ObjectLocalizationData
data class ObjectDetectingData(val objects: MutableMap<String, MutableMap<String, ObjectLocalizationData>>): OtherMetadataType

data class ObjectLocalizationData(val isLocalized: Boolean, val result: List<BoundingBox>, val answers: MutableList<List<BoundingBox>>)

object ObjectDetectingConstants {
    const val TEMPLATE_DATA_NAME = "object-detecting"
}