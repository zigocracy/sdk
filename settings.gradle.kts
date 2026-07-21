plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
	@Suppress("UnstableApiUsage")
	repositories {
		mavenCentral()
	}
}

rootProject.name = "zigocracy"

include("cli")

include("zon-parser")

include("meta-grammar-annotations")
include("meta-grammar-processors")