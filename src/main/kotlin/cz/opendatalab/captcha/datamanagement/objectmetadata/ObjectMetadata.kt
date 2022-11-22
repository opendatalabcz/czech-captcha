package cz.opendatalab.captcha.datamanagement.objectmetadata

import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Object metadata are data about objects used for business purposes, used by task templates
 */
// labels ... <labelGroupName, Labeling>
@Document("objectmetadata")
data class ObjectMetadata(@Id val id: String,
                          val owner: String,
                          val objectType: ObjectType,
                          val tags: MutableSet<String>,
                          val labels: MutableMap<String,Labeling>,
                          val otherMetadata: MutableMap<String, OtherMetadataType>
){
    constructor(id: String, owner: String, format: String):
            this(id, owner, format, mutableSetOf())
    constructor(id: String, owner: String, format: String, tags: Set<String>):
            this(id, owner, format, tags, mutableMapOf())
    constructor(id: String, owner: String, format: String, label: Pair<String, Labeling>):
            this(id, owner, format, mutableMapOf(label))
    constructor(id: String, owner: String, format: String, labels: Map<String, Labeling>):
            this(id, owner, format, mutableSetOf(), labels)
    constructor(id: String, owner: String, format: String, tags: Set<String>, labels: Map<String, Labeling>):
            this(id, owner, format, tags, labels, mutableMapOf())
    constructor(id: String, owner: String, format: String, tags: Set<String>, labels: Map<String, Labeling>, otherMetadata: Map<String, OtherMetadataType>):
            this(id, owner, ObjectType.fromFormat(format), tags.toMutableSet(), labels.toMutableMap(), otherMetadata.toMutableMap())

    fun label(label: String, labelGroupName: String, positive: Boolean, maxCardinality: Int, labelRangeSize: Int) {
        val labeling = labels.putIfAbsent(labelGroupName, Labeling()) ?: labels[labelGroupName]!!
        labels[labelGroupName] = labeling.recordLabel(positive, label, maxCardinality, labelRangeSize)
    }

    fun containsLabel(labelGroup: String, label: String): Boolean {
        return labels[labelGroup]?.labels?.contains(label) ?: false
    }

    fun containsNegativeLabel(labelGroup: String, label: String): Boolean {
        return labels[labelGroup]?.let { (it.isLabeled && !it.labels.contains(label)) ||
                (!it.isLabeled && it.negativeLabels.contains(label)) } ?: false
    }

    fun containsUnresolvedLabel(labelGroup: String, label: String): Boolean {
        return labels[labelGroup]?.let { !it.isLabeled && !it.labels.contains(label) && !it.negativeLabels.contains(label) } ?: true
    }
}

enum class ObjectTypeEnum {
    IMAGE, SOUND, TEXT_FILE
}

interface ObjectType {
    val type: ObjectTypeEnum
    val format: String

    companion object {
        fun fromFormat(format: String): ObjectType {
            return when(format) {
                "jpg", "jpeg", "png", "gif", "svg", "apng", "bmp", "pjpeg", "svg+xml", "tiff", "webp", "x-icon" -> ImageObjectType(format)
                "mp3", "aac", "wav", "mp4", "wma", "flac", "m4a" -> SoundObjectType(format)
                else -> TextObjectType(format)
            }
        }
    }
}

data class ImageObjectType(override val format: String): ObjectType {
    override val type = ObjectTypeEnum.IMAGE
}

data class SoundObjectType(override val format: String): ObjectType {
    override val type = ObjectTypeEnum.SOUND
}

data class TextObjectType(override val format: String): ObjectType {
    override val type = ObjectTypeEnum.TEXT_FILE
}

interface OtherMetadataType

data class ParentImage(val id: String): OtherMetadataType {
    companion object {
        const val OTHER_METADATA_NAME = "parent-image"
    }
}

data class ChildrenImages(val images: List<ChildImage>): OtherMetadataType {
    companion object {
        const val OTHER_METADATA_NAME = "children-images"
    }
}

data class ChildImage(val id: String, val crop: RelativeBoundingBox)