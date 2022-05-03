package cz.opendatalab.captcha.task.taskconfig

import cz.opendatalab.captcha.task.templates.TaskTemplateRouter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.io.File
import java.net.URL

@Service
class TaskConfigService(private val taskTemplateRouter: TaskTemplateRouter, private val objectMapper: ObjectMapper) {
    fun getTaskConfigSchema(taskName: String): JsonNode {
        val resource = (getResource(getResourcePath(taskName)) ?: getResource(getResourcePath("default")))!!
        return objectMapper.readTree(resource.readText())
    }

    fun getTaskNames(): List<String> {
        return taskTemplateRouter.getTaskTypes()
    }

    private fun getResource(path: String): URL? {
        return TaskConfigService::class.java.getResource(path)
    }

    private fun getResourcePath(taskName: String): String {
        return "${File.separator}taskconfigschemas${File.separator}${taskName}.json"
    }
}
