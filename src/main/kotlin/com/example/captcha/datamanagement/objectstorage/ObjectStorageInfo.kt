package com.example.captcha.datamanagement.objectstorage

/**
 * ObjectStorageInfo are metadata about objects used for the technical purpose of storing the objects
 */
data class ObjectStorageInfo(val id: Long, val user: String, val path: String, val repositoryType: ObjectRepositoryType)


enum class ObjectRepositoryType {
    OBJECT_STORAGE, FILESYSTEM, URL
}
