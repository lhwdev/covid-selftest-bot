package com.lhwdev.discord.covidSelfTestBot

import com.charleskorn.kaml.Yaml
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.lhwdev.discord.covidSelfTestBot.commands.CommandsExtension
import com.lhwdev.discord.covidSelfTestBot.commands.testServer
import com.lhwdev.discord.covidSelfTestBot.server.serverMain
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import kotlinx.serialization.Serializable
import java.io.File


@Serializable
class SecretConfig(val token: String)


val secretConfig = Yaml.default.decodeFromString(SecretConfig.serializer(), File("config/secret-info.yml").readText())


suspend fun main() {
	// System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG")
	
	val bot = ExtensibleBot(token = secretConfig.token) {
		presence {
			// playing("저를 맨션해주세요!")
			playing("채팅에서 /hello를 쳐보세요!")
		}
		
		chatCommands {
			enabled = true
		}
		
		applicationCommands {
			enabled = true
			defaultGuild = testServer
		}
		
		extensions {
			add(::CommandsExtension)
		}
	}
	val kord = bot.getKoin().get<Kord>()
	
	kord.serverMain()
	
	kord.on<ReadyEvent> {
		println("ready!")
	}
	
	// kord.getGuildApplicationCommands(testServer).collect { it.delete() }
	
	// This suspends until logout
	bot.start()
}
