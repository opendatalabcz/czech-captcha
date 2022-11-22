package cz.opendatalab.captcha.datamanagement.objectstorage

import cz.opendatalab.captcha.datamanagement.ImageUtils
import cz.opendatalab.captcha.Utils
import cz.opendatalab.captcha.Utils.generateUniqueId
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectType
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectTypeEnum
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.util.DefaultUriBuilderFactory
import java.awt.image.BufferedImage
import java.io.InputStream

@Service
class ObjectService(
    private val objectCatalogue: ObjectCatalogue,
    @Value("\${datamanagement.image.max-size}") private var maxImageSize: Int
    ) {

    fun getObjectById(id: String): InputStream {
        val metadata = objectCatalogue.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Object with id $id does not exist.")

        return ObjectRepository.getFile(metadata.path, metadata.repositoryType)
    }

    fun getImageById(id: String): BufferedImage {
        return ImageUtils.getImageFromInputStream(getObjectById(id))
    }

    fun getInfoById(id: String): ObjectStorageInfo {
        return objectCatalogue.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Object with id $id does not exist.")
    }

    fun getInfoByIdList(ids: Iterable<String>): List<ObjectStorageInfo> {
        return objectCatalogue.findAllById(ids).toList()
    }

    fun saveFileObject(content: InputStream, originalName: String): ObjectStorageInfo {
        val id = generateUniqueId()
        val format = Utils.getFileExtension(originalName)
        val objectType = ObjectType.fromFormat(format)
        val toSave = if (objectType.type == ObjectTypeEnum.IMAGE) ImageUtils.resizeInputStreamToMaxSize(
            content,
            maxImageSize,
            format
        ) else content

        val path = ObjectRepository.saveFile(toSave, "$id.$format", ObjectRepositoryType.FILESYSTEM)

        val toBeAdded = ObjectStorageInfo(id, originalName, path, ObjectRepositoryType.FILESYSTEM)
        objectCatalogue.insert(toBeAdded)

        return toBeAdded
    }

    fun saveUrlObject(url: String): ObjectStorageInfo {
        val filename = getFilenameFromUrl(url)
        val toBeAdded = ObjectStorageInfo(generateUniqueId(), filename, url, ObjectRepositoryType.URL)
        objectCatalogue.insert(toBeAdded)
        return toBeAdded
    }

    fun deleteObject(id: String) {
        val metadata = objectCatalogue.findByIdOrNull(id)
        metadata?.also {
            ObjectRepository.removeFile(it.path, it.repositoryType)
            objectCatalogue.deleteById(id)
        }
    }

    private fun getFilenameFromUrl(url: String): String {
        val urlWithoutParameters = DefaultUriBuilderFactory(url).builder().replaceQuery(null).build().toString()
        return urlWithoutParameters.substring(urlWithoutParameters.lastIndexOf('/') + 1)
    }
}
