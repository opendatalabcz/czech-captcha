package cz.opendatalab.captcha.datamanagement.objectdetection

import java.awt.image.BufferedImage

interface ObjectDetector {
    fun detect(image: BufferedImage): List<DetectedObject>
    fun getSupportedLabels(): Set<String>
}

object ObjectDetectionConstants {
    const val LABEL_GROUP = "object_detection"
}