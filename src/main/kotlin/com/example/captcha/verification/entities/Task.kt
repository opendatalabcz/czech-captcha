package com.example.captcha.verification.entities

import java.time.Instant

data class Task(
    val type: String,
    val siteKey: String,
    val expiration: Instant,
    val description: Description,
    val data: TaskData
    )

@JvmInline
value class Description(val text: String)

//enum class TaskType {
//    NUMERIC_EQUATION, SIMPLE_IMAGE, IMAGE_LABELING, TEXT
//}

