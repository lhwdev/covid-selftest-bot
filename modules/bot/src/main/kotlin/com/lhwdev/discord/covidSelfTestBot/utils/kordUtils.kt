package com.lhwdev.discord.covidSelfTestBot.utils

import com.lhwdev.utils.splitTwoOrNull
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.Event
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.live.LiveKordEntity
import dev.kord.core.live.LiveMessage
import dev.kord.core.live.exception.LiveCancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.WeakHashMap


val Snowflake.long: Long get() = value.toLong()


fun LiveKordEntity.onDelete(scope: CoroutineScope = this, block: suspend (Event) -> Unit) {
	scope.coroutineContext.job.invokeOnCompletion { cause ->
		if(cause is LiveCancellationException) scope.launch {
			block(cause.event)
		}
	}
}

@JvmName("onDeleteFiltered")
inline fun <reified T : Event> LiveKordEntity.onDelete(
	scope: CoroutineScope = this,
	noinline block: suspend (T) -> Unit
) {
	scope.coroutineContext.job.invokeOnCompletion { cause ->
		if(cause is LiveCancellationException) {
			val event = cause.event
			
			if(event is T) scope.launch {
				block(event)
			}
		}
	}
}


fun LiveMessage.onDeleteSelf(scope: CoroutineScope = this, block: suspend (MessageDeleteEvent) -> Unit) {
	onDelete(scope, block)
}


class ButtonManager<T : Any> {
	val uuid = UUID.randomUUID()
	val uuidStr = uuid.toString()
	
	val idMap = mutableListOf<T>()
	
	
	fun button(buttonId: T): String {
		val index = idMap.size
		idMap += buttonId
		return "$uuidStr:$index"
	}
	
	fun findButton(id: String): T? {
		val (uuid, index) = id.splitTwoOrNull(':') ?: return null
		
		return if(uuid == uuidStr) {
			findButtonIndex(index)
		} else {
			null
		}
	}
	
	fun findButtonIndex(index: String): T? {
		val indexInt = index.toIntOrNull() ?: return null
		return findButtonIndex(indexInt)
	}
	
	fun findButtonIndex(index: Int): T? {
		return idMap.getOrNull(index)
	}
}

class InteractionPool(kord: Kord) {
	private val buttonInteractions = kord.events
		.filterIsInstance<InteractionCreateEvent>()
		.mapNotNull {
			val interaction = it.interaction
			if(interaction is ButtonInteraction) {
				val split = interaction.componentId.splitTwoOrNull(':')
				
				if(split == null) {
					null
				} else {
					Button(interaction, split.first, split.second)
				}
			} else {
				null
			}
		}
		.shareIn(
			scope = kord,
			started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 10000, replayExpirationMillis = 0)
		)
	
	class Button(val interaction: ButtonInteraction, val uuid: String, val other: String)
	
	@Suppress("UNCHECKED_CAST")
	fun filterButton(uuid: String): Flow<Button> =
		buttonInteractions.filter { it.uuid == uuid }
}

private val interactionPoolCaches = WeakHashMap<Kord, InteractionPool>()


suspend fun <T : Any> Message.awaitButtonInteraction(manager: ButtonManager<T>): Pair<T, ButtonInteraction> {
	val pool = interactionPoolCaches.getOrPut(kord) { InteractionPool(kord) }
	val flow = pool.filterButton(manager.uuidStr)
	val interaction = flow.first()
	return manager.findButtonIndex(interaction.other)!! to interaction.interaction
}
