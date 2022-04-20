package com.example.captcha.datamanagement.dto

import com.example.captcha.datamanagement.objectmetadata.Label
import com.example.captcha.datamanagement.objectmetadata.LabelGroup
import com.example.captcha.datamanagement.objectmetadata.LabelGroupLimited

data class LabelGroupCreateDTO(val name: String, val labels: List<String> = emptyList(), val maxCardinality: Int) {
    fun toLimitedLabelGroup(): LabelGroup {
        return LabelGroupLimited(name, labels.map { Label(it) }, maxCardinality)
    }

    fun toUnlimitedLabelGroup(): LabelGroup {
        return LabelGroup(name, maxCardinality)
    }
}
