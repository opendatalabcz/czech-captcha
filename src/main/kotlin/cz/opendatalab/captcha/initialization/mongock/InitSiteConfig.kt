package cz.opendatalab.captcha.initialization.mongock

import cz.opendatalab.captcha.siteconfig.SiteConfig
import cz.opendatalab.captcha.siteconfig.SiteConfigRepository
import cz.opendatalab.captcha.siteconfig.TaskConfig
import cz.opendatalab.captcha.task.templates.EmptyGenerationConfig
import cz.opendatalab.captcha.task.templates.ImageLabelingGenerationConfig
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution


@ChangeUnit(id="siteconfig-initializer", order = "0001", author = "ov")
class InitSiteConfig(val siteConfigRepo: SiteConfigRepository) {
    @Execution
    fun changeSet() {
        val toStore = mutableListOf(
            SiteConfig("siteKey", "secretKey", "user", "site1 equations",
                TaskConfig(
                "NUMERIC_EQUATION",
                EmptyGenerationConfig,
                0.8
            )),
            SiteConfig("siteKey3", "secretKey3", "user", "site3 dog/cat",
                TaskConfig(
                "IMAGE_LABELING",
                ImageLabelingGenerationConfig("animals", emptyList(), emptyList()),
                0.95
            )),
            SiteConfig("siteKey4", "secretKey4", "user", "site4 text",
                TaskConfig(
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
