package com.example.captcha.verification.entities

import java.time.Instant

data class Task(
    val type: TaskType,
    val siteKey: String,
    val expiration: Instant,
    val description: Description,
    val data: TaskData
    )

@JvmInline
value class Description(val text: String)


@JvmInline
value class TaskType(val name: String)

//enum class TaskType {
//    NUMERIC_EQUATION, SIMPLE_IMAGE, IMAGE_LABELING, TEXT
//}

