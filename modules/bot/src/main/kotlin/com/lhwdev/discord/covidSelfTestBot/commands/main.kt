// package com.lhwdev.discord.covidSelfTestBot.commands
//
// import cache.data.MessageInteractionData
// import com.kotlindiscord.kord.extensions.commands.Arguments
// import com.kotlindiscord.kord.extensions.commands.converters.impl.string
// import com.kotlindiscord.kord.extensions.extensions.Extension
// import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
// import com.kotlindiscord.kord.extensions.extensions.publicMessageCommand
// import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
// import com.kotlindiscord.kord.extensions.types.respond
// import com.lhwdev.discord.covidSelfTestBot.server.serverConfig
// import dev.kord.common.entity.InteractionType
// import dev.kord.common.entity.Snowflake
// import dev.kord.core.cache.data.InteractionData
// import dev.kord.core.entity.interaction.GuildMessageCommandInteraction
// import dev.kord.core.entity.interaction.MessageInteraction
// import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
// import java.io.File
// import kotlin.script.experimental.api.ResultValue
// import kotlin.script.experimental.api.valueOrThrow
// import kotlin.script.experimental.host.toScriptSource
//
//
// val testServer = Snowflake(serverConfig.servers.values.first().id)
//
//
// class CommandsExtension : Extension() {
// 	override val name: String get() = "commands"
//	
// 	override suspend fun setup() {
// 		publicSlashCommand {
// 			name = "hello"
// 			description = "Say hello!"
//			
// 			action {
// 				// addReaction("✨")
// 				respond { content = "Hello!!!!!!" }
// 			}
// 		}
//		
// 		class EvalArguments : Arguments() {
// 			val command by string(displayName = "command", description = "Command to run")
// 		}
//		
// 		publicSlashCommand(arguments = ::EvalArguments) {
// 			name = "eval"
// 			description = "Say Eval!"
//			
// 			val host = CommandHost(File("."))
//			
// 			action {
// 				// addReaction("✨")
// 				// val properties = mapOf("kord" to this@CommandsExtension.kord)
// 				val result = try {
// 					when(val result =
// 						host.eval(arguments.command.toScriptSource()).valueOrThrow().returnValue) {
// 						is ResultValue.Value -> result.value
// 						is ResultValue.Unit -> "Unit"
// 						is ResultValue.Error -> result.error.stackTraceToString()
// 						ResultValue.NotEvaluated -> "(not evaluated)"
// 					}
// 				} catch(th: Throwable) {
// 					th.stackTraceToString()
// 				}
// 				respond { content = result.toString().take(1900) }
// 			}
// 		}
//		
// 		ephemeralSlashCommand {
// 			name = "hello-me"
// 			description = "Say hello only to me!"
//			
// 			action {
// 				// addReaction("✨")
// 				respond { content = "Hello!!!!!!" }
// 			}
// 		}
//		
// 		publicMessageCommand {
// 			name = "도배 차단"
// 		}
// 	}
// }
