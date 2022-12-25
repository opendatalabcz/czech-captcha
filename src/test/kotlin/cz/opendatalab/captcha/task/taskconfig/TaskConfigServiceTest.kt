package cz.opendatalab.captcha.task.taskconfig

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
internal class TaskConfigServiceTest(
    @Autowired val taskConfigService: TaskConfigService
) {

    @Test
    fun `getTaskConfigSchema default`() {
        val taskName = "TASK_WITHOUT_SCHEMA"

        val expectedTitle = "Empty Generation configuration"

        val schema = taskConfigService.getTaskConfigSchema(taskName)

        assertThat(schema.hasNonNull("title"))
        assertThat(schema["title"].asText()).isEqualTo(expectedTitle)
    }

    @Test
    fun `getTaskConfigSchema concrete schema`() {
        val taskName = "TEST_TASK"

        val expectedTitle = "Test Generation configuration"

        val schema = taskConfigService.getTaskConfigSchema(taskName)

        assertThat(schema.hasNonNull("title"))
        assertThat(schema["title"].asText()).isEqualTo(expectedTitle)
    }

    @Test
    fun `getTaskNames not empty`() {
        assertThat(taskConfigService.getTaskNames()).isNotEmpty
    }
}
