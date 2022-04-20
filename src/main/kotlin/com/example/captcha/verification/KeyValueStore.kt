package com.example.captcha.verification

import com.example.captcha.verification.entities.Task
import com.example.captcha.verification.entities.Token
import org.springframework.stereotype.Service

interface KeyValueStore<T> {
    fun popById(id: String): T?

    fun insertValue(key: String, value: T)
}

open class InMemoryKeyValueStore<T>(private val memory: MutableMap<String, T>): KeyValueStore<T> {

    override fun popById(id: String): T? {
        return memory.remove(id)
    }

    override fun insertValue(key: String, value: T) {
        memory[key] = value
    }
}

@Service
class TokenKeyValueStore: InMemoryKeyValueStore<Token>(mutableMapOf())

@Service
class TaskKeyValueStore: InMemoryKeyValueStore<Task>(mutableMapOf())
