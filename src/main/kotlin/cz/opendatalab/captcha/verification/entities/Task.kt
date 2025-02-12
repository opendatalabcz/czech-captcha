package cz.opendatalab.captcha.verification.entities

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
