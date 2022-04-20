package com.example.captcha.siteconfig

import com.example.captcha.verification.entities.TaskType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class SiteConfigInMemoryRepo(val objectMapper: ObjectMapper) : SiteConfigRepository {
    @EventListener(ApplicationReadyEvent::class)
    fun initializeAfterStartup() {
        val generationConfig = objectMapper.readTree("""{"labelGroup":"animals","tags":[]}""")
        val verificationConfig = TaskConfig(TaskType("IMAGE_LABELING"), generationConfig, 0.75)
        val imageLabelingConfig = SiteConfig("siteKey3", "secretKey3", "user1", verificationConfig)

        add(imageLabelingConfig)
    }

    val repo = mutableListOf(
        SiteConfig("siteKey", "secretKey", "user1", TaskConfig(
            TaskType("NUMERIC_EQUATION"),
            ObjectNode(JsonNodeFactory.instance),
            0.75
        )),
        SiteConfig("siteKey2", "secretKey2", "user1", TaskConfig(
            TaskType("SIMPLE_IMAGE"),
            ObjectNode(JsonNodeFactory.instance),
            0.75
        )),
//        SiteConfig("siteKey3", "secretKey3", "username", VerificationConfig(TaskType("IMAGE_LABELING"), ObjectNode(JsonNodeFactory.instance))),
        SiteConfig("siteKey4", "secretKey4", "user1", TaskConfig(
            TaskType("TEXT"),
            ObjectNode(JsonNodeFactory.instance),
            0.75
        ))
    )

    override fun getAll(): List<SiteConfig> {
        return repo
    }

    override fun getBySiteKey(siteKey: String): SiteConfig? {
        return repo.find { siteConfig -> siteConfig.siteKey == siteKey }
    }

    override fun getBySecretKey(secretKey: String): SiteConfig? {
        return repo.find { siteConfig -> siteConfig.secretKey == secretKey }
    }

    override fun getByUsername(username: String): List<SiteConfig> {
        return repo.filter { siteConfig -> siteConfig.userName == username }
    }

    override fun add(siteConfig: SiteConfig): SiteConfig {
        repo.add(siteConfig)
        return siteConfig
    }
}
