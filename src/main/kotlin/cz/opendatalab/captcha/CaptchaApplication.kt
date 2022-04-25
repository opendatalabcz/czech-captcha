package cz.opendatalab.captcha

import io.mongock.runner.springboot.EnableMongock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableMongock
class CaptchaApplication

fun main(args: Array<String>) {
	runApplication<CaptchaApplication>(*args)
}
