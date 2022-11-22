package cz.opendatalab.captcha.datamanagement.dto

import cz.opendatalab.captcha.datamanagement.objectdetection.RelativeBoundingBox
import cz.opendatalab.captcha.datamanagement.objectmetadata.*
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo

data class UrlObjectCreateDTO(val url: String,
                              val metadata: ObjectMetadataCreateDTO) {
    companion object {
        fun fromUrlImageCreateDTO(urlImageCreateDTO: UrlImageCreateDTO): UrlObjectCreateDTO {
            return UrlObjectCreateDTO(
                urlImageCreateDTO.url,
                urlImageCreateDTO.metadata
            )
        }
    }
}

data class FileObjectCreateDTO(val metadata: ObjectMetadataCreateDTO) {
    companion object {
        fun fromFileImageCreateDTO(fileImageCreateDTO: FileImageCreateDTO): FileObjectCreateDTO {
            return FileObjectCreateDTO(fileImageCreateDTO.metadata)
        }
    }
}

data class UrlImageCreateDTO(val url: String,
                             val metadata: ObjectMetadataCreateDTO,
                             val objectDetection: ObjectDetectionDTO)

data class FileImageCreateDTO(val metadata: ObjectMetadataCreateDTO,
                              val objectDetection: ObjectDetectionDTO)

data class ObjectDTO(val storageInfo: ObjectStorageInfo, val metadata: ObjectMetadata)

data class ObjectMetadataCreateDTO(val knownLabels: Map<String, Set<String>>, val tags: Set<String>)

data class ObjectDetectionDTO(val objectDetectionParameters: ObjectDetectionParametersDTO?,
                              val annotations: List<AnnotationDTO>?)

data class ObjectDetectionParametersDTO(val wantedLabels: Map<String, Set<String>>,
                                        val thresholdOneVote: Double,
                                        val thresholdTwoVotes: Double)

data class AnnotationDTO(val labelGroup: String,
                         val label: String,
                         val boundingBox: RelativeBoundingBox)
