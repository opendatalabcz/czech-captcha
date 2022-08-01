package cz.opendatalab.captcha.datamanagement.objectstorage

import cz.opendatalab.captcha.Utils.generateUniqueId
import cz.opendatalab.captcha.datamanagement.objectmetadata.ImageObjectType
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectType
import cz.opendatalab.captcha.datamanagement.objectmetadata.SoundObjectType
import org.imgscalr.Scalr
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO


@Service
class ObjectService(
    private val objectCatalogue: ObjectCatalogue,
    @Value("\${datamanagement.image.max-size}") private var maxSize: Int
    ) {

    fun getById(id: String): InputStream? {
        val metadata = objectCatalogue.findByIdOrNull(id) ?: return null

        return FileRepository.getFile(metadata.path, metadata.repositoryType)
    }

    fun getInfoByIdList(ids: Iterable<String>): List<ObjectStorageInfo> {
        return objectCatalogue.findAllById(ids).toList()
    }

    fun saveFile(user: String, file: MultipartFile, objectType: ObjectType): String {
        if (objectType is ImageObjectType) {
            val image = file.inputStream.use { ImageIO.read(it) }
            return saveImageFile(image, objectType.format, user)
        }
        val fileRepositoryType = ObjectRepositoryType.FILESYSTEM
        val id = generateUniqueId()
        val fileName = id + getFileExtension(objectType, file.originalFilename)

        FileRepository.saveFile(file.inputStream, fileName, fileRepositoryType)

        val toBeAdded = ObjectStorageInfo(id, user, fileName, fileRepositoryType)
        objectCatalogue.insert(toBeAdded)

        return toBeAdded.id
    }

    fun saveURLFile(user: String, url: String): String {
        val toBeAdded = ObjectStorageInfo(generateUniqueId(), user, url, ObjectRepositoryType.URL)
        objectCatalogue.insert(toBeAdded)

        return toBeAdded.id
    }
    
    fun saveImageFile(image: BufferedImage, imageFormat: String, user: String): String {
        val fileRepositoryType = ObjectRepositoryType.FILESYSTEM
        val id = generateUniqueId()
        val fileName = "$id.$imageFormat"

        // resize image
        val resizedImage = Scalr.resize(image, maxSize)
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(resizedImage, imageFormat, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        FileRepository.saveFile(inputStream, fileName, fileRepositoryType)

        val toBeAdded = ObjectStorageInfo(id, user, fileName, fileRepositoryType)
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

    private fun getFileExtension(objectType: ObjectType, filename: String?): String {
        when (objectType) {
            is ImageObjectType -> return objectType.format
            is SoundObjectType -> return objectType.format
            else -> {
                filename ?: return ""
                val index = filename.lastIndexOf('.')
                if (index > 0) {
                    return filename.substring(index)
                }
                return ""
            }
        }
    }
}
