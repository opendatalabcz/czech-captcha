package cz.opendatalab.captcha.task.templates.simplequationtemplate

import cz.opendatalab.captcha.task.templates.GenerationConfig
import cz.opendatalab.captcha.verification.*
import cz.opendatalab.captcha.verification.entities.*
import cz.opendatalab.captcha.task.templates.TaskTemplate
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component("NUMERIC_EQUATION")
object SimpleEquationTemplate: TaskTemplate {
    override fun generateTask(generationConfig: GenerationConfig, currentUser: String): Triple<Description, TaskData, AnswerSheet> {
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
        return if (correctAnswer == textAnswer) EvaluationResult(1.0) else EvaluationResult(0.0)
    }

    private fun generateDigit(): Int {
        return Random.nextInt() % 10
    }
}
