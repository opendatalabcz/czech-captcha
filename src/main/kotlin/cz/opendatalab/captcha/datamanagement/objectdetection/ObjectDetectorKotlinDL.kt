package cz.opendatalab.captcha.datamanagement.objectdetection

import cz.opendatalab.captcha.datamanagement.ImageUtils
import mu.KotlinLogging
import org.imgscalr.Scalr
import org.jetbrains.kotlinx.dl.api.inference.loaders.ONNXModelHub
import org.jetbrains.kotlinx.dl.api.inference.onnx.ONNXModels
import org.jetbrains.kotlinx.dl.dataset.handler.cocoCategories
import org.jetbrains.kotlinx.dl.dataset.image.ImageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.File
import kotlin.system.measureTimeMillis

@Component
class ObjectDetectorKotlinDL(
    @Value("\${datamanagement.objectdetection.cache-path}") private val cacheDir: String
): ObjectDetector {
    private val modelHub = ONNXModelHub(File(cacheDir))
    private val model = ONNXModels.ObjectDetection.EfficientDetD4.pretrainedModel(modelHub)
    private val size = model.inputShape[1].toInt()
    private val logger = KotlinLogging.logger {}

    override fun detect(image: BufferedImage): List<DetectedObject> {
        val resizedImage = Scalr.resize(image, size)
        val imageSize = ImageSize(resizedImage)
        val paddedImage = ImageUtils.padImageToSquare(resizedImage)

        val detectedObjects = doObjectDetection(paddedImage)

        return mapToDetectedObjects(detectedObjects, imageSize)
    }

    override fun getSupportedLabels(): Set<String> {
        return buildSet {
            addAll(cocoCategories.values)
        }
    }

    private fun doObjectDetection(paddedImage: BufferedImage): List<org.jetbrains.kotlinx.dl.api.inference.objectdetection.DetectedObject> {
        var detectedObjects: List<org.jetbrains.kotlinx.dl.api.inference.objectdetection.DetectedObject>
        val elapsed = measureTimeMillis {
            val tensor = ImageConverter.toRawFloatArray(paddedImage)
            detectedObjects = model.detectObjects(tensor)
        }
        logger.info("Object detection took {} ms", elapsed)
        return detectedObjects
    }

    private fun mapToDetectedObjects(
        detectedObjects: List<org.jetbrains.kotlinx.dl.api.inference.objectdetection.DetectedObject>,
        imageSize: ImageSize
    ): List<DetectedObject> {
        return detectedObjects.map { obj ->
            DetectedObject(
                obj.classLabel,
                obj.probability.toDouble(),
                calculateBoundingBox(obj.xMin.toDouble(), obj.xMax.toDouble(), obj.yMin.toDouble(), obj.yMax.toDouble(), imageSize)
            )
        }
    }

    private fun calculateBoundingBox(
        xMin: Double, xMax: Double, yMin: Double, yMax: Double, imageSize: ImageSize): RelativeBoundingBox {
        var x = xMin
        var y = yMin
        var width = xMax - xMin
        var height = yMax - yMin
        if (imageSize.isWidthBigger()) { // correct padding
            val ratio = size.toDouble() / imageSize.height.toDouble()
            y *= ratio
            height *= ratio
        } else {
            val ratio = size.toDouble() / imageSize.width.toDouble()
            x *= ratio
            width *= ratio
        }
        if (x + width > 1.0) {
            width = 1.0 - x
        }
        if (y + height > 1.0) {
            height = 1.0 - y
        }
        return RelativeBoundingBox(x, y, width, height)
    }
}
