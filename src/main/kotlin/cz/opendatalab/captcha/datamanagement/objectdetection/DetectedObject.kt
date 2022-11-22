package cz.opendatalab.captcha.datamanagement.objectdetection

import cz.opendatalab.captcha.verification.entities.ImageSize
import kotlin.math.max
import kotlin.math.min

data class DetectedObject(val label: String, val probability: Double, val relativeBoundingBox: RelativeBoundingBox)

data class DetectedObjectWithOverlappingLabels(val labelsWithProbability: Map<String, Double>, val relativeBoundingBox: RelativeBoundingBox)

/**
 * Bounding box represented by width and height and by x and y coordinate of the top left corner
 * (all relative to the image size).
 */
data class RelativeBoundingBox(val x: Double, val y: Double, val width: Double, val height: Double) {
    init {
        if (x < 0.0 || y < 0.0 || x >= 1.0 || y >= 1.0) {
            throw IllegalArgumentException("Relative bounding box position must be in an image.")
        }
        if (width <= 0.0 || height <= 0.0 || width > 1.0 || height > 1.0) {
            throw IllegalArgumentException("Relative bounding box size must be bigger than 0 and smaller or equal to 1.")
        }
        if (x + width > 1.0 || y + height > 1.0) {
            throw IllegalArgumentException("Whole relative bounding box must be inside an image.")
        }
    }

    fun isInImage(): Boolean {
        if (x < 0.0 || y < 0.0 || x + width > 1.0 || y + height > 1.0) {
            return false
        }
        return true
    }

    fun calculateArea(): Double {
        return width * height
    }

    fun toAbsoluteBoundingBox(imageSize: ImageSize): AbsoluteBoundingBox {
        val absX = (imageSize.width * x).toInt()
        val absY = (imageSize.height * y).toInt()
        val absW = (imageSize.width * width).toInt()
        val absH = (imageSize.height * height).toInt()
        return AbsoluteBoundingBox(absX, absY, absW, absH)
    }

    companion object {
        fun calculateIntersection(b1: RelativeBoundingBox, b2: RelativeBoundingBox): Double {
            val xIntersection = max(0.0, min(b1.x + b1.width, b2.x + b2.width) - max(b1.x, b2.x))
            val yIntersection = max(0.0, min(b1.y + b1.height, b2.y + b2.height) - max(b1.y, b2.y))
            return xIntersection * yIntersection
        }

        fun calculateUnion(b1: RelativeBoundingBox, b2: RelativeBoundingBox): Double {
            return b1.width * b1.height + b2.width * b2.height - calculateIntersection(b1, b2)
        }

        fun calculateIoU(b1: RelativeBoundingBox, b2: RelativeBoundingBox): Double {
            return calculateIntersection(b1, b2) / calculateUnion(b1, b2)
        }
    }
}

/**
 * Bounding box represented by absolute (in px) width and height and by absolute x and y coordinate of the top left corner
 */
data class AbsoluteBoundingBox(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun toRelativeBoundingBox(imageSize: ImageSize): RelativeBoundingBox {
        val relX = x.toDouble() / imageSize.width
        val relY = y.toDouble() / imageSize.height
        val relW = width.toDouble() / imageSize.width
        val relH = height.toDouble() / imageSize.height
        return RelativeBoundingBox(relX, relY, relW, relH)
    }
}