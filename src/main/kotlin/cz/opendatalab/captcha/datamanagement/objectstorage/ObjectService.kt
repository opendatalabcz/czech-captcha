package cz.opendatalab.captcha.datamanagement.objectstorage

import cz.opendatalab.captcha.Utils.generateUniqueId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

@Service
class ObjectService(private val objectCatalogue: ObjectCatalogue) {
    fun getById(id: String): InputStream? {
        val metadata = objectCatalogue.findByIdOrNull(id) ?: return null

        return FileRepository.getFile(metadata.path, metadata.repositoryType)
    }

    fun getInfoByIdList(ids: Iterable<String>): List<ObjectStorageInfo> {
        return objectCatalogue.findAllById(ids).toList()
    }

    fun saveFile(user: String, file: MultipartFile, fileName: String): String {
        // TODO do not hardcode this
        val fileRepositoryType = ObjectRepositoryType.FILESYSTEM
        val path = FileRepository.saveFile(file, fileName, fileRepositoryType)

        val toBeAdded = ObjectStorageInfo(generateUniqueId(), user, path, fileRepositoryType)
        objectCatalogue.insert(toBeAdded)

        return toBeAdded.id
    }

    fun saveURLFile(user: String, url: String): String {
        val toBeAdded = ObjectStorageInfo(generateUniqueId(), user, url, ObjectRepositoryType.URL)
        objectCatalogue.insert(toBeAdded)

        return toBeAdded.id
    }

    fun deleteFile(id: String) {
        val metadata = objectCatalogue.findByIdOrNull(id)
        metadata?.also {
            objectCatalogue.deleteById(id)
            FileRepository.removeFile(it.path, it.repositoryType)
        }
    }
}
