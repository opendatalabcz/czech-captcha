package cz.opendatalab.captcha.objectdetection

import java.awt.image.BufferedImage

interface ObjectDetector {
    fun detect(image: BufferedImage): List<DetectedObject>
    fun getSupportedLabels(): Set<String>
}