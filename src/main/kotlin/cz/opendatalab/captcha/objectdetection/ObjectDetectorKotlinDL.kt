package cz.opendatalab.captcha.objectdetection

import org.imgscalr.Scalr
import org.jetbrains.kotlinx.dl.api.inference.loaders.ONNXModelHub
import org.jetbrains.kotlinx.dl.api.inference.onnx.ONNXModels
import org.jetbrains.kotlinx.dl.dataset.handler.cocoCategories
import org.jetbrains.kotlinx.dl.dataset.image.ImageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.max

@Component
class ObjectDetectorKotlinDL(
    @Value("\${objectdetection.cache}") private val cacheDir: String
): ObjectDetector {

    private val modelHub = ONNXModelHub(File(cacheDir))
    private val model = ONNXModels.ObjectDetection.EfficientDetD4.pretrainedModel(modelHub)
    private val size = model.inputShape[1].toInt()

    override fun detect(image: BufferedImage): List<DetectedObject> {
        val tensor = ImageConverter.toRawFloatArray(padImage(image))

        val detectedObjects = model.detectObjects(tensor)

        return convertToCoordinates(detectedObjects, image)
    }

    override fun getSupportedLabels(): Set<String> {
        return buildSet {
            addAll(cocoCategories.values)
        }
    }

    private fun convertToCoordinates(detectedObjects: List<org.jetbrains.kotlinx.dl.api.inference.objectdetection.DetectedObject>,
                                     image: BufferedImage): List<DetectedObject> {
        val biggerSide = max(image.width.toDouble(), image.height.toDouble())
        return detectedObjects.map { obj ->
            DetectedObject(
                obj.classLabel,
                obj.probability.toDouble(),
                BoundingBox(
                    (obj.xMin * biggerSide).toInt(),
                    (obj.yMin * biggerSide).toInt(),
                    ((obj.xMax - obj.xMin) * biggerSide).toInt(),
                    ((obj.yMax - obj.yMin) * biggerSide).toInt()
                )
            )
        }
    }

    private fun padImage(inputImage: BufferedImage): BufferedImage {
        val resizedImage = Scalr.resize(inputImage, size)
        val outputImage = BufferedImage(size, size, BufferedImage.TYPE_INT_BGR)
        val g2d = outputImage.createGraphics()
        g2d.color = Color.BLACK
        g2d.fillRect(0, 0, size, size)
        g2d.drawImage(resizedImage, 0, 0, null)
        g2d.dispose()

        return outputImage
    }
}
