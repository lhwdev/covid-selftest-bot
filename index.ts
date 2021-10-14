import { Client, Intents, Interaction, MessageActionRow, MessageButton, MessageSelectMenu, NewsChannel, TextChannel } from 'discord.js'
import { token } from './secrets.json'
import  chalk from 'chalk'


const client = new Client({ intents: [
  Intents.FLAGS.GUILDS,
  // Intents.FLAGS.GUILD_MEMBERS,
  // Intents.FLAGS.GUILD_BANS,
  // Intents.FLAGS.GUILD_EMOJIS_AND_STICKERS,
  // Intents.FLAGS.GUILD_INTEGRATIONS,
  // Intents.FLAGS.GUILD_WEBHOOKS,
  // Intents.FLAGS.GUILD_INVITES,
  // Intents.FLAGS.GUILD_VOICE_STATES,
  // Intents.FLAGS.GUILD_PRESENCES,
  Intents.FLAGS.GUILD_MESSAGES,
  // Intents.FLAGS.GUILD_MESSAGE_REACTIONS,
  // Intents.FLAGS.GUILD_MESSAGE_TYPING,
  // Intents.FLAGS.DIRECT_MESSAGES,
  // Intents.FLAGS.DIRECT_MESSAGE_REACTIONS,
  // Intents.FLAGS.DIRECT_MESSAGE_TYPING,
] })


function removeFromArray(arr, item) {
  arr.splice(arr.indexOf(item), 1)
}

function transform(g, c) {
  return c.replaceAll('<mention_everyone>', g.roles.everyone.toString())
}


client.once('ready', async () => {
  console.log(chalk.greenBright('ready!'))

  const guild = await client.guilds.fetch('868429217740783637')
  const staging = (await guild.channels.fetch('896981889565487144')) as TextChannel
  const release = (await guild.channels.fetch('871386327994728460')) as NewsChannel
  const test = (await guild.channels.fetch('872156323456892968')) as TextChannel

  // const target = release
  const split = '=========='

  await staging.messages.fetch()

  client.on('messageDelete', async m => {
    if(m.channelId != staging.id) return
    if(m.author?.bot) return

    const raw = m.content!
    const index = raw.indexOf(split)

    const meta = JSON.parse(raw.slice(0, index).trim().slice(3, -3))
    
    const target = await m.guild?.channels.fetch(meta.target) as TextChannel

    const message = await target.messages.fetch(meta.id)
    await message.delete()
  })

  const interactionListeners: ((i: Interaction) => Promise<void>)[] = []

  client.on('interactionCreate', async i => {
    for(const il of interactionListeners) await il(i)
  })

  client.on('messageUpdate', async (_, m) => {
    if(m.channelId != staging.id) return
    if(m.author?.bot) return

    const raw = m.content!
    const index = raw.indexOf(split)

    let meta
    try {
      meta = JSON.parse(raw.slice(0, index).trim().slice(3, -3))
    } catch(e) { return }
    const content = raw.slice(index + split.length).trim()
    
    const target = await m.guild?.channels.fetch(meta.target) as TextChannel
    
    if(!meta.id) {
      const buttonId = `lhwdev-bot_notice_upload:${m.id}${Math.random()}`
      const ask = await staging.send(
        {
          content: `공지를 <#${target.id}>에 올릴까요?`,
          components: [
            new MessageActionRow().addComponents(
              new MessageButton()
                .setLabel('업로드하기')
                .setStyle('PRIMARY')
                .setCustomId(buttonId + '.yes'),
              new MessageButton()
                .setLabel('취소')
                .setStyle('SECONDARY')
                .setCustomId(buttonId + '.no')
            )
          ]
        }
      )

      const listener: (i: Interaction) => Promise<void> = async i => {
        if(!i.isButton()) return
        if(i.customId == buttonId + '.yes') {
          // i.reply({ content: '!' })
          removeFromArray(interactionListeners, listener)
          const result = await target.send({
            content: transform(guild, content),
            embeds: meta.embeds ?? [],
            components: [
              new MessageActionRow().addComponents(...meta.components)
            ]
          })
          const resultMeta = { id: result.id, messageId: m.id }
          await ask.edit({ content: `> 공지를 <#${target.id}>에 올렸습니다.\n> id: ${result.id}\n||${JSON.stringify(resultMeta)}||`, components: [] })
        } else if(i.customId == buttonId + '.no') {
          removeFromArray(interactionListeners, listener)
          await ask.delete()
        }
      }
      interactionListeners.push(listener)

      // meta.id = result.id
      // const newMeta = JSON.stringify(meta)
      // m.edit({ content: `${newMeta}\n${split}\n${content}` }) 
    } else {
      const toEdit = await target.messages.fetch(meta.id)
      await toEdit.edit({
        content: transform(guild, content),
        embeds: meta.embeds ?? [],
        components: [ new MessageActionRow().addComponents(...meta.components) ]
      })
    }
  })
})

client.login(token)
