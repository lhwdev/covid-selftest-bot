package com.lhwdev.discord.covidSelfTestBot.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.lhwdev.discord.covidSelfTestBot.server.serverConfig
import dev.kord.common.entity.Snowflake
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource


val testServer = Snowflake(serverConfig.servers.values.first().id)


class CommandsExtension : Extension() {
	override val name: String get() = "commands"
	
	override suspend fun setup() {
		publicSlashCommand {
			name = "hello"
			description = "Say hello!"
			
			action {
				// addReaction("✨")
				respond { content = "Hello!!!!!!" }
			}
		}
		
		class EvalArguments : Arguments() {
			val command by string(displayName = "command", description = "Command to run")
		}
		
		publicSlashCommand(arguments = ::EvalArguments) {
			name = "eval"
			description = "Say Eval!"
			
			val host = CommandHost(File("."))
			
			action {
				// addReaction("✨")
				val receivers = listOf<Any>(channel.asChannel())
				val result = try {
					when(val result =
						host.eval(arguments.command.toScriptSource(), receivers).valueOrThrow().returnValue) {
						is ResultValue.Value -> result.value
						is ResultValue.Unit -> "Unit"
						is ResultValue.Error -> result.error.stackTraceToString()
						ResultValue.NotEvaluated -> "(not evaluated)"
					}
				} catch(th: Throwable) {
					th.stackTraceToString()
				}
				respond { content = result.toString().take(1900) }
			}
		}
		
		ephemeralSlashCommand {
			name = "hello-me"
			description = "Say hello only to me!"
			
			action {
				// addReaction("✨")
				respond { content = "Hello!!!!!!" }
			}
		}
	}
}
