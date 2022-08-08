package cz.opendatalab.captcha.objectdetection

data class DetectedObject(val label: String, val probability: Double, val boundingBox: BoundingBox)

data class BoundingBox(val x: Int, val y: Int, val width: Int, val height: Int)