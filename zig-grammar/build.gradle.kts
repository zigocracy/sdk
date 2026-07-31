plugins {
	alias(libs.plugins.kotlin.jvm)
}

dependencies {
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation(libs.jazzer.junit)
}

kotlin {
	jvmToolchain(25)
}

tasks.test {
	useJUnitPlatform {
		excludeTags("fuzz")
	}

	systemProperty("junit.jupiter.execution.parallel.enabled", "true")
	systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
	systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
	systemProperty("junit.jupiter.execution.parallel.config.executor-service", "worker_thread_pool")
}

tasks.register<Test>("fuzzTest") {
	description = "Runs Jazzer fuzz tests."
	group = "verification"

	val testTask = tasks.test.get()
	testClassesDirs = testTask.testClassesDirs
	classpath = testTask.classpath

	useJUnitPlatform {
		includeTags("fuzz")
	}

	maxHeapSize = "2048m"

	// Disable parallel testing, conflicts with Jazzer
	systemProperty("junit.jupiter.execution.parallel.enabled", "false")

	val instrumentedPackages = listOf(
		"net.landless_city.zigocracy.zig.parser.*",
		"net.landless_city.zigocracy.zig.scanner.*",
		"net.landless_city.zigocracy.zig.scanner.impl.*",
		"net.landless_city.zigocracy.zig.scanner.util.*",
		"net.landless_city.zigocracy.zig.syntax.*",
		"net.landless_city.zigocracy.zig.shared.*",
	)
	systemProperty(
		"jazzer.instrument",
		instrumentedPackages.joinToString(","),
	)

	// Fuzzing relies on randomness and must never be considered up-to-date or cached.
	doNotTrackState("Fuzzing yields different results every run")
}