package com.lhwdev.discord.covidSelfTestBot.commands

import com.lhwdev.discord.covidSelfTestBot.secretConfig
import com.lhwdev.discord.covidSelfTestBot.server.serverConfig
import com.lhwdev.discord.covidSelfTestBot.utils.collectCommand
import com.lhwdev.discord.covidSelfTestBot.utils.filterUser
import com.lhwdev.discord.covidSelfTestBot.utils.long
import com.lhwdev.discord.covidSelfTestBot.utils.selectMenuFilter
import com.lhwdev.utils.splitTwoOrNull
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.interaction.GuildMessageCommandInteraction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
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
	
	val commands = guildApplicationCommands<Int>(guildId = Snowflake(serverConfig.servers.values.first().id)) list@ {
		if(!secretConfig.commands) return@list
		message(id = 0, name = "유사 메시지 필터")
	}
	
	launch {
		collectCommand(commands).collect {
			when(it.data) {
				0 -> filterSimilar(it.interaction as GuildMessageCommandInteraction)
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

