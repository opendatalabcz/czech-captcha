package com.example.captcha.task.templates

import java.util.*

object TemplateUtils {
    fun toBase64Image(dataInBase64: String, imageFormat: String): String {
        val prefix = "data:image/$imageFormat;base64,"
        return prefix + dataInBase64
    }

    fun toBase64String(bytes: ByteArray): String {
        return String(Base64.getEncoder().encode(bytes))
    }
}
