package com.lhwdev.discord.covidSelfTestBot.commands

import com.lhwdev.discord.covidSelfTestBot.utils.collectCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


enum class Command { test }

suspend fun Kord.slashCommands() {
	val manager = guildApplicationCommands<Command>(guildId = Snowflake(868429217740783637)) {
		input(Command.test, name = "test", description = "Wa! Test!") {
			string(name = "arg", description = "String")
		}
	}
	
	launch {
		collectCommand(manager).collect { (command, interaction) ->
			when(command) {
				Command.test -> {
					interaction.respondEphemeral { content = "ping!" }
				}
			}
		}
	}
}
