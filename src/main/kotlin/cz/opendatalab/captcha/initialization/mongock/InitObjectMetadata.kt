package cz.opendatalab.captcha.initialization.mongock

import cz.opendatalab.captcha.datamanagement.objectmetadata.*
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution


@ChangeUnit(id="objectmetadata-initializer", order = "0004", author = "jb")
class InitObjectMetadata(val repo: ObjectMetadataRepository) {
    @Execution
    fun changeSet() {
        repo.insert(ObjectMetadata("bfa25bc2-7350-4b99-99cc-8feaa8e1991b", "user", "jpg"))
    }

    @RollbackExecution
    fun rollback() {
        repo.deleteAll()
    }
}
