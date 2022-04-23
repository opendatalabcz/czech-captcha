package com.example.captcha.task.taskconfig

import com.example.captcha.siteconfig.Parent
import com.example.captcha.task.templates.TaskTemplateRouter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.io.File
import java.net.URL

@Service
class TaskConfigService(val taskTemplateRouter: TaskTemplateRouter, val objectMapper: ObjectMapper) {
    fun getTaskConfigSchema(taskName: String): JsonNode {
        val resource = (getResource(getResourcePath(taskName)) ?: getResource(getResourcePath("default")))!!
        return objectMapper.readTree(resource.readText())
    }

    fun getTaskNames(): List<String> {
        return taskTemplateRouter.getTaskTypes().map { it.name }
    }

    private fun getResource(path: String): URL? {
        return TaskConfigService::class.java.getResource(path)
    }

    private fun getResourcePath(taskName: String): String {
        return "${File.separator}taskconfigschemas${File.separator}${taskName}.json"
    }
}
