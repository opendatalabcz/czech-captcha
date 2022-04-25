package cz.opendatalab.captcha.datamanagement.dto

import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroup
import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroupLimited

data class LabelGroupCreateDTO(val name: String, val labels: List<String> = emptyList(), val maxCardinality: Int) {
    fun toLimitedLabelGroup(): LabelGroup {
        return LabelGroupLimited(name, labels, maxCardinality)
    }

    fun toUnlimitedLabelGroup(): LabelGroup {
        return LabelGroup(name, maxCardinality)
    }
}
