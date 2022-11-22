package cz.opendatalab.captcha.task.templates.texttemplate

import cz.opendatalab.captcha.task.templates.EmptyGenerationConfig
import cz.opendatalab.captcha.verification.entities.AnswerType
import cz.opendatalab.captcha.verification.entities.ImageDisplayData
import cz.opendatalab.captcha.verification.entities.TextAnswer
import cz.opendatalab.captcha.verification.entities.TextData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class TextTemplateTest {

    @Test
    fun `overall test`() {
        val (description, data, answerSheet) = TextTemplate.generateTask(EmptyGenerationConfig, "someUser")

        Assertions.assertThat(description.text).startsWith("Type the text")
        Assertions.assertThat(data).isInstanceOf(TextData::class.java)
        Assertions.assertThat(answerSheet.answerType).isEqualTo(AnswerType.Text)
        Assertions.assertThat(answerSheet.displayData).isInstanceOf(ImageDisplayData::class.java)

        val answer = (data as TextData).text

        val result = TextTemplate.evaluateTask(data, TextAnswer(answer))
        Assertions.assertThat(result.evaluation).isEqualTo(1.0)
    }
}
