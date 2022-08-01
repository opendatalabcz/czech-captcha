package cz.opendatalab.captcha.datamanagement.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import cz.opendatalab.captcha.datamanagement.objectmetadata.*
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo

data class UrlObjectCreateDTO(val url: String, val fileType: FileTypeDTO, val metadata: ObjectMetadataCreateDTO)

data class FileObjectCreateDTO(val fileType: FileTypeDTO, val metadata: ObjectMetadataCreateDTO)

data class UrlImageCreateDTO(val url: String, val fileType: ImageFileTypeDTO, val metadata: ObjectMetadataCreateDTO, val objectDetection: ObjectDetectionParametersDTO)

data class FileImageCreateDTO(val fileType: ImageFileTypeDTO, val metadata: ObjectMetadataCreateDTO, val objectDetection: ObjectDetectionParametersDTO)

data class ObjectDTO(val storageInfo: ObjectStorageInfo, val metadata: ObjectMetadata)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class FileTypeDTO {
    abstract fun toDomain(): ObjectType
}

data class ImageFileTypeDTO(val format: String): FileTypeDTO() {
    override fun toDomain(): ObjectType {
        return ImageObjectType(format)
    }
}

data class SoundFileTypeDTO(val format: String): FileTypeDTO() {
    override fun toDomain(): ObjectType {
        return SoundObjectType(format)
    }
}

object TextFileTypeDTO: FileTypeDTO() {
    override fun toDomain(): ObjectType {
        return TextObjectType
    }
}

data class ObjectMetadataCreateDTO(val labels: Map<String, List<String>>, val tags: List<String>)

data class ObjectDetectionParametersDTO(val wantedLabels: Map<String, List<String>>,
                                        val thresholdOneVote: Double,
                                        val thresholdTwoVotes: Double)
