package com.example.captcha.datamanagement.objectmetadata

import com.example.captcha.verification.entities.TaskType
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Object metadata are data about objects used for business purposes, used by task templates
 */
// labels ... <labelGroupName, Lableling>
data class ObjectMetadata(val objectId: Long, val user: String, val objectType: ObjectType, val labels: MutableMap<String,Labeling>,
                          val taskData: Map<TaskType, OtherMetadataType>, val tags: List<String>) {
    constructor(objectId: Long, user: String, objectType: ObjectType): this(objectId, user, objectType, mutableMapOf())
    constructor(objectId: Long, user: String, objectType: ObjectType, labelGroup: Pair<String, Labeling>): this(objectId, user, objectType, mutableMapOf(labelGroup))
    constructor(objectId: Long, user: String, objectType: ObjectType, labelGroups: MutableMap<String, Labeling>): this(objectId, user, objectType, labelGroups, emptyList())
    constructor(objectId: Long, user: String, objectType: ObjectType, labelGroups: MutableMap<String, Labeling>, tags: List<String>): this(objectId, user, objectType, labelGroups, emptyMap(), tags)

    fun label(label: Label, labelGroupName: String, positive: Boolean, maxCardinality: Int, labelRangeSize: Int) {
        val labeling = labels.putIfAbsent(labelGroupName, Labeling()) ?: labels[labelGroupName]!!
        labels[labelGroupName] = labeling.recordLabel(positive, label, maxCardinality, labelRangeSize)
    }

    fun containsLabel(labelGroup: String, label: Label): Boolean {
        return labels[labelGroup]?.labels?.contains(label) ?: false
    }

    fun containsNegativeLabel(labelGroup: String, label: Label): Boolean {
        return labels[labelGroup]?.let { (it.isLabeled && !it.labels.contains(label)) ||
                (!it.isLabeled && it.negativeLabels.contains(label)) } ?: false
    }

    fun containsUnresolvedLabel(labelGroup: String, label: Label): Boolean {
        return labels[labelGroup]?.let { !it.isLabeled && !it.labels.contains(label) && !it.negativeLabels.contains(label) } ?: true
    }
}

enum class ObjectTypeEnum {
    IMAGE, SOUND, TEXT_FILE
}

interface ObjectType {
    @JsonProperty
    fun type(): ObjectTypeEnum
}

data class ImageObjectType(val format: String): ObjectType {
    override fun type(): ObjectTypeEnum {
        return ObjectTypeEnum.IMAGE
    }
}

data class SoundObjectType(val format: String): ObjectType {
    override fun type(): ObjectTypeEnum {
        return ObjectTypeEnum.SOUND
    }
}

object TextObjectType: ObjectType {
    override fun type(): ObjectTypeEnum {
        return ObjectTypeEnum.TEXT_FILE
    }
}

interface OtherMetadataType
