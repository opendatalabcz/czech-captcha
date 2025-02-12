package cz.opendatalab.captcha.task.templates

sealed interface GenerationConfig

@TaskType("Image Labeling")
data class ImageLabelingGenerationConfig(val labelGroup: String, val tags: Set<String>, val owners: Set<String>): GenerationConfig

@TaskType("Object Detection")
data class ObjectDetectingGenerationConfig(val tags: Set<String>, val owners: Set<String>): GenerationConfig

object EmptyGenerationConfig: GenerationConfig


@Target(AnnotationTarget.CLASS)
annotation class TaskType(val name:String)
