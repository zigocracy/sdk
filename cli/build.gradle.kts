plugins {
	alias(libs.plugins.kotlin.jvm)
	application
}

dependencies {
	implementation(project(":zon-parser"))
	implementation(libs.clikt)
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
	mainClass = "net.landless_city.zigocracy.cli.MainKt"
	applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}