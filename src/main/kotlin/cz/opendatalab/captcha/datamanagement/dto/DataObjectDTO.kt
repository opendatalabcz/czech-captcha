package cz.opendatalab.captcha.datamanagement.dto

import cz.opendatalab.captcha.datamanagement.objectmetadata.Labeling
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadata
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectType
import cz.opendatalab.captcha.datamanagement.objectmetadata.OtherMetadataType
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectRepositoryType
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo

data class DataObjectDTO(val metadata: ObjectMetadataDTO, val storageInfo: ObjectStorageInfoDTO)

data class ObjectStorageInfoDTO(
    val id: String,
    val originalName: String,
    val path: String,
    val repositoryType: ObjectRepositoryType
) {
    companion object {
        fun from(storage: ObjectStorageInfo): ObjectStorageInfoDTO {
            return ObjectStorageInfoDTO(storage.id, storage.originalName, storage.path, storage.repositoryType)
        }
    }
}

data class ObjectMetadataDTO(
    val owner: String,
    val objectType: ObjectType,
    val tags: Set<String>,
    val labels: MutableMap<String, Labeling>,
    val otherMetadata: Map<String, OtherMetadataType>
) {
    companion object {
        fun from(metadata: ObjectMetadata): ObjectMetadataDTO {
            return ObjectMetadataDTO(
                metadata.owner,
                metadata.objectType,
                metadata.tags,
                metadata.labels,
                metadata.otherMetadata
            )
        }
    }
}
