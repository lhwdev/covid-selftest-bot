package com.lhwdev.discord.covidSelfTestBot.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.JarURLConnection
import java.net.URL
import kotlin.reflect.KType
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate


// See https://github.com/Kotlin/kotlin-script-examples/blob/master/jvm/simple-main-kts/simple-main-kts/src/main/kotlin/org/jetbrains/kotlin/script/examples/simpleMainKts/scriptDef.kt


@KotlinScript
abstract class CommandScript

internal fun URL.toContainingJarOrNull(): File? =
	if(protocol == "jar") {
		(openConnection() as? JarURLConnection)?.jarFileURL?.toFileOrNull()
	} else null

internal fun URL.toFileOrNull() =
	try {
		File(toURI())
	} catch(e: IllegalArgumentException) {
		null
	} catch(e: java.net.URISyntaxException) {
		null
	} ?: run {
		if(protocol != "file") null
		else File(file)
	}


class CommandHost(val commandDir: File) {
	open class ImplicitReceivers {
		fun receiver(type: KType): ImplicitReceiver {
			return ImplicitReceiver(type)
		}
	}
	
	class ImplicitReceiver(val type: KType)
	
	
	val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<Any> {
		implicitReceivers(CommandHost::class)
		
		jvm {
			val keyResource = CommandHost::class.java.name.replace('.', '/') + ".class"
			val thisJarFile = CommandHost::class.java.classLoader.getResource(keyResource)?.toContainingJarOrNull()
			if(thisJarFile != null) {
				dependenciesFromClassContext(
					CommandHost::class,
					thisJarFile.name, "kotlin-stdlib", "kotlin-reflect", "kotlin-scripting-dependencies"
				)
			} else {
				dependenciesFromClassContext(CommandHost::class, wholeClasspath = true)
			}
		}
	}
	
	
	val host = BasicJvmScriptingHost()
	
	suspend fun eval(sourceCode: SourceCode) =
		withContext(Dispatchers.IO) { // IDK why; host uses runBlocking
			val evaluationConfiguration = ScriptEvaluationConfiguration {
				implicitReceivers(this@CommandHost)
				// providedProperties.put(properties)
			}
			
			host.eval(
				sourceCode,
				compilationConfiguration = compilationConfiguration,
				evaluationConfiguration = evaluationConfiguration
			)
		}
	
	
	// val commandCache = mutableMapOf<String, >()
	//
	//
	// suspend fun initialize() {
	//
	// }
} 
