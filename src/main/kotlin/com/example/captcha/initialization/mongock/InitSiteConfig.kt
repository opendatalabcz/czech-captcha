package com.example.captcha.initialization.mongock

import com.example.captcha.siteconfig.SiteConfig
import com.example.captcha.siteconfig.SiteConfigRepository
import com.example.captcha.siteconfig.TaskConfig
import com.example.captcha.task.templates.EmptyGenerationConfig
import com.example.captcha.task.templates.ImageLabelingGenerationConfig
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution


@ChangeUnit(id="siteconfig-initializer", order = "0001", author = "ov")
class InitSiteConfig(val siteConfigRepo: SiteConfigRepository) {
    @Execution
    fun changeSet() {
        val toStore = mutableListOf(
            SiteConfig("siteKey", "secretKey", "user", TaskConfig(
                "NUMERIC_EQUATION",
                EmptyGenerationConfig,
                0.8
            )),
            SiteConfig("siteKey3", "secretKey3", "user", TaskConfig(
                "IMAGE_LABELING",
                ImageLabelingGenerationConfig("animals", emptyList(), emptyList()),
                0.95
            )),
            SiteConfig("siteKey4", "secretKey4", "user", TaskConfig(
                "TEXT",
                EmptyGenerationConfig,
                0.8
            ))
        )

        siteConfigRepo.insert(toStore)
    }

    @RollbackExecution
    fun rollback() {
        siteConfigRepo.deleteAll()
    }
}
