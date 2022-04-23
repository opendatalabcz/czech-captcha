package com.example.captcha.datamanagement.objectmetadata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LabelingTest {

    @Test
    fun `Simple positive labeling`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(true, label, 10, 10)
            .recordLabel(true, label, 10, 10)
            .recordLabel(true, label, 10, 10)

        assertThat(result.labels.size).isEqualTo(1)
        assertThat(result.negativeLabels.size).isEqualTo(0)
    }

    @Test
    fun `Simple positive labeling ++-+ label not registered`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(true, label, 10, 10)
            .recordLabel(true, label, 10, 10)
            .recordLabel(false, label, 10, 10)
            .recordLabel(true, label, 10, 10)


        assertThat(result.labels.size).isEqualTo(0)
        assertThat(result.negativeLabels.size).isEqualTo(0)
    }

    @Test
    fun `Simple positive labeling ++-+0 label registered`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(true, label, 10, 10)
            .recordLabel(true, label, 10, 10)
            .recordLabel(false, label, 10, 10)
            .recordLabel(true, label, 10, 10)
            .recordLabel(true, label, 10, 10)

        assertThat(result.labels.size).isEqualTo(1)
        assertThat(result.negativeLabels.size).isEqualTo(0)
    }

    @Test
    fun `Simple positive labeling +---- label registered`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(true, label, 10, 10)
            .recordLabel(false, label, 10, 10)
            .recordLabel(false, label, 10, 10)
            .recordLabel(false, label, 10, 10)
            .recordLabel(false, label, 10, 10)

        assertThat(result.labels.size).isEqualTo(0)
        assertThat(result.negativeLabels.size).isEqualTo(1)
    }

    @Test
    fun `Simple negative labeling`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(false, label, 10, 10)
            .recordLabel(false, label, 10, 10)
            .recordLabel(false, label, 10, 10)

        assertThat(result.labels.size).isEqualTo(0)
        assertThat(result.negativeLabels.size).isEqualTo(1)
    }

    @Test
    fun `Is labeled max cardinality is reached`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(true, label, 1, 10)
            .recordLabel(true, label, 1, 10)
            .recordLabel(true, label, 1, 10)

        assertThat(result.isLabeled).isTrue
    }

    @Test
    fun `Is labeled max cardinality is not reach through negative label`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(false, label, 1, 10)
            .recordLabel(false, label, 1, 10)
            .recordLabel(false, label, 1, 10)

        assertThat(result.isLabeled).isFalse
    }

    @Test
    fun `Is labeled labelRange size is reached with positive label`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(true, label, 10, 1)
            .recordLabel(true, label, 10, 1)
            .recordLabel(true, label, 10, 1)

        assertThat(result.isLabeled).isTrue
    }

    @Test
    fun `Is labeled labelRange size is reached with negative label`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(false, label, 10, 1)
            .recordLabel(false, label, 10, 1)
            .recordLabel(false, label, 10, 1)

        assertThat(result.isLabeled).isTrue
    }

    @Test
    fun `Is labeled labelRange size is not reached`() {
        val labeling = Labeling()
        val label = "labelValue"

        val result = labeling.recordLabel(true, label, 10, 2)
            .recordLabel(true, label, 10, 2)
            .recordLabel(true, label, 10, 2)

        assertThat(result.isLabeled).isFalse
    }
}
