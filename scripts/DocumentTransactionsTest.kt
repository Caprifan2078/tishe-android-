package ru.tishe.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DocumentTransactionsTest {
    private fun original() = JSONObject()
        .put("diary", JSONArray().put(JSONObject().put("note", "Важная запись")))
        .put("checkIn", JSONObject().put("state", "старое"))
        .put("favoritePractices", JSONArray().put("wave"))

    @Test fun failedDiskWriteDoesNotMutateCachedDocument() {
        val cached = original()
        val before = cached.toString()
        var calls = 0
        try {
            DocumentTransactions.commit(cached, { candidate ->
                candidate.getJSONArray("diary").getJSONObject(0).put("note", "Не записано")
                candidate.getJSONObject("checkIn").put("state", "другое")
            }, {
                calls++
                error("Simulated disk failure")
            })
            fail("Expected simulated failure")
        } catch (expected: IllegalStateException) { /* correct */ }
        assertEquals(1, calls)
        assertEquals(before, cached.toString())
        assertEquals("Важная запись", cached.getJSONArray("diary").getJSONObject(0).getString("note"))
    }

    @Test fun mutationFailureSkipsWriteAndPreservesAllFields() {
        val cached = original()
        val before = cached.toString()
        var written = false
        try {
            DocumentTransactions.commit(cached, { candidate ->
                candidate.remove("diary")
                error("Simulated JSON mutation failure")
            }, { written = true })
            fail("Expected mutation failure")
        } catch (expected: IllegalStateException) { /* correct */ }
        assertFalse(written)
        assertEquals(before, cached.toString())
    }

    @Test fun successfulWriteContainsChangeAndKeepsOriginalUntouched() {
        val cached = original()
        val before = cached.toString()
        var persisted: JSONObject? = null
        DocumentTransactions.commit(cached, { candidate ->
            candidate.getJSONArray("diary").put(JSONObject().put("note", "Новая запись"))
        }, { persisted = it })
        assertEquals(before, cached.toString())
        assertEquals(2, persisted!!.getJSONArray("diary").length())
        assertEquals("wave", persisted!!.getJSONArray("favoritePractices").getString(0))
    }

    @Test fun replacingNestedArrayDoesNotLeakIntoOriginal() {
        val cached = original()
        DocumentTransactions.commit(cached, { candidate ->
            candidate.getJSONArray("favoritePractices").put("garden")
        }, { _ -> })
        assertEquals(1, cached.getJSONArray("favoritePractices").length())
    }
}
