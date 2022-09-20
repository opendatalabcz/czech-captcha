package cz.opendatalab.captcha.task.templates

import cz.opendatalab.captcha.datamanagement.objectmetadata.ImageObjectType
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadata
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.verification.entities.ImageDisplayData
import java.util.*

object TemplateUtils {
    fun toDisplayImage(objectService: ObjectService, objectMetadata: ObjectMetadata): ImageDisplayData {
        val imageId = objectMetadata.objectId
        val format = (objectMetadata.objectType as ImageObjectType).format

        val inputStream = objectService.getById(imageId) ?:
        throw IllegalStateException("File with id $imageId cannot be accessed.")
        val base64ImageString = toBase64Image(inputStream.readAllBytes(), format)
        inputStream.close()

        return ImageDisplayData(base64ImageString)
    }

    fun toBase64Image(bytes: ByteArray, imageFormat: String): String {
        val prefix = "data:image/$imageFormat;base64,"
        return prefix + toBase64String(bytes)
    }

    private fun toBase64String(bytes: ByteArray): String {
        return String(Base64.getEncoder().encode(bytes))
    }
}
