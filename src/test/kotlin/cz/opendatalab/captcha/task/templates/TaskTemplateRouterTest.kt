package cz.opendatalab.captcha.task.templates

import cz.opendatalab.captcha.verification.AnswerSheet
import cz.opendatalab.captcha.verification.AnswerType
import cz.opendatalab.captcha.verification.EmptyDisplayData
import cz.opendatalab.captcha.verification.TextAnswer
import cz.opendatalab.captcha.verification.entities.TextData
import org.junit.jupiter.api.Test

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
internal class TaskTemplateRouterTest(@Autowired val router: TaskTemplateRouter) {

    @Test
    fun `generate task positive`() {
        val taskType = "NUMERIC_EQUATION"
        val (description, data, answerSheet) = router.generateTask(taskType, "someUser", EmptyGenerationConfig)

        assertThat(description.text).startsWith("What is ")
        assertThat(data).isInstanceOf(TextData::class.java)
        assertThat(answerSheet).isEqualTo(AnswerSheet(EmptyDisplayData, AnswerType.Text))
    }

    @Test
    fun `generate task taskType not found`() {
        val taskType = "UNKNOWN_TASK_TYPE_RANDOMSTRING"

        assertThatExceptionOfType(ResponseStatusException::class.java).isThrownBy {
            router.generateTask(taskType, "someUser", EmptyGenerationConfig)
        }
    }

    @Test
    fun evaluateTask() {
        val taskType = "NUMERIC_EQUATION"
        val (description, data, answerSheet) = router.generateTask(taskType, "someUser", EmptyGenerationConfig)

        assertThat(description.text).startsWith("What is ")
        assertThat(data).isInstanceOf(TextData::class.java)
        assertThat(answerSheet).isEqualTo(AnswerSheet(EmptyDisplayData, AnswerType.Text))

        val answer = (data as TextData).text

        val result = router.evaluateTask(taskType, data, TextAnswer(answer))
        assertThat(result.evaluation).isEqualTo(1.0)
    }

    @Test
    fun getTaskTypes() {
        val taskTypes = router.getTaskTypes()
        assertThat(taskTypes.size).isEqualTo(3)
    }
}
