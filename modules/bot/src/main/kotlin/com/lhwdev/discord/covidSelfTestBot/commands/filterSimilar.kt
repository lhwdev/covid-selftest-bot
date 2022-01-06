package com.lhwdev.discord.covidSelfTestBot.commands

import com.kotlindiscord.kord.extensions.utils.hasPermissions
import com.lhwdev.discord.covidSelfTestBot.utils.*
import com.lhwdev.discord.utils.MessageCreateOrModifyBuilder
import com.lhwdev.discord.utils.actionRow
import com.lhwdev.discord.utils.asCommonBuilder
import com.lhwdev.utils.takeEllipsis
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Permission
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.behavior.interaction.acknowledgePublicUpdateMessage
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.GuildMessageCommandInteraction
import dev.kord.rest.builder.message.create.actionRow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile


private sealed interface Parameters

private enum class Misc : Parameters { okay, cancel }

private enum class Option(val label: String) : Parameters {
	message(label = "메시지 필터링하기"),
	user(label = "멤버 필터링하기")
}


private enum class Precision(val label: String, val emoji: String, val value: Int) {
	low(label = "낮음", emoji = "👌", value = 5),
	medium(label = "보통", emoji = "📖", value = 15),
	strong(label = "강함", emoji = "🔨", value = 30),
	hardcore(label = "매우 강함", emoji = "🤬", value = 50)
}


suspend fun Kord.filterSimilar(interaction: GuildMessageCommandInteraction) {
	// is this right?
	val hasPermission = interaction.user.asMember(interaction.guildId).hasPermissions(
		Permission.ManageMessages,
		Permission.ManageRoles,
		Permission.BanMembers
	)
	
	if(!hasPermission) {
		interaction.respondEphemeral { content = "⚠ 이 명령어를 사용할 권한이 없습니다." }
		return
	}
	
	val target = interaction.getTarget()
	
	
	val handler = InteractGroup<Parameters>()
	val options = InteractManager<Option>(handler)
	val optionsSet = mutableSetOf<Option>()
	
	val actionHandler = InteractManager<Misc>(handler)
	
	
	fun MessageCreateOrModifyBuilder.updateContent() {
		handler.reset()
		content = "'${target.content.takeEllipsis(20, ellipsis = Typography.ellipsis)}'과(와) 비슷한 메시지를 필터링합니다."
		
		actionRow {
			fun optionButton(option: Option) {
				val enabled = option in optionsSet
				
				interactionButton(
					style = if(enabled) ButtonStyle.Success else ButtonStyle.Secondary,
					customId = options.component(option)
				) {
					emoji = if(enabled) DiscordPartialEmoji(name = "🔘") else null
					label = option.label
				}
			}
			
			for(option in enumValues<Option>()) {
				optionButton(option)
			}
		}
		
		actionRow {
			interactionButton(style = ButtonStyle.Primary, customId = actionHandler.component(Misc.okay)) {
				label = "확인"
			}
			interactionButton(style = ButtonStyle.Secondary, customId = actionHandler.component(Misc.cancel)) {
				label = "취소"
			}
		}
	}
	
	val response1 = interaction.respondContextual {
		asCommonBuilder().updateContent()
	}
	
	
	fun click(option: Option) {
		if(option in optionsSet) {
			optionsSet -= option
		} else {
			optionsSet += option
		}
	}
	
	collectComponents(handler).takeWhile { (parameter, interaction) ->
		when(parameter) {
			is Option -> {
				click(parameter)
				interaction.acknowledgePublicUpdateMessage { asCommonBuilder().updateContent() }
				
				true
			}
			Misc.okay -> {
				val handler2 = selectMenuFilter()
				var precision: Precision? = null
				
				interaction.acknowledgePublicUpdateMessage {
					content = "매칭 정확도를 정해주세요."
					
					actionRow {
						selectMenu(customId = handler2.customId) {
							placeholder = "정확도"
							
							for(item in enumValues<Precision>()) option(
								label = item.label,
								value = item.value.toString()
							) {
								emoji = DiscordPartialEmoji(name = item.emoji)
							}
						}
					}
				}
				
				handler2.takeWhile { select ->
					val value = select.values.single().toInt()
					precision = enumValues<Precision>().find { it.value == value }
					select.acknowledgePublicDeferredMessageUpdate()
					
					true
				}.collect()
				
				false
			}
			Misc.cancel -> {
				(response1 as? PublicInteractionResponseBehavior)?.delete()
				false
			}
		}
	}.collect()
}
