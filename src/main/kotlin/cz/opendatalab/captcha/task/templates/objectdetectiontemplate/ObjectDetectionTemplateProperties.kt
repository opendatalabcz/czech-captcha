package cz.opendatalab.captcha.task.templates.objectdetectiontemplate

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "task.templates.object-detection-template")
data class ObjectDetectionTemplateProperties @ConstructorBinding constructor(
    val answersNeededForFinalPosition: Int,
    val addDetectionDataThreshold: Double,
    val similarityThreshold: Double
    ) {}