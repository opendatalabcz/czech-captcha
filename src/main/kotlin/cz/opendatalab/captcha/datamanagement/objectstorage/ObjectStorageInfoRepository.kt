package cz.opendatalab.captcha.datamanagement.objectstorage

import org.springframework.data.mongodb.repository.MongoRepository

interface ObjectStorageInfoRepository: MongoRepository<ObjectStorageInfo, String>
