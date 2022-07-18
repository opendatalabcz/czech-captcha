package cz.opendatalab.captcha.datamanagement.objectstorage

import cz.opendatalab.captcha.Utils.generateUniqueId
import cz.opendatalab.captcha.Utils.getFileExtension
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectType
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectTypeEnum
import org.imgscalr.Scalr
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
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
        val fileRepositoryType = ObjectRepositoryType.FILESYSTEM
        val id = generateUniqueId()

        val editedFile = if (objectType.type() == ObjectTypeEnum.IMAGE) resizeImage(file) else file

        val path = FileRepository.saveFile(editedFile, id, fileRepositoryType)

        val toBeAdded = ObjectStorageInfo(id, user, path, fileRepositoryType)
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

    private fun resizeImage(file: MultipartFile): MultipartFile {
        val originalImage: BufferedImage
        file.inputStream.use {
            originalImage = ImageIO.read(it)
        }
        val resizedImage: BufferedImage = Scalr.resize(originalImage, maxSize)

        return ByteArrayOutputStream().use {
            ImageIO.write(resizedImage, getFileExtension(file.originalFilename), it)
            MockMultipartFile(file.name, file.originalFilename, file.contentType, it.toByteArray())
        }
    }
}
