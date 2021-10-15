import com.lhwdev.build.*
import org.jetbrains.compose.compose


plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
	
	id("common-plugin") // must be applied after Kotlin/Android plugins
}

kotlin {
	setup()
}


dependencies {
	implementation(libs.kord)
	
	implementation(compose.runtime)
	implementation(libs.serialization.core)
}
