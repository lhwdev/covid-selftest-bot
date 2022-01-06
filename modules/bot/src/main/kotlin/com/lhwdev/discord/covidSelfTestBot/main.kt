package com.lhwdev.discord.covidSelfTestBot

import com.charleskorn.kaml.Yaml
import com.lhwdev.discord.covidSelfTestBot.commands.commandsMain
import com.lhwdev.discord.covidSelfTestBot.server.serverMain
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import kotlinx.serialization.Serializable
import java.io.File


@Serializable
class SecretConfig(val token: String, val commands: Boolean)


val secretConfig = Yaml.default.decodeFromString(SecretConfig.serializer(), File("config/secret-info.yml").readText())


suspend fun main() {
	// System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG")
	
	val kord = Kord(token = secretConfig.token)
	
	kord.serverMain()
	kord.commandsMain()
	
	kord.on<ReadyEvent> {
		println("ready!")
	}
	
	// kord.getGuildApplicationCommands(testServer).collect { it.delete() }
	
	// This suspends until logout
	kord.login()
}
