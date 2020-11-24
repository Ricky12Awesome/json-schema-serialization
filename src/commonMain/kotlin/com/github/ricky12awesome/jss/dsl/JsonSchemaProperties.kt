package com.github.ricky12awesome.jss.dsl

import com.github.ricky12awesome.jss.globalJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PropertyDelegate<T, B : PropertyBuilder<T>, V>(
  val id: String? = null,
  val default: JsonElement = JsonNull,
  val get: B.(JsonElement) -> V,
  val set: B.(V) -> JsonElement
) : ReadWriteProperty<B, V> {
  override fun getValue(thisRef: B, property: KProperty<*>): V {
    return thisRef.get(thisRef.data[id ?: property.name] ?: default)
  }

  override fun setValue(thisRef: B, property: KProperty<*>, value: V) {
    thisRef.data[id ?: property.name] = thisRef.set(value)
  }
}

var <T> PropertyBuilder<T>.enum by PropertyDelegate(
  default = JsonArray(listOf()),
  get = { globalJson.decodeFromJsonElement(ListSerializer(serializer), it) },
  set = { globalJson.encodeToJsonElement(ListSerializer(serializer), it) }
)

var <T> PropertyBuilder<T>.const by PropertyDelegate(
  default = JsonNull,
  get = { globalJson.decodeFromJsonElement(serializer, it) },
  set = { globalJson.encodeToJsonElement(serializer, it) }
)

var <T> PropertyBuilder<T>.description by PropertyDelegate(
  default = JsonPrimitive(""),
  get = { globalJson.decodeFromJsonElement(String.serializer(), it) },
  set = { globalJson.encodeToJsonElement(String.serializer(), it) }
)

var StringPropertyBuilder.pattern by PropertyDelegate(
  default = JsonPrimitive(""),
  get = { globalJson.decodeFromJsonElement(serializer, it).toRegex() },
  set = { globalJson.encodeToJsonElement(serializer, it.toString()) }
)

var <T> PropertyBuilder<T>.default by PropertyDelegate(
  default = JsonNull,
  get = { globalJson.decodeFromJsonElement(serializer, it) },
  set = { globalJson.encodeToJsonElement(serializer, it) }
)

var <T : Number> NumberPropertyBuilder<T>.minimum by PropertyDelegate(
  default = JsonPrimitive(0),
  get = { globalJson.decodeFromJsonElement(serializer, it) },
  set = { globalJson.encodeToJsonElement(serializer, it) }
)

var <T : Number> NumberPropertyBuilder<T>.maximum by PropertyDelegate(
  default = JsonPrimitive(0),
  get = { globalJson.decodeFromJsonElement(serializer, it) },
  set = { globalJson.encodeToJsonElement(serializer, it) }
)

@ExperimentalJsonSchemaDSL
var <T, B : PropertyBuilder<T>> ArrayPropertyBuilder<T, B>.minItems by PropertyDelegate(
  default = JsonPrimitive(0),
  get = { globalJson.decodeFromJsonElement(Int.serializer(), it) },
  set = { globalJson.encodeToJsonElement(Int.serializer(), it) }
)

@ExperimentalJsonSchemaDSL
var <T, B : PropertyBuilder<T>> ArrayPropertyBuilder<T, B>.maxItems by PropertyDelegate(
  default = JsonPrimitive(0),
  get = { globalJson.decodeFromJsonElement(Int.serializer(), it) },
  set = { globalJson.encodeToJsonElement(Int.serializer(), it) }
)

@ExperimentalJsonSchemaDSL
inline fun <T, B : PropertyBuilder<T>> ArrayPropertyBuilder<T, B>.items(builder: B.() -> Unit) {
  data["items"] = buildProperty(itemType, builder)
}

@ExperimentalJsonSchemaDSL
inline fun CommonObjectBuilder<*>.additionalProperties(value: Boolean) {
  data["additionalProperties"] = JsonPrimitive(value)
}

@ExperimentalJsonSchemaDSL
inline fun <T, B : PropertyBuilder<T>> CommonObjectBuilder<*>.additionalProperties(
  type: PropertyType<T, B>,
  builder: B.() -> Unit
) {
  data["additionalProperties"] = buildProperty(type, builder)
}

@ExperimentalJsonSchemaDSL
inline fun <T, B : PropertyBuilder<T>> ObjectMapBuilder<T, B>.additionalProperties(
  builder: B.() -> Unit
) {
  data["additionalProperties"] = buildProperty(valueType, builder)
}

@ExperimentalJsonSchemaDSL
inline fun CommonObjectBuilder<*>.propertyNames(
  builder: StringPropertyBuilder.() -> Unit
) {
  data["propertyNames"] = buildProperty(PropertyType.String, builder)
}

