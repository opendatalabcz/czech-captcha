package cz.opendatalab.captcha.task.templates

import cz.opendatalab.captcha.verification.Answer
import cz.opendatalab.captcha.verification.AnswerSheet
import cz.opendatalab.captcha.verification.entities.*
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
    fun generateTask(generationConfig: GenerationConfig, currentUser: String): Triple<Description, TaskData, AnswerSheet>
    fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult
}

@Service
class TaskTemplateRouter(val context: ApplicationContext) {

    lateinit var templates:Map<String, TaskTemplate>

    @EventListener(ApplicationReadyEvent::class)
    fun initializeAfterStartup() {
        templates = context.getBeansOfType(TaskTemplate::class.java)
    }

    fun generateTask(taskType: String, userName: String, generationConfig: GenerationConfig): Triple<Description, TaskData, AnswerSheet> {
        return getTemplate(taskType).generateTask(generationConfig, userName)
    }

    fun evaluateTask(taskType: String, taskData: TaskData, answer: Answer): EvaluationResult {
        return getTemplate(taskType).evaluateTask(taskData, answer)
    }

    private fun getTemplate(taskType: String): TaskTemplate {
        return templates[taskType] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Template with given task name is not implemented")
    }

    fun getTaskTypes(): List<String> {
        return templates.keys.stream().toList()
    }
}
