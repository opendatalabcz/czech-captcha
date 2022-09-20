package cz.opendatalab.captcha.objectdetection

import cz.opendatalab.captcha.verification.entities.ImageSize

data class DetectedObject(val label: String, val probability: Double, val absoluteBoundingBox: AbsoluteBoundingBox)

/**
 * Bounding box represented by width and height and by x and y coordinate from top left corner
 * (all relative to the image size).
 */
data class RelativeBoundingBox(val x: Double, val y: Double, val width: Double, val height: Double) {
    fun isInImage(): Boolean {
        if (x < 0.0 || y < 0.0 || x + width > 1.0 || y + height > 1.0) {
            return false
        }
        return true
    }

    fun toAbsoluteBoundingBox(imageSize: ImageSize): AbsoluteBoundingBox {
        val absX = (imageSize.width * x).toInt()
        val absY = (imageSize.height * y).toInt()
        val absW = (imageSize.width * width).toInt()
        val absH = (imageSize.height * height).toInt()
        return AbsoluteBoundingBox(absX, absY, absW, absH)
    }
}

/**
 * Bounding box represented by absolute (in px) width and height and by x and y coordinate from top left corner
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