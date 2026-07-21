plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.ksp)
}

dependencies {
	compileOnly(project(":meta-grammar-annotations"))
	ksp(project(":meta-grammar-processors"))
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	jvmToolchain(25)
}

tasks.test {
	useJUnitPlatform()
}