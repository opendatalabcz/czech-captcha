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
    private val objectStorageInfoRepository: ObjectStorageInfoRepository,
    private val objectRepository: ObjectRepository,
    @Value("\${datamanagement.image.max-size}") private var maxImageSize: Int
    ) {

    fun getObjectById(id: String): InputStream {
        return getObjectByStorageInfo(getInfoById(id))
    }

    private fun getObjectByStorageInfo(storageInfo: ObjectStorageInfo): InputStream {
        return objectRepository.getFile(storageInfo.path, storageInfo.repositoryType)
    }

    fun getImageById(id: String): BufferedImage {
        return ImageUtils.getImageFromInputStream(getObjectById(id))
    }

    fun getImageBase64StringById(id: String): String {
        val storageInfo = getInfoById(id)
        val format = Utils.getFileExtension(storageInfo.originalName)
        val bytes = Utils.getBytesFromInputStream(getObjectByStorageInfo(storageInfo))
        return ImageUtils.getBase64StringWithImage(bytes, format)
    }

    fun getInfoById(id: String): ObjectStorageInfo {
        return objectStorageInfoRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Object with id $id does not exist.")
    }

    fun getInfoByIdList(ids: Iterable<String>): List<ObjectStorageInfo> {
        return objectStorageInfoRepository.findAllById(ids).toList()
    }

    fun saveFileObject(content: InputStream, originalName: String): ObjectStorageInfo {
        val id = generateUniqueId()
        val format = Utils.getFileExtension(originalName)
        val objectType = ObjectType.fromFormat(format)
        val toSave = if (objectType.type == ObjectTypeEnum.IMAGE) ImageUtils.resizeImageFromInputStreamToMaxSize(
            content,
            maxImageSize,
            format
        ) else content

        val path = objectRepository.saveFile(toSave, "$id.$format", ObjectRepositoryType.FILESYSTEM)

        val toBeAdded = ObjectStorageInfo(id, originalName, path, ObjectRepositoryType.FILESYSTEM)
        objectStorageInfoRepository.insert(toBeAdded)

        return toBeAdded
    }

    fun saveUrlObject(url: String): ObjectStorageInfo {
        val filename = getFilenameFromUrl(url)
        val toBeAdded = ObjectStorageInfo(generateUniqueId(), filename, url, ObjectRepositoryType.URL)
        objectStorageInfoRepository.insert(toBeAdded)
        return toBeAdded
    }

    fun deleteObject(id: String) {
        val metadata = objectStorageInfoRepository.findByIdOrNull(id)
        metadata?.also {
            objectRepository.removeFile(it.path, it.repositoryType)
            objectStorageInfoRepository.deleteById(id)
        }
    }

    private fun getFilenameFromUrl(url: String): String {
        val urlWithoutParameters = DefaultUriBuilderFactory(url).builder().replaceQuery(null).build().toString()
        return urlWithoutParameters.substring(urlWithoutParameters.lastIndexOf('/') + 1)
    }
}
