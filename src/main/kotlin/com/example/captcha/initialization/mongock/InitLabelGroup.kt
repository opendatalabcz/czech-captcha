package com.example.captcha.initialization.mongock

import com.example.captcha.datamanagement.objectmetadata.LabelGroupLimited
import com.example.captcha.datamanagement.objectmetadata.LabelGroupRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution


@ChangeUnit(id="labelgroup-initializer", order = "0002", author = "ov")
class InitLabelGroup(val labelGroupRepository: LabelGroupRepository) {
    @Execution
    fun changeSet() {
        labelGroupRepository.insert(LabelGroupLimited("animals", listOf("cat", "dog"), 1))
    }

    @RollbackExecution
    fun rollback() {
        labelGroupRepository.deleteByName("animals")
    }
}
