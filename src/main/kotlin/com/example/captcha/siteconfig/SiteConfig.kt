package com.example.captcha.siteconfig

import com.example.captcha.task.templates.GenerationConfig
import com.fasterxml.jackson.databind.JsonNode
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("siteconfig")
data class SiteConfig(val siteKey: String, val secretKey: String, val userName: String, val taskConfig: TaskConfig)

class TaskConfig(val taskType: String, val generationConfig: GenerationConfig, val evaluationThreshold: Double)

data class TaskConfigDTO(val taskType: String, val generationConfig: JsonNode, val evaluationThreshold: Double)
