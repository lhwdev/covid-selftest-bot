package com.lhwdev.discord.covidSelfTestBot.utils

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.event.Event
import dev.kord.core.live.live
import dev.kord.core.live.onUpdate
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder


// Simulates public ephemeral behavior.


suspend fun Message.temporalReply(reply: Message) {
	val live = live()
	
	val listener: suspend (Event) -> Unit = {
		reply.delete()
		live.shutDown()
	}
	
	live.onUpdate(block = listener)
	live.onDelete(block = listener)
}


suspend inline fun Message.temporalReply(block: UserMessageCreateBuilder.() -> Unit) {
	temporalReply(reply(block))
}
