/// Dependencies
// Looking for dependencies? see gradle/libs.versions.toml file.
// Also see Gradle Version Catalog(https://docs.gradle.org/7.1.1/userguide/platforms.html#sub:central-declaration-of-dependencies).
enableFeaturePreview("VERSION_CATALOGS")


/// Plugins
pluginManagement.repositories {
	gradlePluginPortal()
	maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}


/// Projects
includeBuild("includeBuild")

fun includeModules(path: String, prefix: String, vararg projectPaths: String) {
	for(projectPath in projectPaths) {
		val realPath = "$prefix$projectPath"
		include(realPath)
		
		val project = project(realPath)
		
		project.projectDir = File(path, projectPath.drop(1).replace(':', '/'))
	}
}

includeModules(
	path = "modules",
	prefix = "",
	
	":bot"
)
