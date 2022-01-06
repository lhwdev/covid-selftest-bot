package com.lhwdev.discord.covidSelfTestBot.selftest.utils

import com.lhwdev.discord.covidSelfTestBot.server.serverConfig
import com.lhwdev.discord.covidSelfTestBot.utils.long
import dev.kord.core.behavior.UserBehavior


fun UserBehavior.ensureAdmin() {
	require(id.long in serverConfig.admins) { "user is not admin" }
}
