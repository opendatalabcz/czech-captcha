package cz.opendatalab.captcha.datamanagement.objectstorage

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * ObjectStorageInfo are metadata about objects used for the technical purpose of storing the objects
 */
@Document("objectstorageinfo")
data class ObjectStorageInfo(@Id val id: String, val originalName: String, val user: String, val path: String, val repositoryType: ObjectRepositoryType)


enum class ObjectRepositoryType {
    OBJECT_STORAGE, FILESYSTEM, URL
}
