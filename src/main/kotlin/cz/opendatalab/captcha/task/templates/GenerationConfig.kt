package cz.opendatalab.captcha.task.templates

sealed interface GenerationConfig

@TaskType("IMAGE_LABELING")
data class ImageLabelingGenerationConfig(val labelGroup: String, val tags: Set<String>, val owners: Set<String>): GenerationConfig

@TaskType("OBJECT_DETECTING")
data class ObjectDetectingGenerationConfig(val tags: Set<String>, val owners: Set<String>): GenerationConfig

object EmptyGenerationConfig: GenerationConfig


@Target(AnnotationTarget.CLASS)
annotation class TaskType(val name:String)
