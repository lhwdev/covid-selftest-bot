package com.lhwdev.utils


inline fun <K, V> MutableMap(keys: Array<K>, block: (K) -> V): MutableMap<K, V> {
	val map = HashMap<K, V>(keys.size)
	
	for(key in keys) {
		map[key] = block(key)
	}
	
	return map
}
