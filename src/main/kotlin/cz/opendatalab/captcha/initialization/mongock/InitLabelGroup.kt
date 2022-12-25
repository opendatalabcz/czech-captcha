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
class InitLabelGroup(val labelGroupRepository: LabelGroupRepository, val objectDetector: ObjectDetector) {
    @Execution
    fun changeSet() {
        labelGroupRepository.insert(LabelGroupLimited("animals", setOf("cat", "dog"), 1))
        labelGroupRepository.insert(LabelGroupLimited(ObjectDetectionConstants.LABEL_GROUP, objectDetector.getSupportedLabels(), objectDetector.getSupportedLabels().size))
        labelGroupRepository.insert(LabelGroup("all", Int.MAX_VALUE))
    }

    @RollbackExecution
    fun rollback() {
        labelGroupRepository.deleteByName("animals")
        labelGroupRepository.deleteByName(ObjectDetectionConstants.LABEL_GROUP)
        labelGroupRepository.deleteByName("all")
    }
}
