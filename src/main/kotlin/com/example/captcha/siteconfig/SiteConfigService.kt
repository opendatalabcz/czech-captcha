package com.example.captcha.siteconfig

import com.example.captcha.Utils
import com.example.captcha.task.taskconfig.TaskConfigService
import net.pwall.json.schema.JSONSchema
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class SiteConfigService(val siteConfigRepo: SiteConfigRepository,
                        val taskConfigService: TaskConfigService) {
    fun getTaskConfig(siteKey: String): Pair<TaskConfig, String> {
        val config = siteConfigRepo.getBySiteKey(siteKey) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid sitekey $siteKey")
        return Pair(config.taskConfig, config.userName)
    }

    fun getSiteConfigsForUser(username: String): List<SiteConfig> {
        return siteConfigRepo.getByUsername(username)
    }

    fun secretKeyToSiteKey(secreteKey: String): String? {
        val siteConfig = siteConfigRepo.getBySecretKey(secreteKey)
        return siteConfig?.siteKey
    }

    fun create(userName: String, taskConfig: TaskConfig): SiteConfig {
        validateTaskConfig(taskConfig)

        val siteKey = Utils.generateUniqueId()
        val secreteKey = Utils.generateUniqueId()

        return siteConfigRepo.add(SiteConfig(siteKey, secreteKey, userName, taskConfig))
    }


    private fun validateTaskConfig(taskConfig: TaskConfig) {
        val schema = taskConfigService.getTaskConfigSchema(taskConfig.taskType.name)

        val validator = JSONSchema.parse(schema.toString())

        val valid = validator.validate(taskConfig.generationConfig.toString())
        if (!valid) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Generation configuration does not conform to schema")
    }
}
