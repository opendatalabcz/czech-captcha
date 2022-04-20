package com.example.captcha.task.taskconfig


import com.fasterxml.jackson.databind.JsonNode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/taskconfig")
class TaskConfigController(val taskConfigService: TaskConfigService) {

    @GetMapping("tasks")
    fun getTasks(): List<String> {
        return taskConfigService.getTaskNames()
    }

    @GetMapping("schemas")
    fun getTaskConfigSchema(@RequestParam("taskName") taskName: String): JsonNode {
        return taskConfigService.getTaskConfigSchema(taskName)
    }
}
