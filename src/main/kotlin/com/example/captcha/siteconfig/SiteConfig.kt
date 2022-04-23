package com.example.captcha.siteconfig

import com.example.captcha.task.templates.GenerationConfig
import com.example.captcha.verification.entities.TaskType
import com.fasterxml.jackson.databind.JsonNode

data class SiteConfig(val siteKey: String, val secretKey: String, val userName: String, val taskConfig: TaskConfig)

data class TaskConfig(val taskType: TaskType, val generationConfig: GenerationConfig, val evaluationThreshold: Double)

data class TaskConfigDTO(val taskType: TaskType, val generationConfig: JsonNode, val evaluationThreshold: Double)
