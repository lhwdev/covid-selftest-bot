package com.lhwdev.discord.covidSelftestBot

import dev.kord.core.Kord
import java.io.File


suspend fun main() {
	val kord = Kord(token = File("config/secret-token.txt").readText().trim()) {
		
	}
	kord.login { playing("저를 맨션해주세요!") }
	
	
}
