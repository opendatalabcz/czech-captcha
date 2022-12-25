package cz.opendatalab.captcha.task.templates.imagelabelingtemplate

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "task.templates.image-labeling-template")
data class ImageLabelingTemplateProperties @ConstructorBinding constructor(
    val labelUnknownThreshold: Double,
    val totalImagesCount: Int,
    val unknownLabelCount: Int,
    val minWithLabel: Int
    ) {}