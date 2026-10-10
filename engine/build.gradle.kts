plugins {
	alias(libs.plugins.kotlin.jvm)
}

dependencies {
	api(project(":zig-grammar"))
	api(project(":zon-grammar"))

	testImplementation(libs.junit.jupiter)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	jvmToolchain(25)
}

tasks.test {
	useJUnitPlatform()

	systemProperty("junit.jupiter.execution.parallel.enabled", "true")
	systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
	systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
	systemProperty("junit.jupiter.execution.parallel.config.executor-service", "worker_thread_pool")
}