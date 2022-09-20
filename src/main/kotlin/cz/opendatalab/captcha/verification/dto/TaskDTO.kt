package cz.opendatalab.captcha.verification.dto

import cz.opendatalab.captcha.verification.entities.AnswerSheet
import cz.opendatalab.captcha.verification.entities.Task
import java.time.Instant

data class TaskDTO(
    val id: String,
    val expiration: Instant,
    val description: String,
    val answerSheet: AnswerSheet,
    val taskType: String,
) {
    companion object {
        fun fromTask(task: Task, id: String, answerSheet: AnswerSheet): TaskDTO {
            return TaskDTO(id, task.expiration, task.description.text, answerSheet, task.type)
        }
    }
}
