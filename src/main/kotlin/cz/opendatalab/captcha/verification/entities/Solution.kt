package cz.opendatalab.captcha.verification.entities

interface Solution {
}

data class TextSolution(val text: String) : Solution
