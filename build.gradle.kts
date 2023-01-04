import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.3"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.10"
	kotlin("plugin.spring") version "1.6.10"
}

group = "cz.opendatalab.captcha"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("net.pwall.json:json-kotlin-schema:0.32")
	implementation("io.mongock:mongock:5.0.32")
	implementation("io.mongock:mongock-springboot:5.0.32")
	implementation("io.mongock:mongodb-springdata-v3-driver:5.0.32")
	implementation("org.springframework:spring-test:5.3.15")
	implementation("commons-io:commons-io:2.11.0")
	implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
	implementation("ch.qos.logback:logback-classic:1.2.6")

	implementation("org.imgscalr:imgscalr-lib:4.2")

	implementation("org.jetbrains.kotlinx:kotlin-deeplearning-onnx:0.4.0")
	implementation("org.jetbrains.kotlinx:kotlin-deeplearning-api:0.4.0")

	implementation("org.springdoc:springdoc-openapi-ui:1.6.5")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:mongodb:1.17.6")
	testImplementation("org.testcontainers:junit-jupiter:1.17.6")
	testImplementation("com.ninja-squad:springmockk:3.1.1")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	if (project.hasProperty("runs-in-docker")) {
		exclude("**/ObjectControllerTest*") // cannot run MongoDB testcontainer during docker build
	}
	testLogging.showExceptions = true
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}
