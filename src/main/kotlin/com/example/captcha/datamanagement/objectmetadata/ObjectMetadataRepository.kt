package com.example.captcha.datamanagement.objectmetadata

import org.springframework.data.mongodb.repository.MongoRepository

interface ObjectMetadataRepository: MongoRepository<ObjectMetadata, String> {
    fun findByObjectId(objectId: String): ObjectMetadata?
}
