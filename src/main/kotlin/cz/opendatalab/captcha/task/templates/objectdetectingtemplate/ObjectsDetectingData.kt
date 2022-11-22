package cz.opendatalab.captcha.task.templates.objectdetectingtemplate

import cz.opendatalab.captcha.datamanagement.objectmetadata.OtherMetadataType
import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox

// label group -> label -> ObjectLocalizationData
data class ObjectsDetectingData(val objects: MutableMap<String, MutableMap<String, ObjectDetectingData>>): OtherMetadataType {
    constructor(): this(mutableMapOf())
}

data class ObjectDetectingData(var isLocalized: Boolean, val result: MutableList<RelativeBoundingBox>, val answers: MutableList<List<RelativeBoundingBox>>) {
    constructor(result: MutableList<RelativeBoundingBox>): this(true, result, mutableListOf())
    constructor(): this(false, mutableListOf(), mutableListOf())
}

object ObjectDetectingConstants {
    const val TEMPLATE_DATA_NAME = "object-detecting"
}