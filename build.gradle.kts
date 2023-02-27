import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	id("org.springframework.boot") version "3.0.2"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.8.10"
	kotlin("plugin.spring") version "1.8.10"
	id("pl.allegro.tech.build.axion-release") version "1.14.4"
	id("org.openapi.generator") version "6.4.0"
	id("java-library")
}

group = "org.laelith"
version = "0.0.1-SNAPSHOT"
java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
}

scmVersion {
	tag {
		prefix.set("")
	}
}

project.version = scmVersion.version

dependencies {
	val kotlinxCoroutinesVersion = "1.6.4"
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.security:spring-security-oauth2-jose")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.security:spring-security-test")

	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.0.2")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	implementation("com.google.code.findbugs:jsr305:3.0.2")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("jakarta.validation:jakarta.validation-api")
	implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")

	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(module = "junit")
	}
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
	// Dices
	implementation("dev.diceroll:dice-parser:0.2.0")
}

springBoot {
	mainClass.set("org.laelith.vtt.LaelithVttApplicationKt")
}

tasks.withType<BootBuildImage> {
	dependsOn("build")
	environment.set(mapOf("BP_JVM_VERSION" to "17"))
	if (System.getenv("GHCR_IMAGE") != null) {
		imageName.set("${System.getenv("GHCR_IMAGE")}:${System.getenv("GHCR_TAG")}")
		publish.set(true)
		tags.add("${System.getenv("GHCR_IMAGE")}:latest")
		docker {
			publishRegistry {
				username.set(System.getenv("GHCR_USERNAME"))
				password.set(System.getenv("GHCR_PASSWORD"))
			}
		}
	}
}

gradleEnterprise {
	buildScan {
		publishAlwaysIf(!System.getenv("BUILD_SCAN").isNullOrEmpty())
		termsOfServiceUrl = "https://gradle.com/terms-of-service"
		termsOfServiceAgree = "yes"
	}
}

val openapiSpec = "$rootDir/src/main/openapi/laelith-vtt.yaml"
val openApiServerSourcesGenerationDir = "${buildDir}/generated-sources/openapi/kotlin-spring"
val basePackage = "$group.${rootProject.name}"

sourceSets {
	main { java.srcDirs("$openApiServerSourcesGenerationDir/src/main/kotlin") }
}

tasks.withType<KotlinCompile> {
	dependsOn("openApiGenerate")
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<org.openapitools.generator.gradle.plugin.tasks.ValidateTask> {
	inputSpec.set(openapiSpec)
}

tasks.withType<org.openapitools.generator.gradle.plugin.tasks.GenerateTask> {
	inputSpec.set(openapiSpec)
	outputDir.set(openApiServerSourcesGenerationDir)
	generatorName.set("kotlin-spring")
	apiPackage.set("${basePackage}.api")
	modelPackage.set("${basePackage}.domain")
	groupId.set(group)
	packageName.set(basePackage)
	globalProperties.set(
		mapOf(
			"apiDocs" to "true",
			// Excluded because the OpenAPI Generator generates test classes that expect the
			// Service Implementation to be present in the 'apiPackage' package,
			// which is not the case when serviceInterface is true.
			// We will write our own tests instead.
			"apiTests" to "false"))
	additionalProperties.set(
		mapOf(
			"title" to "Laelith VTT Application",
			"basePackage" to basePackage,
			"artifactId" to rootProject.name,
			"enumPropertyNaming" to "original",
			"reactive" to true,
			"exceptionHandler" to false,
			"serviceInterface" to true,
			"useTags" to true,
			"useSpringBoot3" to true,
		)
	)
}