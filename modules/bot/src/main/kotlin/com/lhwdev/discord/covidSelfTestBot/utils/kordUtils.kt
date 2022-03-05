package com.lhwdev.discord.covidSelfTestBot.utils

import com.lhwdev.discord.utils.MessageCreateOrModifyBuilder
import com.lhwdev.discord.utils.actionRow
import com.lhwdev.utils.splitTwoOrNull
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.InteractionResponseBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Message
import dev.kord.core.entity.interaction.*
import dev.kord.core.event.Event
import dev.kord.core.event.interaction.ApplicationInteractionCreateEvent
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.live.LiveKordEntity
import dev.kord.core.live.LiveMessage
import dev.kord.core.live.exception.LiveCancellationException
import dev.kord.rest.builder.message.create.InteractionResponseCreateBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


val Snowflake.long: Long get() = value.toLong()

const val global = true

suspend inline fun ActionInteractionBehavior.respondContextual(
	block: InteractionResponseCreateBuilder.() -> Unit
): InteractionResponseBehavior = if(global) {
	respondPublic(block)
} else {
	respondEphemeral(block)
}

suspend fun ActionInteractionBehavior.acknowledgeContextual(): InteractionResponseBehavior = if(global) {
	acknowledgePublic()
} else {
	acknowledgeEphemeral()
}


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


data class ComponentInteractionData<T : ComponentInteraction>(val interaction: T, val uuid: String, val other: String)

typealias ButtonInteractionData = ComponentInteractionData<ButtonInteraction>
typealias SelectMenuInteractionData = ComponentInteractionData<SelectMenuInteraction>

private fun <T> Flow<T>.shareInDefaultGlobal(scope: CoroutineScope) = shareIn(
	scope = scope,
	started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 10000, replayExpirationMillis = 0)
)

class InteractionPool(kord: Kord) {
	val componentInteractions = kord.events.mapNotNull {
		if(it !is ComponentInteractionCreateEvent) return@mapNotNull null
		
		val interaction = it.interaction
		
		val split = interaction.componentId.splitTwoOrNull(':')
		
		if(split == null) {
			null
		} else {
			ComponentInteractionData(interaction, split.first, split.second)
		}
	}.shareInDefaultGlobal(kord)
	
	val commandInteractions = kord.events.filterIsInstance<ApplicationInteractionCreateEvent>()
		.map { it.interaction }
		.shareInDefaultGlobal(kord)
	
	
	@Suppress("UNCHECKED_CAST")
	fun filterComponentUuid(uuid: String): Flow<ComponentInteractionData<*>> =
		componentInteractions.filter { it.uuid == uuid }
	
	@Suppress("UNCHECKED_CAST")
	inline fun <reified T : ComponentInteraction> filterComponent(uuid: String): Flow<ComponentInteractionData<T>> =
		componentInteractions.filter { it.interaction is T && it.uuid == uuid }
			as Flow<ComponentInteractionData<T>>
	
	fun filterButton(uuid: String): Flow<ButtonInteractionData> = filterComponent(uuid)
	fun filterSelectMenu(uuid: String): Flow<SelectMenuInteractionData> = filterComponent(uuid)
}

@JvmName("filterUserData")
fun <T : ComponentInteractionData<*>> Flow<T>.filterUser(from: Snowflake): Flow<T> =
	filter { it.interaction.user.id == from }

@JvmName("filterUserComponent")
fun <T : ComponentInteraction> Flow<T>.filterUser(from: Snowflake): Flow<T> =
	filter { it.user.id == from }


private val interactionPoolCaches = WeakHashMap<Kord, InteractionPool>()

fun Kord.interactionPool(): InteractionPool = synchronized(interactionPoolCaches) {
	interactionPoolCaches.getOrPut(this) { InteractionPool(this) }
}

class InteractionFilter<T : ComponentInteraction>(
	uuid: String,
	flow: Flow<T>
) : Flow<T> by flow {
	val customId = "$uuid:_"
}

inline fun <reified T : ComponentInteraction> Kord.componentInteractionFilter(): InteractionFilter<T> {
	val uuid = UUID.randomUUID().toString()
	return InteractionFilter(uuid, interactionPool().filterComponent<T>(uuid).map { it.interaction })
}


fun Kord.selectMenuFilter(): InteractionFilter<SelectMenuInteraction> =
	componentInteractionFilter()

fun Kord.buttonFilter(): InteractionFilter<SelectMenuInteraction> =
	componentInteractionFilter()


private val ids = AtomicLong(0)

private fun nextId() = "${ids.getAndIncrement()}"

class InteractManager<T : Any>(val group: InteractGroup<in T> = InteractGroup()) {
	fun component(id: T): String {
		val index = group.idMap.size
		group.idMap += id
		return "${group.uuid}:$index"
	}
	
	fun command(id: T) {
		group.idMap += id
	}
}

class InteractGroup<T : Any> {
	val uuid = nextId()
	val managers = mutableListOf<InteractManager<out T>>()
	val idMap = mutableListOf<T>()
	
	val commandActualIdsMap = mutableMapOf<Snowflake, T>()
	
	fun <M : T> manager(): InteractManager<M> =
		InteractManager<M>(this).also { managers += it }
	
	
	fun findIndex(index: String): T? {
		val indexInt = index.toIntOrNull() ?: return null
		return findIndex(indexInt)
	}
	
	@Suppress("UNCHECKED_CAST")
	fun findIndex(index: Int): T? {
		return idMap.getOrNull(index)
	}
	
	fun reset() {
		managers.clear()
		idMap.clear()
		commandActualIdsMap.clear()
	}
}

data class InteractData<out T : Any, out I : Interaction>(val data: T, val interaction: I)



class InteractFlow<T : Any, I : Interaction>(val manager: InteractManager<T>) : Flow<InteractData<T, I>> {
	internal lateinit var flow: Flow<InteractData<T, I>>
	
	// note: used this though not safe to implement; as `Flow<...> by anotherFlow` makes code too complicated
	@InternalCoroutinesApi
	override suspend fun collect(collector: FlowCollector<InteractData<T, I>>) {
		flow.collect(collector)
	}
	
	fun find(id: String): T? {
		val (uuid, index) = id.splitTwoOrNull(':') ?: return null
		
		@Suppress("UNCHECKED_CAST")
		return if(uuid == manager.group.uuid) {
			manager.group.findIndex(index) as T?
		} else {
			null
		}
	}
}


private fun <T> Flow<T>.shareInDefault(scope: CoroutineScope) = shareIn(
	scope = scope,
	started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 100, replayExpirationMillis = 0)
)

fun <T : Any> Kord.collectComponent(manager: InteractManager<T>): InteractFlow<T, ComponentInteraction> =
	InteractFlow<T, ComponentInteraction>(manager = manager).also {
		it.flow = interactionPool().componentInteractions.mapNotNull { item ->
			if(item.uuid == manager.group.uuid) {
				@Suppress("UNCHECKED_CAST")
				val data = manager.group.findIndex(item.other) as? T ?: error("uuid matched but unknown index")
				InteractData(data, item.interaction)
			} else {
				null
			}
		}
	}

fun <T : Any> Kord.collectComponents(group: InteractGroup<T>): Flow<InteractData<T, ComponentInteraction>> =
	interactionPool().filterComponentUuid(group.uuid).map {
		InteractData(group.findIndex(it.other)!!, it.interaction)
	}

fun <T : Any> Kord.collectCommand(manager: InteractManager<T>): InteractFlow<T, ApplicationCommandInteraction> =
	InteractFlow<T, ApplicationCommandInteraction>(manager = manager).also {
		it.flow = interactionPool().commandInteractions.mapNotNull {
			@Suppress("UNCHECKED_CAST")
			val data = manager.group.commandActualIdsMap[it.invokedCommandId] as T?
			if(data != null) {
				InteractData(data, it)
			} else {
				null
			}
		}
	}

fun <T : Any> Kord.collectCommands(group: InteractGroup<T>): Flow<InteractData<T, ApplicationCommandInteraction>> =
	interactionPool().commandInteractions.mapNotNull {
		@Suppress("UNCHECKED_CAST")
		val data = group.commandActualIdsMap[it.invokedCommandId]
		if(data != null) {
			InteractData(data, it)
		} else {
			null
		}
	}


@Suppress("UNCHECKED_CAST")
inline fun <T : Any, reified I : Interaction> Flow<InteractData<T, *>>.filterInteraction(): Flow<InteractData<T, I>> =
	filter { it.interaction is I } as Flow<InteractData<T, I>>

fun <T : Any> Flow<InteractData<T, *>>.filterButton(): Flow<InteractData<T, ButtonInteraction>> =
	filterInteraction()

fun <T : Any> Flow<InteractData<T, *>>.filterSelectMenu(): Flow<InteractData<T, SelectMenuInteraction>> =
	filterInteraction()

fun <T : Any> Flow<InteractData<T, *>>.filterCommand(): Flow<InteractData<T, ApplicationCommandInteraction>> =
	filterInteraction()

fun <T : Any> Flow<InteractData<T, *>>.filterGuildCommand(): Flow<InteractData<T, GuildApplicationCommandInteraction>> =
	filterInteraction()



suspend fun <T : Any> InteractFlow<T, *>.awaitButtonInteraction(
	targetMessage: Message,
	filterSameUser: Snowflake? = targetMessage.referencedMessage?.author?.id
): InteractData<T, ButtonInteraction> {
	val flow = filterButton()
	return if(filterSameUser != null) {
		flow.first { it.interaction.user.id == filterSameUser }
	} else {
		flow.first()
	}
}


@OptIn(ExperimentalContracts::class)
suspend fun Kord.askYesNoInteraction(
	yesStyle: ButtonStyle,
	yesText: String,
	noStyle: ButtonStyle,
	noText: String,
	previous: Message? = null,
	filterSameUser: Boolean = true,
	block: suspend (doContent: MessageCreateOrModifyBuilder.() -> Unit) -> Message
): Pair<Boolean, Message> {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	
	val button = InteractManager<Boolean>()
	
	val doContent: MessageCreateOrModifyBuilder.() -> Unit = {
		actionRow {
			interactionButton(style = yesStyle, customId = button.component(true)) {
				label = yesText
			}
			
			interactionButton(style = noStyle, customId = button.component(false)) {
				label = noText
			}
		}
	}
	
	
	@Suppress("IfThenToElvis")
	val ask = block(doContent)
	
	val (result, interaction) = collectComponent(button).awaitButtonInteraction(
		targetMessage = ask,
		filterSameUser = if(filterSameUser) ask.referencedMessage?.author?.id else null
	)
	interaction.acknowledgePublic().delete()
	return result to ask
}
