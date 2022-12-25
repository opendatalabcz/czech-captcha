package cz.opendatalab.captcha.datamanagement.objectdetection

import cz.opendatalab.captcha.datamanagement.ImageUtils
import org.imgscalr.Scalr
import org.jetbrains.kotlinx.dl.api.inference.loaders.ONNXModelHub
import org.jetbrains.kotlinx.dl.api.inference.onnx.ONNXModels
import org.jetbrains.kotlinx.dl.dataset.handler.cocoCategories
import org.jetbrains.kotlinx.dl.dataset.image.ImageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.File

@Component
class ObjectDetectorKotlinDL(
    @Value("\${datamanagement.objectdetection.cache-path}") private val cacheDir: String
): ObjectDetector {

    private val modelHub = ONNXModelHub(File(cacheDir))
    private val model = ONNXModels.ObjectDetection.EfficientDetD4.pretrainedModel(modelHub)
    private val size = model.inputShape[1].toInt()

    override fun detect(image: BufferedImage): List<DetectedObject> {
        val tensor = ImageConverter.toRawFloatArray(padImage(image))

        val detectedObjects = model.detectObjects(tensor)

        return mapToDetectedObjects(detectedObjects)
    }

    override fun getSupportedLabels(): Set<String> {
        return buildSet {
            addAll(cocoCategories.values)
        }
    }

    private fun mapToDetectedObjects(detectedObjects: List<org.jetbrains.kotlinx.dl.api.inference.objectdetection.DetectedObject>): List<DetectedObject> {
        return detectedObjects.map { obj ->
            DetectedObject(
                obj.classLabel,
                obj.probability.toDouble(),
                RelativeBoundingBox(
                    obj.xMin.toDouble(),
                    obj.yMin.toDouble(),
                    (obj.xMax - obj.xMin).toDouble(),
                    (obj.yMax - obj.yMin).toDouble()
                )
            )
        }
    }

    private fun padImage(inputImage: BufferedImage): BufferedImage {
        val resizedImage = Scalr.resize(inputImage, size)
        return ImageUtils.padImageToSquare(resizedImage)
    }
}
