package org.wfanet.panelmatch.client.exchangetasks

/** Convenience function for throwing an [IllegalArgumentException] if a map key is missing. */
fun <K, V> Map<K, V>.getRequired(key: K): V {
  return requireNotNull(this[key]) { "Key '$key' not found in map. Keys are: $keys" }
}
