package com.lhwdev.discord.covidSelfTestBot.utils

import dev.kord.common.entity.ApplicationCommandOption
import dev.kord.common.entity.ApplicationCommandOptionType
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.Kord
import dev.kord.core.entity.Entity
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.ResolvedChannel
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.core.entity.interaction.OptionValue


private val currentSlashInvocation = ThreadLocal<SlashCommandInvocation>()

sealed class ApplicationCommand

class SlashApplicationCommand(
	val name: String,
	val description: String,
	val options: List<ApplicationCommandOption>,
	val onInvoke: (InteractionCommand) -> Unit
) : ApplicationCommand()


class SlashCommandInvocation(val arguments: Map<String, OptionValue<*>>)

private operator fun OptionalBoolean.Companion.invoke(value: Boolean?): OptionalBoolean = if(value == null) {
	OptionalBoolean.Missing
} else {
	OptionalBoolean.Value(value)
}

class SlashCommandParameter<T>(
	val type: ApplicationCommandOptionType,
	val name: String,
	val description: String,
	val default: Boolean?,
	val required: Boolean?,
	val options: Optional<List<ApplicationCommandOption>>
) {
	val currentValue: T
		@Suppress("UNCHECKED_CAST")
		get() = currentSlashInvocation.get()!!.arguments[name]?.value as T
	
	fun toKord(): ApplicationCommandOption = ApplicationCommandOption(
		type = type,
		name = name, description = description,
		default = OptionalBoolean(default),
		required = OptionalBoolean(required),
		// choices = 
		options = options
	)
}


class InteractionBuilder(val kord: Kord) {
	@PublishedApi
	internal val applicationCommands = mutableListOf<ApplicationCommand>()
	
	inline fun slashCommand(name: String, description: String, block: SlashCommandBodyBuilder.() -> Unit = {}) {
		val builder = SlashCommandBodyBuilder()
		builder.block()
		applicationCommands += SlashApplicationCommand(
			name,
			description,
			builder.parameters.map { it.toKord() },
			builder.onInvoke!!
		)
	}
}


abstract class ApplicationCommandBodyBuilder

abstract class SlashCommandBodyBuilderBase : ApplicationCommandBodyBuilder() {
	private var index = 0
	var parameters = mutableListOf<SlashCommandParameter<*>>()
	
	
	fun <T> parameter(
		type: ApplicationCommandOptionType,
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null,
		options: Optional<List<ApplicationCommandOption>> = Optional.Missing()
	): SlashCommandParameter<T> {
		val parameter = SlashCommandParameter<T>(
			type = type,
			name = name,
			description = description,
			default = default,
			required = required,
			options = options
		)
		parameters += parameter
		return parameter
	}
}


class SlashCommandBodyBuilder : SlashCommandBodyBuilderBase() {
	inline fun parameterSubCommandGroup(
		name: String,
		description: String,
		block: SlashSubcommandBodyBuilder.() -> Unit
	) {
		parameter<Nothing>(
			ApplicationCommandOptionType.SubCommandGroup,
			name,
			description,
			options = Optional.Value(SlashSubcommandBodyBuilder().apply(block).build())
		)
	}
	
	fun parameterString(
		name: String,
		description: String,
		default: Boolean? = null
	): SlashCommandParameter<String> =
		parameter(ApplicationCommandOptionType.String, name, description, default, required = true)
	
	fun parameterString(
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null
	): SlashCommandParameter<String?> =
		parameter(ApplicationCommandOptionType.String, name, description, default, required)
	
	fun parameterInteger(
		name: String,
		description: String,
		default: Boolean? = null
	): SlashCommandParameter<Int> =
		parameter(ApplicationCommandOptionType.Integer, name, description, default, required = true)
	
	fun parameterInteger(
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null
	): SlashCommandParameter<Int?> =
		parameter(ApplicationCommandOptionType.Integer, name, description, default, required)
	
	fun parameterBoolean(
		name: String,
		description: String,
		default: Boolean? = null
	): SlashCommandParameter<Boolean> =
		parameter(ApplicationCommandOptionType.Boolean, name, description, default, required = true)
	
	fun parameterBoolean(
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null
	): SlashCommandParameter<Boolean?> =
		parameter(ApplicationCommandOptionType.Boolean, name, description, default, required)
	
	fun parameterUser(
		name: String,
		description: String,
		default: Boolean? = null
	): SlashCommandParameter<User> = parameter(ApplicationCommandOptionType.User, name, description, default)
	
	fun parameterUser(
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null
	): SlashCommandParameter<User?> = parameter(ApplicationCommandOptionType.User, name, description, default, required)
	
	fun parameterChannel(
		name: String,
		description: String,
		default: Boolean? = null
	): SlashCommandParameter<ResolvedChannel> =
		parameter(ApplicationCommandOptionType.Channel, name, description, default, required = true)
	
	fun parameterChannel(
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null
	): SlashCommandParameter<ResolvedChannel?> =
		parameter(ApplicationCommandOptionType.Channel, name, description, default, required)
	
	fun parameterRole(
		name: String,
		description: String,
		default: Boolean? = null
	): SlashCommandParameter<Role> =
		parameter(ApplicationCommandOptionType.Role, name, description, default, required = true)
	
	fun parameterRole(
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null
	): SlashCommandParameter<Role?> = parameter(ApplicationCommandOptionType.Role, name, description, default, required)
	
	fun parameterMentionable(
		name: String,
		description: String,
		default: Boolean? = null
	): SlashCommandParameter<Entity> =
		parameter(ApplicationCommandOptionType.Mentionable, name, description, default, required = true)
	
	fun parameterMentionable(
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null
	): SlashCommandParameter<Entity?> =
		parameter(ApplicationCommandOptionType.Mentionable, name, description, default, required)
	
	fun parameterNumber(
		name: String,
		description: String,
		default: Boolean? = null
	): SlashCommandParameter<Double> =
		parameter(ApplicationCommandOptionType.Number, name, description, default, required = true)
	
	fun parameterNumber(
		name: String,
		description: String,
		default: Boolean? = null,
		required: Boolean? = null
	): SlashCommandParameter<Double?> =
		parameter(ApplicationCommandOptionType.Number, name, description, default, required)
	
	
	var onInvoke: ((InteractionCommand) -> Unit)? = null
}

class SlashSubcommandBodyBuilder : SlashCommandBodyBuilderBase() {
	inline fun parameterSubCommand(
		name: String,
		description: String,
		block: SlashSubcommandBodyBuilder.() -> Unit
	) {
		parameter<Nothing>(ApplicationCommandOptionType.SubCommand, name, description)
	}
	
	fun build(): List<ApplicationCommandOption> = parameters.map { it.toKord() }
}



suspend fun Kord.interactions(block: InteractionBuilder.() -> Unit) {
	// createGuildApplicationCommands()
}
