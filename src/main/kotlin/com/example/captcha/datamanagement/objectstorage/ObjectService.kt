package com.example.captcha.datamanagement.objectstorage

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

@Service
class ObjectService(private val objectCatalogue: ObjectCatalogue) {
    fun getById(id: Long): InputStream? {
        val metadata = objectCatalogue.getById(id) ?: return null

        return FileRepository.getFile(metadata.path, metadata.repositoryType)
    }

    fun saveFile(user: String, file: MultipartFile, fileName: String): Long {
        // TODO do not hardcode this
        val fileRepositoryType = ObjectRepositoryType.FILESYSTEM
        val path = FileRepository.saveFile(file, fileName, fileRepositoryType)

        val metadata = objectCatalogue.addFile(user, path, fileRepositoryType)

        return metadata.id
    }

    fun saveURLFile(user: String, url: String): Long {
        val metadata = objectCatalogue.addFile(user, url, ObjectRepositoryType.URL)

        return metadata.id
    }

    fun deleteFile(id: Long) {
        val metadata = objectCatalogue.getById(id)
        metadata?.also {
            objectCatalogue.deleteFile(id)
            FileRepository.removeFile(it.path, it.repositoryType)
        }
    }
}
