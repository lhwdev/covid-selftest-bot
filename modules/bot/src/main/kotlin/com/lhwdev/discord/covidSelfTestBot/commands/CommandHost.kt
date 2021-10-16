package com.lhwdev.discord.covidSelfTestBot.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate


@KotlinScript
class CommandScript


class CommandHost(val commandDir: File) {
	val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<Any> {
		jvm {
			// dependenciesFromCurrentContext("script"/* wholeClasspath = true*/)
		}
	}
	
	
	val host = BasicJvmScriptingHost()
	
	suspend fun eval(sourceCode: SourceCode, receivers: List<Any>) =
		withContext(Dispatchers.IO) { // IDK why; host uses runBlocking
			
			// val evaluationConfiguration = ScriptEvaluationConfiguration {
			// 	this.implicitReceivers.put(receivers)
			// }
			
			host.eval(
				sourceCode,
				compilationConfiguration = compilationConfiguration,
				evaluationConfiguration = /*evaluationConfiguration*/ null
			)
		}
	
	
	// val commandCache = mutableMapOf<String, >()
	//
	//
	// suspend fun initialize() {
	//
	// }
} 
