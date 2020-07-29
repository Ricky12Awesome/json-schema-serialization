@file:Suppress("UNCHECKED_CAST")

package me.github.ricky12awesome.jss.dsl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import me.github.ricky12awesome.jss.JsonType
import me.github.ricky12awesome.jss.globalJson
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@ExperimentalJsonSchemaDSL
open class JsonSchemaPropertyBuilder<T>(
  type: JsonType,
  private val serializer: KSerializer<T>
) {
  val contents: MutableMap<String, JsonElement> = mutableMapOf("type" to type.json)

  var description by JsonSchemaPropertyElement("", JsonElement::content, ::JsonPrimitive)

  var enum by JsonSchemaPropertyElement(
    default = listOf(),
    get = { globalJson.fromJson(ListSerializer(serializer), it) },
    set = { globalJson.toJson(ListSerializer(serializer), it) }
  )

  var default by JsonSchemaPropertyElement(
    default = null,
    get = { globalJson.fromJson(serializer, it) },
    set = { it?.let { globalJson.toJson(serializer, it) } ?: JsonNull }
  )

  open fun build() = JsonObject(contents)
}

@ExperimentalJsonSchemaDSL
internal class JsonSchemaPropertyElement<T>(
  private val default: T,
  private val get: (JsonElement) -> T,
  private val set: (T) -> JsonElement
) : ReadWriteProperty<JsonSchemaPropertyBuilder<*>, T> {
  override fun getValue(thisRef: JsonSchemaPropertyBuilder<*>, property: KProperty<*>): T {
    return thisRef.contents[property.name]?.let(get) ?: default
  }

  override fun setValue(thisRef: JsonSchemaPropertyBuilder<*>, property: KProperty<*>, value: T) {
    val element = set(value)

    if (!element.isNull ||
      (element is JsonArray && element.isNotEmpty()) ||
      (element is JsonObject && element.isNotEmpty())
    ) {
      thisRef.contents[property.name] = element
    }
  }
}

@ExperimentalJsonSchemaDSL
var <T> JsonSchemaPropertyBuilder<T>.minimum: T?
    where T : Comparable<T>, T : Number
    by JsonSchemaPropertyElement(
      default = null,
      get = { it.doubleOrNull as T? },
      set = ::JsonPrimitive
    )

@ExperimentalJsonSchemaDSL
var <T> JsonSchemaPropertyBuilder<T>.maximum: T?
    where T : Comparable<T>, T : Number
    by JsonSchemaPropertyElement(
      default = null,
      get = { it.doubleOrNull as T? },
      set = ::JsonPrimitive
    )

@ExperimentalJsonSchemaDSL
var <T> JsonSchemaPropertyBuilder<T>.range: ClosedRange<T>?
    where T : Comparable<T>, T : Number
  get() {
    return minimum?.rangeTo(maximum ?: return null)
  }
  set(value) {
    minimum = value?.start
    maximum = value?.endInclusive
  }
