package com.example.captcha.task.templates.simplequationtemplate

import com.example.captcha.verification.*
import com.example.captcha.verification.entities.*
import com.example.captcha.task.templates.TaskTemplate
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component("NUMERIC_EQUATION")
object SimpleEquationTemplate: TaskTemplate {
    override fun generateTask(generationConfig: JsonNode, userName: String): Triple<Description, TaskData, AnswerSheet> {
        // todo generate equation...
        val firstDigit = generateDigit()
        val secondDigit = generateDigit()
        val description = Description("What is $firstDigit + $secondDigit?")
        val data = TextData((firstDigit+secondDigit).toString())
        val answerSheet = AnswerSheet(EmptyDisplayData, AnswerType.Text)

        return Triple(description, data, answerSheet)
    }

    override fun evaluateTask(taskData: TaskData, answer: Answer): EvaluationResult {
        val textAnswer = (answer as TextAnswer).text
        val correctAnswer = (taskData as TextData).text
        return if (correctAnswer == textAnswer) EvaluationResult(1F) else EvaluationResult(0F)
    }

    private fun generateDigit(): Int {
        return Random.nextInt() % 10
    }
}
