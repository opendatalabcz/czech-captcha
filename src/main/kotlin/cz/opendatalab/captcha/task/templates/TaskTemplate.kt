package cz.opendatalab.captcha.task.templates

import cz.opendatalab.captcha.verification.entities.*
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException


interface TaskTemplate {
    // Description, TaskData
    fun generateTask(generationConfig: GenerationConfig, currentUser: String): Triple<Description, TaskData, AnswerSheet>
    fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult
}

@Service
class TaskTemplateRouter(private val context: ApplicationContext) {

    private lateinit var templates:Map<String, TaskTemplate>

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

    fun getTaskTypes(): Set<String> {
        return templates.keys
    }
}
