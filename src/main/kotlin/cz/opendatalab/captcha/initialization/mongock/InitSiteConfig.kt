package cz.opendatalab.captcha.initialization.mongock

import cz.opendatalab.captcha.siteconfig.SiteConfig
import cz.opendatalab.captcha.siteconfig.SiteConfigRepository
import cz.opendatalab.captcha.siteconfig.TaskConfig
import cz.opendatalab.captcha.task.templates.EmptyGenerationConfig
import cz.opendatalab.captcha.task.templates.ImageLabelingGenerationConfig
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution


@ChangeUnit(id="siteconfig-initializer", order = "0003", author = "ov")
class InitSiteConfig(val repo: SiteConfigRepository) {
    @Execution
    fun changeSet() {
        val toStore = mutableListOf(
            SiteConfig("c830a9f1-dc3a-4c70-bcc0-40356eb75c4c", "2b017cdd-767d-4764-9b63-89ab12cc8da5", "user", "site1 equations",
                TaskConfig(
                "Equation",
                EmptyGenerationConfig,
                0.8
            )),
            SiteConfig("26249119-fc74-41f1-a947-9380960a7eb9", "e9b5abe0-a5b0-4ad2-8606-c2815a1cd70b", "user", "site2 image labeling",
                TaskConfig(
                "Image Labeling",
                ImageLabelingGenerationConfig("all", emptySet(), emptySet()),
                0.95
            )),
            SiteConfig("13a44d25-1193-4974-ad2a-82eb735bb6f2", "b39d0dde-9596-4ab4-ae08-d0826f180c72", "user", "site3 text",
                TaskConfig(
                "Text",
                EmptyGenerationConfig,
                0.8
            ))
        )

        repo.insert(toStore)
    }

    @RollbackExecution
    fun rollback() {
        repo.deleteAll()
    }
}
