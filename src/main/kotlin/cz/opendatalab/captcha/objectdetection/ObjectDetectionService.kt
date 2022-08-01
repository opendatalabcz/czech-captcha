package cz.opendatalab.captcha.objectdetection

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
        val image = ImageIO.read(inputStream) ?: throw IllegalStateException("Cannot read image with id $fileId")
        inputStream.close()

        val detectedObjects = objectDetector.detect(image).filter { obj -> wantedLabels.contains(obj.label) }
        return saveDetectedObjects(image, imageFormat, detectedObjects, user)
    }

    private fun saveDetectedObjects(
        image: BufferedImage,
        imageFormat: String,
        detectedObjects: List<DetectedObject>,
        user: String
    ): List<DetectedImage> {
        val result = mutableListOf<DetectedImage>()
        for (obj in detectedObjects) {
            val croppedImage = Scalr.crop(image, obj.x, obj.y, obj.width, obj.height)
            val id = objectService.saveImageFile(croppedImage, imageFormat, user)
            // todo add overlapping objects
            result.add(DetectedImage(id, mapOf(obj.label to obj.probability)))
        }
        return result
    }
}

data class DetectedImage(val id: String, val labels: Map<String, Double>)