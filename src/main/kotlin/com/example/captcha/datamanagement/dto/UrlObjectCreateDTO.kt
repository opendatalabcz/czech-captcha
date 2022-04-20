package com.example.captcha.datamanagement.objectmetadata.dto

import com.example.captcha.datamanagement.objectmetadata.*
import com.example.captcha.datamanagement.objectstorage.ObjectStorageInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo

//data class ObjectCreateDTO(val fileData: FileDataDTO, val fileType: FileTypeDTO, val metadata: FileMetadataDTO)
data class UrlObjectCreateDTO(val url: String, val fileType: FileTypeDTO, val metadata: ObjectMetadataCreateDTO)

data class ObjectDTO(val storageInfo: ObjectStorageInfo, val metadata: ObjectMetadata)

//// Needed for abstract type deserialization
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "storageType")
//sealed class FileDataDTO(val storageType: String)
//
//data class URLFileDataDTO(val url: String): FileDataDTO("url")

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
