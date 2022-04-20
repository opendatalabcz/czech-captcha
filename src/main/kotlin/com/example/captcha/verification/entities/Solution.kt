package com.example.captcha.verification.entities

interface Solution {
}

data class TextSolution(val text: String) : Solution
