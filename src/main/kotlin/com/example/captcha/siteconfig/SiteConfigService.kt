package com.example.captcha.siteconfig

import com.example.captcha.Utils
import com.example.captcha.task.taskconfig.TaskConfigService
import com.example.captcha.task.templates.ATaskType
import com.example.captcha.task.templates.EmptyGenerationConfig
import com.example.captcha.task.templates.GenerationConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import net.pwall.json.schema.JSONSchema
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Service
class SiteConfigService(val siteConfigRepo: SiteConfigRepository,
                        val taskConfigService: TaskConfigService,
                        val objectMapper: ObjectMapper,
                        ) {
    fun getTaskConfig(siteKey: String): Pair<TaskConfig, String> {
        val config = siteConfigRepo.getBySiteKey(siteKey) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid sitekey $siteKey")
        return Pair(config.taskConfig, config.userName)
    }

    fun getSiteConfigsForUser(username: String): List<SiteConfig> {
        return siteConfigRepo.getByUserName(username)
    }

    fun secretKeyToSiteKey(secreteKey: String): String? {
        val siteConfig = siteConfigRepo.getBySecretKey(secreteKey)
        return siteConfig?.siteKey
    }

    fun create(userName: String, taskConfigDTO: TaskConfigDTO): SiteConfig {
        validateTaskConfig(taskConfigDTO)
        val taskConfig = fromTaskConfigDTO(taskConfigDTO)

        val siteKey = Utils.generateUniqueId()
        val secreteKey = Utils.generateUniqueId()

        return siteConfigRepo.insert(SiteConfig(siteKey, secreteKey, userName, taskConfig))
    }

    fun deleteConfig(username: String, siteKey: String) {
        val canBeDeleted = siteConfigRepo.getBySiteKey(siteKey)?.let { it.userName == username } ?: false

        if (!canBeDeleted) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Sitekey $siteKey not found")
        }

        siteConfigRepo.deleteBySiteKey(siteKey)
    }

    private fun fromTaskConfigDTO(taskConfigDTO: TaskConfigDTO): TaskConfig {
        val generationConfig = fromGenerationConfigJSON(taskConfigDTO.generationConfig, taskConfigDTO.taskType)

        return TaskConfig(taskConfigDTO.taskType, generationConfig, taskConfigDTO.evaluationThreshold)
    }

    private fun fromGenerationConfigJSON(json: JsonNode, taskType: String): GenerationConfig {
        // Get GenerationConfig data class for the task type
        val generationConfigClass = GenerationConfig::class.sealedSubclasses
            .filter { it.hasAnnotation<ATaskType>() }
            .find { it.findAnnotation<ATaskType>()?.name == taskType }

        return generationConfigClass?.let { genConfigClass -> objectMapper.treeToValue(json, genConfigClass.java) }
            ?: EmptyGenerationConfig
    }

    private fun validateTaskConfig(taskConfig: TaskConfigDTO) {
        val schema = taskConfigService.getTaskConfigSchema(taskConfig.taskType)

        val validator = JSONSchema.parse(schema.toString())

        val valid = validator.validate(taskConfig.generationConfig.toString())
        if (!valid) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Generation configuration does not conform to schema")
    }
}
