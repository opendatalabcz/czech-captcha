package com.example.captcha.siteconfig

import com.example.captcha.verification.entities.TaskType
import com.fasterxml.jackson.databind.JsonNode

data class SiteConfig(val siteKey: String, val secretKey: String, val userName: String, val taskConfig: TaskConfig)

data class TaskConfig(val taskType: TaskType, val generationConfig: JsonNode, val evaluationThreshold: Double)
