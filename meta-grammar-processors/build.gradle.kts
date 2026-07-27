plugins {
	alias(libs.plugins.kotlin.jvm)
}

dependencies {
	implementation(project(":meta-grammar-annotations"))
	implementation(libs.ksp.api)
	implementation(libs.kotlinpoet)
	implementation(libs.kotlinpoet.ksp)
}

kotlin {
	jvmToolchain(25)
	explicitApi()
}