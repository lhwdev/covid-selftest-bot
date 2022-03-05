package com.lhwdev.discord.covidSelfTestBot.commands

import com.lhwdev.discord.covidSelfTestBot.secretConfig
import com.lhwdev.discord.covidSelfTestBot.server.serverConfig
import com.lhwdev.discord.covidSelfTestBot.utils.collectCommand
import com.lhwdev.discord.covidSelfTestBot.utils.filterUser
import com.lhwdev.discord.covidSelfTestBot.utils.long
import com.lhwdev.discord.covidSelfTestBot.utils.selectMenuFilter
import com.lhwdev.utils.splitTwoOrNull
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.entity.interaction.GuildMessageCommandInteraction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.ActionRowComponentBuilder
import dev.kord.rest.builder.component.SelectMenuBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.route.Route
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@Suppress("SuspendFunctionOnCoroutineScope")
suspend fun Kord.commandsMain() {
	on<MessageCreateEvent> {
		if(selfId in message.mentionedUserIds) {
			println(message.content)
			val realContent = message.content.trim()
				.removePrefix("<@!${selfId.asString}>")
				.removePrefix("<@${selfId.asString}>")
				.trim()
			onCommand(realContent)
		}
		
	}
	
	slashMain()
}

enum class Command { filter, test }


@Suppress("SuspendFunctionOnCoroutineScope")
suspend fun Kord.slashMain() {
	val manager = guildApplicationCommands<Command>(guildId = Snowflake(868429217740783637)) {
		if(!secretConfig.commands) return@guildApplicationCommands
		message(id = Command.filter, name = "유사 메시지 필터")
		
		input(Command.test, name = "test", description = "Wa! Test!") {
			string(name = "arg", description = "String")
		}
	}
	
	launch {
		collectCommand(manager).collect { (command, interaction) ->
			when(command) {
				Command.filter -> filterSimilar(interaction as GuildMessageCommandInteraction)
				Command.test -> {
					interaction as GuildChatInputCommandInteraction
					@OptIn(KordUnsafe::class, KordExperimental::class)
					rest.unsafe(route = Route.InteractionResponseCreate) {
						keys[Route.InteractionId] = interaction.id
						keys[Route.InteractionToken] = interaction.token
						body(
							ModalInteractionResponseCreateRequest.serializer(),
							ModalInteractionResponseCreateRequest(
								type = InteractionResponseType.Unknown(9), // MODAL
								data = ModalInteractionApplicationCommandCallbackData(
									customId = "my_dialog",
									title = "와! 이것이 창??",
									components = listOf(
										// ButtonBuilder.InteractionButtonBuilder(
										// 	style = ButtonStyle.Primary,
										// 	customId = "wow"
										// ).apply {
										// 	label = "확인"
										// }.build()
										// ActionRowBuilder().apply { 
										// 	selectMenu(customId = "select!") {
										// 		option("히히히", value = "1")
										// 		option("신기하다", value = "2")
										//		
										// 	}
										// }.build()
										// SelectMenuBuilder(customId = "select").apply {
										// 	option("히히히", value = "1")
										// 	option("신기하다", value = "2")
										// }.build()
										DiscordComponent(
											ComponentType.ActionRow,
											components = Optional(listOf(
												DiscordComponent(
													type = ComponentType.Unknown(4), // text input
													customId = Optional("dialog"),
													style = Optional(ButtonStyle.Primary), // not button but '1' is correct
													label = Optional("하하하 나는 입력이다")
													// type	integer	4 for a text input
													// custom_id	string	a developer-defined identifier for the input, max 100 characters
													// style	integer	the Text Input Style
													// label	string	the label for this component
													// min_length?	integer	the minimum input length for a text input, min 0, max 4000
													// max_length?	integer	the maximum input length for a text input, min 1, max 4000
													// required?	boolean	whether this component is required to be filled, default false
													// value?	string	a pre-filled value for this component, max 4000 characters
													// placeholder?
												)
											))
										)
									)
								)
							)
						)
					}
				}
			}
		}
	}
}


private suspend fun MessageCreateEvent.onCommand(args: String) {
	val (command, other) = args.splitTwoOrNull(' ') ?: (args to "")
	when(command.trim()) {
		"ban" -> ban(other.trim())
		else -> message.reply { content = "알 수 없는 명령어입니다." }
	}
}

private suspend fun MessageCreateEvent.ban(args: String) {
	val author = message.author ?: return
	
	val isAdmin = author.id.long in serverConfig.admins
	if(!isAdmin) return
	
	val filter = kord.selectMenuFilter()
	
	val ask = message.reply {
		content = "벤 유형을 선택해주세요."
		actionRow {
			selectMenu(filter.customId) {
				option(label = "맴버", value = "0") {
					description = "탈퇴하지 않은 맴버에 대해 벤을 처리합니다."
				}
				
				option(label = "탈퇴한 계정", value = "1") {
					description = "이미 탈퇴한 계정에 대해서 벤을 처리합니다. 시간이 더 오래 걸립니다."
				}
			}
		}
	}
	
	// wait selection
	val select = filter.filterUser(from = author.id).first()
	when(select.values.first()) {
		"0" -> {
			// member
		}
	}
}

