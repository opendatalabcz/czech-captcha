package cz.opendatalab.captcha.verification.dto

import cz.opendatalab.captcha.verification.Answer

data class AnswerDTO(val taskId: String, val data: Answer) {
}
