package cz.opendatalab.captcha.datamanagement.objectdetection

import cz.opendatalab.captcha.datamanagement.ImageUtils
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.InputStream
import java.util.*

@Service
class ObjectDetectionService(private val objectDetector: ObjectDetector) {
    fun getSupportedLabels(): Set<String> {
        return objectDetector.getSupportedLabels()
    }

    fun detectObjects(image: BufferedImage, wantedLabels: Set<String>): List<DetectedObject> {
        return objectDetector.detect(image).filter { obj -> wantedLabels.contains(obj.label) }
    }

    fun detectObjects(imageStream: InputStream, wantedLabels: Set<String>): List<DetectedObject> {
        val image = ImageUtils.getImageFromInputStream(imageStream)
        return detectObjects(image, wantedLabels)
    }

    fun detectObjectsWithOverlaps(image: BufferedImage, wantedLabels: Set<String>): List<DetectedObjectWithOverlappingLabels> {
        val detectedObjects = detectObjects(image, wantedLabels)
        return addOverlappingLabels(detectedObjects)
    }

    fun detectObjectsWithOverlaps(imageStream: InputStream, wantedLabels: Set<String>): List<DetectedObjectWithOverlappingLabels> {
        val detectedObjects = detectObjects(imageStream, wantedLabels)
        return addOverlappingLabels(detectedObjects)
    }

    private fun addOverlappingLabels(objects: List<DetectedObject>): MutableList<DetectedObjectWithOverlappingLabels> {
        val objectsWithOverlappingLabels = mutableListOf<DetectedObjectWithOverlappingLabels>()
        val objectsLabels = List(objects.size) { mutableMapOf<String, Double>() }
        for (i in objects.indices) {
            for (j in i + 1 until objects.size) {
                addTwoOverlappingLabels(objectsLabels, objects, i, j)
            }
            addOriginalLabel(objectsLabels[i], objects[i])
            objectsWithOverlappingLabels.add(DetectedObjectWithOverlappingLabels(objectsLabels[i], objects[i].relativeBoundingBox))
        }
        return objectsWithOverlappingLabels
    }

    private fun addTwoOverlappingLabels(
        objectsLabels: List<MutableMap<String, Double>>,
        objects: List<DetectedObject>,
        i1: Int,
        i2: Int
    ) {
        val o1 = objects[i1]
        val o2 = objects[i2]
        val b1 = o1.relativeBoundingBox
        val b2 = o2.relativeBoundingBox
        val intersection = RelativeBoundingBox.calculateIntersection(b1, b2)
        if (intersection <= 0.0) {
            return
        }
        setNewProbabilityOrLeaveBigger(objectsLabels[i1], o2.label, calculateProbabilityForOverlappingLabels(intersection, b1.calculateArea(), o2.probability))
        setNewProbabilityOrLeaveBigger(objectsLabels[i2], o1.label, calculateProbabilityForOverlappingLabels(intersection, b2.calculateArea(), o1.probability))
    }

    private fun addOriginalLabel(allLabels: MutableMap<String, Double>, originalObj: DetectedObject) {
        setNewProbabilityOrLeaveBigger(allLabels, originalObj.label, originalObj.probability)
    }

    private fun setNewProbabilityOrLeaveBigger(allLabels: MutableMap<String, Double>, key: String, newProbability: Double) {
        val originalProbability = allLabels[key] ?: 0.0
        if (originalProbability < newProbability) {
            allLabels[key] = newProbability
        }
    }

    private fun calculateProbabilityForOverlappingLabels(intersection: Double, area: Double, originalProbability: Double): Double {
        return intersection / area * originalProbability
    }
}