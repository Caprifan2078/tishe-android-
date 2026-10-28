package ru.tishe.app

import org.json.JSONObject

/**
 * Commit an encrypted document mutation only after isolating a deep snapshot.
 * JSONObject/JSONArray are mutable; never pass the cached root into a mutator.
 * The persist callback is the only way the new document becomes current.
 */
internal object DocumentTransactions {
    fun commit(
        current: JSONObject,
        mutate: (JSONObject) -> Unit,
        persist: (JSONObject) -> Unit
    ) {
        val candidate = JSONObject(current.toString())
        mutate(candidate)
        persist(candidate)
    }
}
