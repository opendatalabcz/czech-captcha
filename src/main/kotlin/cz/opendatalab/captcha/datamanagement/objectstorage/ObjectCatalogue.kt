package cz.opendatalab.captcha.datamanagement.objectstorage

import org.springframework.data.mongodb.repository.MongoRepository

interface ObjectCatalogue: MongoRepository<ObjectStorageInfo, String>
