package com.example.captcha

import java.util.*

object Utils {
    fun generateUniqueId(): String {
        return UUID.randomUUID().toString()
    }

    fun <T> selectRandom(list: List<T>, count: Int): List<T> {
        return list.shuffled().take(count)
    }
}
