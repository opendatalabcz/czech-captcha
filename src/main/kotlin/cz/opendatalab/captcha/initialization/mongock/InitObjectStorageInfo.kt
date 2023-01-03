package cz.opendatalab.captcha.initialization.mongock

import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectRepositoryType
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfoRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution


@ChangeUnit(id="objectstorageinfo-initializer", order = "0005", author = "jb")
class InitObjectStorageInfo(val repo: ObjectStorageInfoRepository) {
    @Execution
    fun changeSet() {
        repo.insert(ObjectStorageInfo(
            "bfa25bc2-7350-4b99-99cc-8feaa8e1991b",
            "677743874_168dcb5ed1_z.jpg",
            "https://c5.staticflickr.com/2/1244/677743874_168dcb5ed1_z.jpg",
            ObjectRepositoryType.URL
        ))
    }

    @RollbackExecution
    fun rollback() {
        repo.deleteAll()
    }
}
