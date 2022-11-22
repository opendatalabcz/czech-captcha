package cz.opendatalab.captcha.datamanagement.objectmetadata

import org.springframework.data.mongodb.core.mapping.Document

@Document("labelgroup")
open class LabelGroup(val name: String,
                      val maxCardinality: Int) {

    open fun rangeContainsLabel(label: String): Boolean = true
    open fun rangeSize(): Int = Int.MAX_VALUE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LabelGroup

        if (name != other.name) return false
        if (maxCardinality != other.maxCardinality) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + maxCardinality
        return result
    }
}

// contains (0..n) vs 1 labels, where n is labels size
class LabelGroupLimited(name: String,
                        val labelRange: Set<String>,
                        maxCardinality: Int): LabelGroup(name, maxCardinality) {
    override fun rangeContainsLabel(label: String): Boolean {
        return labelRange.contains(label)
    }

    override fun rangeSize(): Int {
        return labelRange.size
    }
}


data class Labeling(val isLabeled: Boolean,
                    val labels: Set<String>,
                    val negativeLabels: Set<String>,
                    val labelStatistics: LabelStatistics) {
    constructor(): this(false, emptySet(), emptySet(), LabelStatistics())
    constructor(labels: Set<String>): this(true, labels, emptySet(), LabelStatistics())
    constructor(statistics: LabelStatistics): this(false, emptySet(), emptySet(), statistics)

    fun recordLabel(positive: Boolean, label: String, maxCardinality: Int, labelRangeSize: Int): Labeling {
        require(!isLabeled)
        require(!(labels.contains(label) || negativeLabels.contains(label)))

        return when(labelStatistics.recordLabel(label, positive)) {
            LabelingResult.LABELED_POSITIVE, LabelingResult.LABELED_NEGATIVE ->  addLabel(positive, label, maxCardinality, labelRangeSize)
            LabelingResult.UNDECIDABLE -> this // TODO what should happen?
            else -> this
        }
    }

    private fun addLabel(positive: Boolean, label: String, maxCardinality: Int, labelRangeSize: Int): Labeling {
        val finishedLabeling = (positive && (labels.size + 1 == maxCardinality)) ||
                labels.size + negativeLabels.size + 1 == labelRangeSize
        return if (finishedLabeling) {
            if (positive) {
                Labeling(true, labels + label, emptySet(), labelStatistics)
            } else {
                Labeling(true, labels, emptySet(), labelStatistics)
            }
        } else {
            if (positive) {
                this.copy(labels = labels + label)
            } else {
                this.copy(negativeLabels = negativeLabels + label)
            }
        }
    }
}

data class LabelStatistics(val statistics: MutableMap<String, LabelStatistic>) {
    constructor() : this(mutableMapOf())
    fun recordLabel(label: String, positive: Boolean): LabelingResult {
        val result = statistics[label]?.recordLabel(positive)
            ?: run {
                val statistic = LabelStatistic()
                val result = statistic.recordLabel(positive)
                statistics[label] = statistic
                result
            }
        return result
    }
}

data class LabelStatistic(var value: Int, var count: Int) {
    companion object {
        const val FALSE_ATTEMPTS_ALLOWED = 3
        const val DOMINANCE = 3
    }
    constructor() : this(0, 0)
    fun recordLabel(positive: Boolean): LabelingResult {
        value += if (positive) +1 else -1
        count++
        return getLabelingResult()
    }
    private fun labelDetermined(): Boolean {
        return Math.abs(value) >= DOMINANCE
    }
    private fun isUndecidable(): Boolean {
        return count >= FALSE_ATTEMPTS_ALLOWED * 2 + DOMINANCE
    }

    private fun labeledResult(): LabelingResult {
        return if (value > 0) LabelingResult.LABELED_POSITIVE
        else LabelingResult.LABELED_NEGATIVE
    }

    private fun unLabeledResult(): LabelingResult {
        return if (isUndecidable()) LabelingResult.UNDECIDABLE
        else LabelingResult.UNDECIDED
    }

    private fun getLabelingResult(): LabelingResult {
        return if (labelDetermined()) labeledResult()
        else unLabeledResult()
    }
}

enum class LabelingResult {
    UNDECIDED, LABELED_POSITIVE, LABELED_NEGATIVE, UNDECIDABLE
}
