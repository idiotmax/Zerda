package org.mozilla.rocket.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@Throws(JSONException::class)
fun String.toJsonArray(): JSONArray = JSONArray(this)

@Throws(JSONException::class)
fun String.toJsonObject(): JSONObject = JSONObject(this)

@Throws(JSONException::class)
fun <T> String.toJsonArray(parser: (JSONObject) -> T): List<T> {
    return toJsonArray().serialize(parser)
}

@Throws(JSONException::class)
fun <T> JSONObject.getJsonArray(name: String, parser: (JSONObject) -> T): List<T> {
    return getJSONArray(name).serialize(parser)
}

@Throws(JSONException::class)
private fun <T> JSONArray.serialize(parser: (JSONObject) -> T): List<T> {
    return (0 until this.length())
            .map { index -> this.getJSONObject(index) }
            .map { jsonObject -> parser(jsonObject) }
}