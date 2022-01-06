@file:Suppress("SuspendFunctionOnCoroutineScope")

package com.lhwdev.discord.covidSelfTestBot.server

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.lhwdev.discord.covidSelfTestBot.utils.askYesNoInteraction
import com.lhwdev.discord.covidSelfTestBot.utils.long
import com.lhwdev.discord.covidSelfTestBot.utils.onDelete
import com.lhwdev.discord.utils.MessageCreateOrModifyBuilder
import com.lhwdev.discord.utils.actionRow
import com.lhwdev.discord.utils.asCommonBuilder
import com.lhwdev.utils.splitTwoOrNull
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.Event
import dev.kord.core.live.LiveMessage
import dev.kord.core.live.channel.live
import dev.kord.core.live.channel.onMessageDelete
import dev.kord.core.live.channel.onMessageUpdate
import dev.kord.core.live.live
import dev.kord.rest.builder.component.ButtonBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File


private fun MessageChannel.processContent(from: String, guild: Guild): String = from
	.replace("<mention_everyone>", guild.everyoneRole.mention)


private val sContentYamlConfig = Yaml(
	configuration = YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property)
)


@Serializable
class ServerInfoConfig(
	val servers: Map<String, Server>,
	val admins: List<Long>
) {
	@Serializable
	class Server(
		val id: Long,
		val stagingChannels: List<StagingChannel>
	)
	
	@Serializable
	class StagingChannel(
		val id: Long
	)
}

@Serializable
private class Content(
	val targetChannel: String,
	val targetMessageId: Long? = null,
	val components: List<Row>? = null,
	val embeds: List<Embed>? = null,
	val forceTargetMessageId: Long? = null,
	val forceDelete: Boolean = false
) {
	@Serializable
	class Row(
		val components: List<Component>
	)
	
	@Serializable
	sealed class Component
	
	@Serializable
	@SerialName("button")
	class Button(
		val label: String,
		val style: Style,
		val action: String,
		val disabled: Boolean = false,
		val emoji: String? = null
	) : Component() {
		enum class Style { primary, secondary, danger, url }
	}
	
	
	@Serializable
	class Embed()
}

@Serializable
private data class BotContent(
	val originalMessageId: Long,
	val publishChannelId: Long,
	val publishedMessageId: Long?
)



val serverConfig = Yaml.default.decodeFromString(
	ServerInfoConfig.serializer(), File("config/server-info.yml").readText()
)


suspend fun Kord.serverMain() {
	serverConfig.servers.forEach { (name, server) ->
		val guild = getGuild(id = Snowflake(server.id))!!
		
		for(stagingChannelConfig in server.stagingChannels) {
			val stagingChannel = getChannel(Snowflake(stagingChannelConfig.id)) as MessageChannel
			
			stagingChannel.live {
				onMessageUpdate { event ->
					val message = event.getMessage()
					if(message.author?.isBot == false) {
						onMessage(stagingChannel, message)
					}
				}
				
				onMessageDelete onDelete@{ event ->
					val message = event.message // this will happen when this was cached
					if(message?.author?.isBot == true) {
						return@onDelete
					}
					
					val channel = event.getChannel()
					val result = queryBotMetaForUserMessage(channel, event.messageId)
					if(result != null) {
						val (botMessage, botMeta) = result
						val publishedId = botMeta.publishedMessageId
						if(publishedId != null) launch {
							val published = channel.getMessageOrNull(messageId = Snowflake(publishedId))
							published?.delete(reason = "스테이징 채널의 원본 공지가 삭제되어서 자동으로 삭제됩니다.")
							botMessage.edit { content = "✅ 공지를 성공적으로 삭제했습니다." }
							
							delay(5000)
							
							botMessage.delete()
						}
					}
				}
			}
		}
	}
}


private val sChannelRegex = Regex("""<#(\d+)>""")
private val sEmojiRegex = Regex("""<:([\w\d_]+):(\d+)>""")

private val sLiveCache = mutableMapOf<Snowflake, LiveMessage>()


private suspend fun Message.tempReply(result: Pair<Message, BotContent>?, content: String) {
	responseReply(result, temporary = true) { this.content = content }
}

private suspend fun Message.responseReply(
	result: Pair<Message, BotContent>?,
	temporary: Boolean,
	block: suspend MessageCreateOrModifyBuilder.() -> Unit
): Pair<Message, BotContent> {
	val botContent = BotContent(
		originalMessageId = id.long,
		publishChannelId = getChannel().id.long,
		publishedMessageId = result?.second?.publishedMessageId
	)
	
	suspend fun MessageCreateOrModifyBuilder.doContent() {
		components.clear()
		
		block()
		val last = content
		val head = "`" + Json.encodeToString(BotContent.serializer(), botContent) + "`\n"
		
		content = if(last == null) {
			head
		} else {
			"$head$last"
		}
	}
	
	@Suppress("JoinDeclarationAndAssignment") // ??? R U serious, intellij?
	lateinit var reply: Message
	
	if(!temporary) {
		val lastLive = sLiveCache.getOrElse(id) { null }
		if(lastLive != null) {
			sLiveCache -= id
			lastLive.shutDown()
		}
	}
	if(temporary) sLiveCache.getOrPut(id) {
		val live = live()
		
		val listener: suspend (Event) -> Unit = {
			reply.delete()
			live.shutDown()
		}
		
		// live.onUpdate(block = listener)
		live.onDelete(block = listener)
		live
	}
	
	
	@Suppress("IfThenToElvis")
	reply = if(result == null) reply {
		asCommonBuilder().doContent()
	} else result.first.edit {
		asCommonBuilder().doContent()
	}
	return reply to botContent
}


private fun getBotMeta(content: String): BotContent? {
	val metaStr = content.substringBefore("\n").removeSurrounding(prefix = "`", suffix = "`")
	return try {
		Json.decodeFromString(BotContent.serializer(), metaStr)
	} catch(th: Throwable) {
		null
	}
}

suspend fun Kord.onMessage(channel: MessageChannel, message: Message) {
	// lookup existing
	val result = queryBotMetaForUserMessage(channel, message.id)
	
	// parse content
	val (userMetaStr, userContent) = message.content.splitTwoOrNull("----------") // 10 '-' (dash)
		?.let { it.first to it.second.trim() }
		?: run {
			message.tempReply(result, "⚠ 잘못된 양식입니다.\n```\n<yaml 설정>\n----------\n내용\n```\n의 형식으로 작성돼야 합니다.")
			return
		}
	
	val userMeta = try {
		sContentYamlConfig.decodeFromString(Content.serializer(), userMetaStr)
	} catch(th: Throwable) {
		message.tempReply(result, "⚠ 잘못된 양식입니다. (`${th.message}`)")
		return
	}
	
	val targetMatch = sChannelRegex.matchEntire(userMeta.targetChannel) ?: run {
		message.tempReply(result, "⚠ 잘못된 대상 채널입니다.\n`#채널`의 형식으로 입력해주세요. (내부적으로는 `<#채널 id>`)")
		return
	}
	val target = getChannel(Snowflake(targetMatch.groupValues[1])) as? MessageChannel ?: run {
		message.tempReply(result, "⚠ 대상 채널을 찾을 수 없거나, 메세지를 보낼 수 없는 채널입니다.")
		return
	}
	
	
	suspend fun MessageCreateOrModifyBuilder.doContent() {
		content = target.processContent(userContent, message.getGuild())
		
		@Suppress("SuspendFunctionOnCoroutineScope")
		if(userMeta.components != null) for(row in userMeta.components) actionRow {
			fun ButtonBuilder.common(c: Content.Button) {
				label = c.label
				disabled = c.disabled
				
				if(c.emoji != null) {
					val match = sEmojiRegex.matchEntire(c.emoji)
					emoji = if(match == null) {
						DiscordPartialEmoji(name = c.emoji)
					} else {
						DiscordPartialEmoji(
							name = match.groupValues[1],
							id = Snowflake(match.groupValues[2])
						)
					}
				}
			}
			
			// val buttons = ComponentsManager<Int>()
			
			for((index, c) in row.components.withIndex()) when(c) {
				is Content.Button -> when(c.style) {
					Content.Button.Style.primary,
					Content.Button.Style.secondary,
					Content.Button.Style.danger -> {
						val style = when(c.style) {
							Content.Button.Style.primary -> ButtonStyle.Primary
							Content.Button.Style.secondary -> ButtonStyle.Secondary
							Content.Button.Style.danger -> ButtonStyle.Danger
							else -> error("???")
						}
						
						// interactionButton(style = style, customId = buttons.component(index)) {
						// 	common(c)
						// }
						TODO()
					}
					Content.Button.Style.url -> linkButton(c.action) {
						common(c)
					}
				}
			}
			
			// If it has buttons, add action eval feature
			
		}
	}
	
	val publishedMessageId = result?.second?.publishedMessageId
	
	suspend fun createNotification(ask: Message, botMeta: BotContent) {
		val published = target.createMessage {
			asCommonBuilder().doContent()
		}
		message.responseReply(ask to botMeta.copy(publishedMessageId = published.id.long), temporary = true) {
			content = "✅ 공지를 <#${target.id.asString}>에 등록했습니다."
			components.clear()
			actionRow {
				linkButton("discord://-/channels/${message.getGuild().id.asString}/${target.id.asString}/${published.id.asString}") {
					label = "공지 보기"
				}
			}
		}
	}
	
	suspend fun editNotification(publishedId: Long) {
		val published = target.getMessageOrNull(messageId = Snowflake(publishedId)) ?: run {
			val botMeta: BotContent
			val (answer, ask) = askYesNoInteraction(
				yesStyle = ButtonStyle.Primary,
				yesText = "올리기",
				noStyle = ButtonStyle.Secondary,
				noText = "취소",
				previous = result?.first
			) { doContent ->
				val pair = message.responseReply(result, temporary = true) {
					content = "⚠ 기존 메세지가 없습니다. 새롭게 공지를 올리시겠습니까?"
					doContent()
				}
				botMeta = pair.second
				pair.first
			}
			
			if(answer) {
				createNotification(ask, botMeta)
			} else {
				ask.delete()
			}
			
			return
		}
		
		published.edit {
			asCommonBuilder().doContent()
		}
		message.responseReply(result, temporary = false) {
			content = "✅ 내용을 수정했어요."
		}
	}
	
	when {
		userMeta.forceTargetMessageId != null -> editNotification(userMeta.forceTargetMessageId)
		publishedMessageId == null -> launch { // create message
			val botMeta: BotContent
			val (answer, ask) = askYesNoInteraction(
				yesStyle = ButtonStyle.Primary,
				yesText = "올리기",
				noStyle = ButtonStyle.Secondary,
				noText = "취소",
				previous = result?.first
			) { doContent ->
				val pair = message.responseReply(result, temporary = true) {
					content = "공지를 <#${target.id.asString}>에 올리시겠습니까?"
					doContent()
				}
				botMeta = pair.second
				pair.first
			}
			if(answer) {
				createNotification(ask, botMeta)
			} else {
				ask.delete()
			}
			
		}
		else -> {
			editNotification(publishedMessageId)
		}
	}
}

private suspend fun Kord.queryBotMetaForUserMessage(
	channel: MessageChannel,
	messageId: Snowflake
) = channel.getMessagesAfter(messageId, limit = 5).transformWhile find@{
	if(it.author?.id != selfId) return@find true
	
	val meta = getBotMeta(it.content) ?: return@find true
	
	if(meta.originalMessageId == messageId.long) {
		emit(it to meta)
		return@find false
	}
	
	true
}.firstOrNull()



