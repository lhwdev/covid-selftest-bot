// DSL_SCOPE_VIOLATION: IDK why happening 'val Project.libs: LibrariesForLibs' can't be called in this context by implicit receiver.
@Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")
plugins {
	// `libs` is from Version Catalog (https://docs.gradle.org/7.0/userguide/platforms.html#sub:central-declaration-of-dependencies)
	// See gradle/libs.versions.toml.
	alias(libs.plugins.kotlin.multiplatform) apply false
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.serialization) apply false
	alias(libs.plugins.compose) apply false
}

allprojects {
	repositories {
		google()
		mavenCentral()
		maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
	}
}
