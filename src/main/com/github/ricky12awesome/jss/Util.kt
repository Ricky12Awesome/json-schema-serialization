package com.github.ricky12awesome.jss

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

fun MutableMap<String, JsonElement>.merge(key: String, element: JsonElement): JsonElement? {
  return merge(key, element, ::merge)
}

fun merge(previous: JsonElement, value: JsonElement): JsonElement? {
  return when {
    previous == value -> value
    previous is JsonObject && value is JsonObject -> merge(previous, value)
    previous is JsonArray && value is JsonArray -> JsonArray(previous + value)
    value is JsonNull -> previous
    else -> value
  }
}

fun merge(previous: JsonObject, value: JsonObject): JsonObject? {
  val elements = mutableMapOf<String, JsonElement>()
  val values = (previous.keys + value.keys).toSet().associateWith {
    (previous[it] ?: JsonNull) to (value[it] ?: JsonNull)
  }

  values.forEach { (name, oldNewPair) ->
    val (old, new) = oldNewPair
    val merged = merge(old, new) ?: return null

    elements[name] = merged
  }

  return JsonObject(elements)
}