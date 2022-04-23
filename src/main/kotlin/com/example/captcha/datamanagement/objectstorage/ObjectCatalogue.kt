package com.example.captcha.datamanagement.objectstorage

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component


//interface ObjectCatalogue {
//    fun getById(id: Long): ObjectStorageInfo?
//    fun getAll(): List<ObjectStorageInfo>
//    fun addFile(user: String, path: String, repositoryType: ObjectRepositoryType): ObjectStorageInfo
//    fun deleteFile(id: Long)
//}

interface ObjectCatalogue: MongoRepository<ObjectStorageInfo, String>

//@Component
//class InMemoryObjectCatalogue: ObjectCatalogue {
//
//    var lastId = 0L
//
//    val repo = mutableListOf<ObjectStorageInfo>(
//        ObjectStorageInfo(getNewId(), "system", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRIRGRnOnRec-So4shuqTQqpNRO0ozrxJFPUg&usqp=CAU", ObjectRepositoryType.URL),
//        ObjectStorageInfo(getNewId(), "system", "https://cdn.iconscout.com/icon/premium/png-64-thumb/dog-10-110145.png", ObjectRepositoryType.URL)
//    )
//
//    override fun getById(id: Long): ObjectStorageInfo? {
//        return repo.find { metadata -> metadata.id == id }
//    }
//
//    override fun getAll(): List<ObjectStorageInfo> {
//        return repo
//    }
//
//    override fun addFile(user: String, path: String, repositoryType: ObjectRepositoryType): ObjectStorageInfo {
//        val id = getNewId()
//        val metadata = ObjectStorageInfo(id, user, path, repositoryType)
//        repo.add(metadata)
//        return metadata
//    }
//
//    override fun deleteFile(id: Long) {
//        repo.removeIf{metadata -> metadata.id == id}
//    }
//
//    final fun getNewId(): Long {
//        return ++lastId
//    }
//}
