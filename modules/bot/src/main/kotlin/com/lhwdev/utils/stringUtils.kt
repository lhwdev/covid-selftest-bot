package com.lhwdev.utils


fun String.splitTwo(separator: Char): Pair<String, String> {
	val index = indexOf(separator)
	return take(index) to drop(index + 1)
}

fun String.splitTwo(separator: String): Pair<String, String> {
	val index = indexOf(separator)
	return take(index) to drop(index + separator.length)
}

fun String.splitTwoOrNull(separator: Char): Pair<String, String>? {
	val index = indexOf(separator)
	if(index == -1) return null
	return take(index) to drop(index + 1)
}

fun String.splitTwoOrNull(separator: String): Pair<String, String>? {
	val index = indexOf(separator)
	if(index == -1) return null
	return take(index) to drop(index + separator.length)
}

fun String.takeEllipsis(n: Int, ellipsis: String): String = if(length <= n) {
	this
} else {
	take(n - ellipsis.length) + ellipsis
}

fun String.takeEllipsis(n: Int, ellipsis: Char): String = if(length <= n) {
	this
} else {
	take(n - 1) + ellipsis
}
