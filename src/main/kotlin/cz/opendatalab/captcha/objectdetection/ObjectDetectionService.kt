package cz.opendatalab.captcha.objectdetection

import cz.opendatalab.captcha.Utils
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import org.imgscalr.Scalr
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

@Service
class ObjectDetectionService(private val objectService: ObjectService,
                             private val objectDetector: ObjectDetector) {

    fun getSupportedLabels(): Set<String> {
        return objectDetector.getSupportedLabels()
    }

    fun detectObjects(fileId: String, imageFormat: String, user: String, wantedLabels: List<String>): List<DetectedImage> {
        val inputStream = objectService.getById(fileId) ?: throw IllegalArgumentException("File with id $fileId cannot be accessed.")
        val originalName = objectService.getInfoById(fileId)?.originalName ?: (fileId + imageFormat)
        val image = ImageIO.read(inputStream) ?: throw IllegalStateException("Cannot read image with id $fileId")
        inputStream.close()

        val detectedObjects = objectDetector.detect(image).filter { obj -> wantedLabels.contains(obj.label) }
        return saveDetectedObjects(image, imageFormat, detectedObjects, originalName, user)
    }

    private fun saveDetectedObjects(
        image: BufferedImage,
        imageFormat: String,
        detectedObjects: List<DetectedObject>,
        parentOriginalName: String,
        user: String
    ): List<DetectedImage> {
        val result = mutableListOf<DetectedImage>()
        for ((i, obj) in detectedObjects.withIndex()) {
            val croppedImage = Scalr.crop(image, obj.absoluteBoundingBox.x, obj.absoluteBoundingBox.y, obj.absoluteBoundingBox.width, obj.absoluteBoundingBox.height)
            val originalName = "${parentOriginalName}-detected$i.${Utils.getFileExtension(parentOriginalName)}"
            val id = objectService.saveImageFile(croppedImage, imageFormat, originalName, user)
            result.add(DetectedImage(id, mapOf(obj.label to obj.probability)))
        }
        return result
    }
}

data class DetectedImage(val id: String, val labels: Map<String, Double>)