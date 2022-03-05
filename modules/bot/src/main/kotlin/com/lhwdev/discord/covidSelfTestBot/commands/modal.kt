package com.lhwdev.discord.covidSelfTestBot.commands

import dev.kord.common.entity.DiscordComponent
import dev.kord.common.entity.InteractionResponseType
import dev.kord.common.entity.optional.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
data class ModalInteractionResponseCreateRequest(
	val type: InteractionResponseType,
	val data: ModalInteractionApplicationCommandCallbackData
)

@Serializable
class ModalInteractionApplicationCommandCallbackData(
	@SerialName("custom_id")
	val customId: String,
	val title: String,
	val components: List<DiscordComponent>
)
