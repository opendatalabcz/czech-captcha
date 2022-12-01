package cz.opendatalab.captcha.datamanagement.objectmetadata

import org.springframework.data.mongodb.repository.MongoRepository

interface ObjectMetadataRepository: MongoRepository<ObjectMetadata, String> {}
