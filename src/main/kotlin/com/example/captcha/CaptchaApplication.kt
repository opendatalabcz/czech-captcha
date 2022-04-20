package com.example.captcha

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CaptchaApplication

fun main(args: Array<String>) {
	runApplication<CaptchaApplication>(*args)
}
