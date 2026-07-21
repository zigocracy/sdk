plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.ksp)
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