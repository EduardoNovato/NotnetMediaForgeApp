package com.col.notnetmediaforge.data.repository

import org.json.JSONObject

internal fun JSONObject.optNullable(key: String): String? =
    if (isNull(key)) null else optString(key)

internal fun JSONObject.putNullable(key: String, value: String?) {
    put(key, value ?: JSONObject.NULL)
}