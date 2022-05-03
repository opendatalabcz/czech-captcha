package cz.opendatalab.captcha.datamanagement.dto

import cz.opendatalab.captcha.datamanagement.objectmetadata.Labeling
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadata
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectType
import cz.opendatalab.captcha.datamanagement.objectmetadata.OtherMetadataType
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectRepositoryType
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo

data class DataObjectDTO(val metadata: ObjectMetadataDTO, val storageInfo: ObjectStorageInfoDTO)

data class ObjectStorageInfoDTO(val id: String, val owner: String, val path: String, val repositoryType: ObjectRepositoryType) {
    companion object {
        fun from(info: ObjectStorageInfo): ObjectStorageInfoDTO {
            return ObjectStorageInfoDTO(info.id, info.user, info.path, info.repositoryType)
        }
    }
}

data class ObjectMetadataDTO(val objectType: ObjectType, val labels: MutableMap<String, Labeling>,
                             val templateData: Map<String, OtherMetadataType>, val tags: List<String>) {
    companion object {
        fun from(metadata: ObjectMetadata): ObjectMetadataDTO {
            return ObjectMetadataDTO(metadata.objectType, metadata.labels, metadata.templateData, metadata.tags)
        }
    }
}
