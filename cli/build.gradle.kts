plugins {
	alias(libs.plugins.kotlin.jvm)
	application
}

dependencies {
	implementation(project(":zig-grammar"))
	implementation(project(":zon-grammar"))
	implementation(libs.clikt)
	implementation(libs.mordant)
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	jvmToolchain(25)
}

tasks.test {
	useJUnitPlatform()
}

application {
	applicationName = "zigocracy"

	mainClass = "com.zigocracy.sdk.cli.MainKt"
	applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}