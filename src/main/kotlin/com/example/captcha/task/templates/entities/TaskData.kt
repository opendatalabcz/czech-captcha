package com.example.captcha.verification.entities

import com.example.captcha.datamanagement.objectmetadata.Label

interface TaskData {
}

data class TextData(val text: String): TaskData

// file id,
data class ObjectsWithLabels(val label: Label, val labelGroup: String, val expectedResults: List<Pair<String, ExpectedResult>>): TaskData


enum class ExpectedResult {
    CORRECT, INCORRECT, UNKNOWN
}
