package cz.opendatalab.captcha.initialization.mongock

import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroupLimited
import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroupRepository
import cz.opendatalab.captcha.datamanagement.objectdetection.ObjectDetectionConstants
import cz.opendatalab.captcha.datamanagement.objectdetection.ObjectDetector
import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroup
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution


@ChangeUnit(id="labelgroup-initializer", order = "0002", author = "ov")
class InitLabelGroup(val repo: LabelGroupRepository, val objectDetector: ObjectDetector) {
    @Execution
    fun changeSet() {
        repo.insert(LabelGroupLimited(ObjectDetectionConstants.LABEL_GROUP, objectDetector.getSupportedLabels(), objectDetector.getSupportedLabels().size))
        repo.insert(LabelGroup("all", Int.MAX_VALUE))
    }

    @RollbackExecution
    fun rollback() {
        repo.deleteAll()
    }
}
