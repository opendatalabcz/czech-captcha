package com.example.captcha.siteconfig

import com.example.captcha.task.templates.EmptyGenerationConfig
import com.example.captcha.task.templates.ImageLabelingGenerationConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

//@Component
//class SiteConfigInMemoryRepo(val objectMapper: ObjectMapper) : SiteConfigRepository {
//    @EventListener(ApplicationReadyEvent::class)
//    fun initializeAfterStartup() {
////        val generationConfig = objectMapper.readTree("""{"labelGroup":"animals","tags":[]}""")
//        val generationConfig = ImageLabelingGenerationConfig("animals", emptyList(), emptyList())
//        val verificationConfig = TaskConfig("IMAGE_LABELING", generationConfig, 0.75)
//        val imageLabelingConfig = SiteConfig("siteKey3", "secretKey3", "user1", verificationConfig)
//
//        add(imageLabelingConfig)
//    }
//
//    val repo = mutableListOf(
//        SiteConfig("siteKey", "secretKey", "user1", TaskConfig(
//            "NUMERIC_EQUATION",
//            EmptyGenerationConfig,
//            0.75
//        )),
//        SiteConfig("siteKey2", "secretKey2", "user1", TaskConfig(
//            "SIMPLE_IMAGE",
//            EmptyGenerationConfig,
//            0.75
//        )),
////        SiteConfig("siteKey3", "secretKey3", "username", VerificationConfig(TaskType("IMAGE_LABELING"), ObjectNode(JsonNodeFactory.instance))),
//        SiteConfig("siteKey4", "secretKey4", "user1", TaskConfig(
//            "TEXT",
//            EmptyGenerationConfig,
//            0.75
//        ))
//    )
//
//    override fun getAll(): List<SiteConfig> {
//        return repo
//    }
//
//    override fun getBySiteKey(siteKey: String): SiteConfig? {
//        return repo.find { siteConfig -> siteConfig.siteKey == siteKey }
//    }
//
//    override fun getBySecretKey(secretKey: String): SiteConfig? {
//        return repo.find { siteConfig -> siteConfig.secretKey == secretKey }
//    }
//
//    override fun getByUsername(username: String): List<SiteConfig> {
//        return repo.filter { siteConfig -> siteConfig.userName == username }
//    }
//
//    override fun add(siteConfig: SiteConfig): SiteConfig {
//        repo.add(siteConfig)
//        return siteConfig
//    }
//}
