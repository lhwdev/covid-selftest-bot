package com.lhwdev.discord.covidSelfTestBot.commands

import com.lhwdev.discord.covidSelfTestBot.utils.InteractManager
import dev.kord.common.annotation.KordDsl
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.application.GuildApplicationCommand
import dev.kord.rest.builder.interaction.*
import kotlinx.coroutines.flow.toList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract



@KordDsl
class MyMultiApplicationCommandBuilder<T> {
	val commands = mutableMapOf<T, ApplicationCommandCreateBuilder>()

	@OptIn(ExperimentalContracts::class)
	inline fun message(id: T, name: String, builder: MessageCommandCreateBuilder.() -> Unit = {}) {
		contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
		commands[id] = MessageCommandCreateBuilder(name).apply(builder)
	}

	@OptIn(ExperimentalContracts::class)
	inline fun user(id: T, name: String, builder: UserCommandCreateBuilder.() -> Unit = {}) {
		contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
		commands[id] = UserCommandCreateBuilder(name).apply(builder)
	}

	@OptIn(ExperimentalContracts::class)
	inline fun input(
		id: T,
		name: String,
		description: String,
		builder: ChatInputCreateBuilder.() -> Unit = {}
	) {
		contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
		commands[id] = ChatInputCreateBuilder(name, description).apply(builder)
	}
}


@OptIn(ExperimentalContracts::class)
suspend inline fun <T : Any> Kord.guildApplicationCommands(
	guildId: Snowflake,
	builder: MyMultiApplicationCommandBuilder<T>.() -> Unit,
): InteractManager<T> {
	contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }

	val previousList = getGuildApplicationCommands(guildId = guildId).toList()
	val newMap = MyMultiApplicationCommandBuilder<T>().apply(builder).commands

	val addOrUpdate = mutableListOf<ApplicationCommandCreateBuilder>()
	val delete = mutableListOf<GuildApplicationCommand>()

	val manager = InteractManager<T>()

	for(previous in previousList) {
		val new = newMap.values.find { it.name == previous.name }
		if(new == null) {
			delete += previous
		}
	}

	for((key, new) in newMap.entries) {
		addOrUpdate += new
		manager.command(key)
	}

	val addRequests = addOrUpdate.map { it.toRequest() }

	val commands = rest.interaction.createGuildApplicationCommands(
		applicationId = resources.applicationId,
		guildId = guildId,
		request = addRequests
	)
	for((index, key) in newMap.keys.withIndex()) {
		val command = commands[index]
		manager.group.commandActualIdsMap[command.id] = key
	}

	delete.forEach { it.delete() }

	return manager
}
