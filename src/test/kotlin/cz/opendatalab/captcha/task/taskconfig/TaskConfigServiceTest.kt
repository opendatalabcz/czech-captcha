package cz.opendatalab.captcha.task.taskconfig

import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroupRepository
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadataRepository
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectCatalogue
import cz.opendatalab.captcha.siteconfig.SiteConfigRepository
import cz.opendatalab.captcha.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
internal class TaskConfigServiceTest(@Autowired val taskConfigService: TaskConfigService,
                                     @Autowired @MockBean val userRepository: UserRepository,
                                     @Autowired @MockBean val catalog: ObjectCatalogue,
                                     @Autowired @MockBean val objectMetadataRepo: ObjectMetadataRepository,
                                     @Autowired @MockBean val siteConfigRepo: SiteConfigRepository,
                                     @Autowired @MockBean val labelGroupRepo: LabelGroupRepository
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
