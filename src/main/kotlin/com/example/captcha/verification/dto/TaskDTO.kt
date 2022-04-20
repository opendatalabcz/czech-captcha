package com.example.captcha.verification.dto

import com.example.captcha.verification.AnswerSheet
import com.example.captcha.verification.entities.Task
import com.example.captcha.verification.entities.TaskType
import java.time.Instant

data class TaskDTO(
    val id: String,
    val expiration: Instant,
    val description: String,
    val answerSheet: AnswerSheet,
    val taskType: TaskType,
) {
    companion object {
        fun fromTask(task: Task, id: String, answerSheet: AnswerSheet): TaskDTO {
            return TaskDTO(id, task.expiration, task.description.text, answerSheet, task.type)
        }
    }
}
