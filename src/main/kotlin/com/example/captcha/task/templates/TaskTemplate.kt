package com.example.captcha.task.templates

import com.example.captcha.verification.Answer
import com.example.captcha.verification.AnswerSheet
import com.example.captcha.verification.entities.*
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.streams.toList


interface TaskTemplate {
    // Description, TaskData
    fun generateTask(generationConfig: JsonNode, currentUser: String): Triple<Description, TaskData, AnswerSheet>
    fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult
}

@Service
class TaskTemplateRouter(val context: ApplicationContext) {

    lateinit var templates:Map<TaskType, TaskTemplate>

    @EventListener(ApplicationReadyEvent::class)
    fun initializeAfterStartup() {
        templates = context.getBeansOfType(TaskTemplate::class.java).mapKeys { (key, _) -> TaskType(key) }
    }

    fun generateTask(taskType: TaskType, userName: String, generationConfig: JsonNode): Triple<Description, TaskData, AnswerSheet> {
        return getTemplate(taskType).generateTask(generationConfig, userName)
    }

    fun evaluateTask(taskType: TaskType, taskData: TaskData, answer: Answer): EvaluationResult {
        return getTemplate(taskType).evaluateTask(taskData, answer)
    }

    private fun getTemplate(taskType: TaskType): TaskTemplate {
        return templates[taskType] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Template with given task name is not implemented")
    }

    fun getTaskTypes(): List<TaskType> {
        return templates.keys.stream().toList()
    }
}
