import com.lhwdev.build.*
import org.jetbrains.compose.compose


plugins {
	application
	
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
	
	id("common-plugin") // must be applied after Kotlin/Android plugins
}

kotlin {
	setup()
	
	sourceSets.all {
		languageSettings.optIn("dev.kord.common.annotation.KordPreview")
	}
}


tasks.named<JavaExec>("run") {
	mainClass.set("com.lhwdev.discord.covidSelfTestBot.MainKt")
	
	workingDir = rootProject.file(".")
}


dependencies {
	implementation(libs.kord)
	implementation(libs.kordExtension)
	
	implementation(compose.runtime)
	implementation(libs.serialization.core)
	implementation(libs.serialization.yaml)
	
	implementation("org.slf4j:slf4j-api:1.7.32")
	implementation("org.slf4j:slf4j-simple:1.7.32")
	
	// 이거
	val kotlinVersion = libs.versions.kotlin.get()
	implementation(kotlin("reflect"))
	implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
	implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion")
	// testRuntimeOnly("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion")
}
