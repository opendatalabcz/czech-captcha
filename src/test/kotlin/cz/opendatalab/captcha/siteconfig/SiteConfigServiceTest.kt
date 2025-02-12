package cz.opendatalab.captcha.siteconfig

import com.fasterxml.jackson.databind.ObjectMapper
import cz.opendatalab.captcha.SpringBootTestWithoutMongoDB
import cz.opendatalab.captcha.task.templates.EmptyGenerationConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.server.ResponseStatusException

@SpringBootTestWithoutMongoDB
internal class SiteConfigServiceTest(
    @Autowired private val siteConfigService: SiteConfigService,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val siteConfigRepo: SiteConfigRepository
) {

    @Test
    fun getSiteConfigsForUser() {
        val user = "user"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val configs = listOf(SiteConfig("site1", "sec2", user, "config1", taskConfig),
            SiteConfig("site1", "sec2", user, "config1", taskConfig))

        `when`(siteConfigRepo.getByUserName(user)).thenReturn(configs)

        val result = siteConfigService.getSiteConfigsForUser(user)

        assertThat(result).isEqualTo(configs)
    }

    @Test
    fun secretKeyToSiteKey() {
        val secretKey = "secret"
        val siteKey = "site1"
        val user = "user"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey, secretKey, user, "config1", taskConfig)

        `when`(siteConfigRepo.getBySecretKey(secretKey)).thenReturn(siteConfig)

        val result = siteConfigService.secretKeyToSiteKey(secretKey)

        assertThat(result).isEqualTo(siteKey)
    }

    @Test
    fun create() {
        val secretKey = "secret"
        val siteKey = "site1"
        val user = "user"
        val configName = "configName"
        val taskType = "Text"
        val evaluationThreshold = 1.0
        val taskConfigDTO = TaskConfigDTO(taskType, objectMapper.createObjectNode(), evaluationThreshold)
        // expected
        val taskConfig = TaskConfig(taskType, EmptyGenerationConfig, evaluationThreshold)
        val siteConfig = SiteConfig(siteKey, secretKey, user, configName, taskConfig)

        `when`(siteConfigRepo.insert(any(SiteConfig::class.java))).thenReturn(siteConfig)

        val result = siteConfigService.create(user, configName, taskConfigDTO)

        assertThat(result).isEqualTo(siteConfig)
    }

    @Test
    fun deleteConfig() {
        val siteKey = "site1"
        val user = "user"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey, "sec1", user, "config1", taskConfig)

        `when`(siteConfigRepo.getBySiteKey(siteKey)).thenReturn(siteConfig)

        siteConfigService.deleteConfig(user, siteKey)

        verify(siteConfigRepo).deleteBySiteKey(siteKey)
    }

    @Test
    fun `deleteConfig failed`() {
        val siteKey = "site1"
        val user = "user"
        val otherUser = "otheruser"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey, "sec1", otherUser, "config1", taskConfig)

        `when`(siteConfigRepo.getBySiteKey(siteKey)).thenReturn(siteConfig)

        assertThrows<ResponseStatusException> {
            siteConfigService.deleteConfig(user, siteKey)

        }
    }

}
