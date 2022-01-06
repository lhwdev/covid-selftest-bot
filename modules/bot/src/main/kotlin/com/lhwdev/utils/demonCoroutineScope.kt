package com.lhwdev.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.startCoroutine


// I don't know this is good implementation...
@OptIn(ExperimentalContracts::class)
suspend fun <R> demonCoroutineScope(block: suspend CoroutineScope.() -> R): R {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	
	return suspendCancellableCoroutine { cont ->
		val scope = CoroutineScope(cont.context)
		
		val myBlock: suspend CoroutineScope.() -> R = {
			try {
				block()
			} finally {
				scope.cancel()
			}
		}
		myBlock.startCoroutine(receiver = scope, completion = cont)
	}
}
