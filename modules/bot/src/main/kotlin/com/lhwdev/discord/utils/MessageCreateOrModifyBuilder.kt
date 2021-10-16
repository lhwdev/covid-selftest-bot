package com.lhwdev.discord.utils

import dev.kord.rest.NamedFile
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.MessageComponentBuilder
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


interface MessageCreateOrModifyBuilder {
	
	/**
	 * The text content of the message.
	 */
	var content: String?
	
	/**
	 * The embedded content of the message.
	 */
	val embeds: MutableList<EmbedBuilder>
	
	/**
	 * The mentions in this message that are allowed to raise a notification.
	 * Setting this to null will default to creating notifications for all mentions.
	 */
	var allowedMentions: AllowedMentionsBuilder?
	
	/**
	 * The message components to include in this message.
	 */
	
	val components: MutableList<MessageComponentBuilder>
	
	/**
	 * The files to include as attachments.
	 */
	val files: MutableList<NamedFile>
	
	
	/**
	 * Adds a file with the [name] and [content] to the attachments.
	 */
	fun addFile(name: String, content: InputStream): NamedFile {
		val namedFile = NamedFile(name, content)
		files += namedFile
		return namedFile
	}
	
	/**
	 * Adds a file with the given [path] to the attachments.
	 */
	suspend fun addFile(path: Path): NamedFile = withContext(Dispatchers.IO) {
		@Suppress("BlockingMethodInNonBlockingContext")
		addFile(path.fileName.toString(), Files.newInputStream(path))
	}
}

@OptIn(ExperimentalContracts::class)
inline fun MessageCreateOrModifyBuilder.embed(block: EmbedBuilder.() -> Unit) {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	
	embeds.add(EmbedBuilder().apply(block))
}

/**
 * Configures the mentions that should trigger a ping. Not calling this function will result in the default behavior
 * (ping everything), calling this function but not configuring it before the request is build will result in all
 * pings being ignored.
 */
@OptIn(ExperimentalContracts::class)
inline fun MessageCreateOrModifyBuilder.allowedMentions(block: AllowedMentionsBuilder.() -> Unit = {}) {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	allowedMentions = (allowedMentions ?: AllowedMentionsBuilder()).apply(block)
}


@OptIn(ExperimentalContracts::class)
inline fun MessageCreateOrModifyBuilder.actionRow(builder: ActionRowBuilder.() -> Unit) {
	contract {
		callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
	}
	components.add(ActionRowBuilder().apply(builder))
}


fun MessageCreateBuilder.asCommonBuilder(): MessageCreateOrModifyBuilder = MessageCreateOrModifyBuilderCreate(this)

class MessageCreateOrModifyBuilderCreate(private val create: MessageCreateBuilder) : MessageCreateOrModifyBuilder {
	override var content: String?
		get() = create.content
		set(value) {
			create.content = value
		}
	override val embeds: MutableList<EmbedBuilder>
		get() = create.embeds
	override var allowedMentions: AllowedMentionsBuilder?
		get() = create.allowedMentions
		set(value) {
			create.allowedMentions = value
		}
	override val components: MutableList<MessageComponentBuilder>
		get() = create.components
	override val files: MutableList<NamedFile>
		get() = create.files
}


fun MessageModifyBuilder.asCommonBuilder(): MessageCreateOrModifyBuilder = MessageCreateOrModifyBuilderModify(this)

private class SetOnModifyList<T>(val base: MutableList<T> = mutableListOf(), val onModify: (MutableList<T>?) -> Unit) :
	AbstractMutableList<T>() {
	override fun add(index: Int, element: T) {
		base.add(index, element)
		onModify(this)
	}
	
	override fun removeAt(index: Int): T {
		if(size == 1) onModify(null)
		return base.removeAt(index)
	}
	
	override fun set(index: Int, element: T): T = base.set(index, element)
	override val size: Int get() = base.size
	override fun get(index: Int): T = base[index]
}

class MessageCreateOrModifyBuilderModify(private val modify: MessageModifyBuilder) : MessageCreateOrModifyBuilder {
	override var content: String?
		get() = modify.content
		set(value) {
			modify.content = value
		}
	override val embeds: MutableList<EmbedBuilder> = modify.embeds ?: SetOnModifyList { modify.embeds = it }
	override var allowedMentions: AllowedMentionsBuilder?
		get() = modify.allowedMentions
		set(value) {
			modify.allowedMentions = value
		}
	override val components: MutableList<MessageComponentBuilder>
		get() = modify.components ?: SetOnModifyList { modify.components = it }
	override val files: MutableList<NamedFile>
		get() = modify.files ?: SetOnModifyList { modify.files = it }
}

