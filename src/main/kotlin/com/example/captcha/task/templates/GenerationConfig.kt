package com.example.captcha.task.templates

sealed interface GenerationConfig

@TaskType("IMAGE_LABELING")
data class ImageLabelingGenerationConfig(val labelGroup: String, val tags: List<String>, val owners: List<String>): GenerationConfig

object EmptyGenerationConfig: GenerationConfig


@Target(AnnotationTarget.CLASS)
annotation class TaskType(val name:String)
