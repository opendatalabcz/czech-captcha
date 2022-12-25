package cz.opendatalab.captcha.task.templates

import cz.opendatalab.captcha.verification.entities.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@ActiveProfiles("test")
internal class TaskTemplateRouterTest(
    @Autowired val router: TaskTemplateRouter
) {

    @Test
    fun `generate task positive`() {
        val taskType = "Equation"
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
        val taskType = "Equation"
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
        assertThat(taskTypes.size).isEqualTo(4)
    }
}
