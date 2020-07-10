package me.github.ricky12awesome.jss

import kotlinx.serialization.json.*
import me.github.ricky12awesome.jss.internal.json
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalJsonSchemaDSL

@ExperimentalJsonSchemaDSL
val KClass<*>.jsonType: JsonType
  get() = when (this) {
    List::class -> JsonType.ARRAY
    Number::class -> JsonType.NUMBER
    String::class, Char::class, Enum::class -> JsonType.STRING
    Boolean::class -> JsonType.BOOLEAN
    else -> JsonType.OBJECT
  }

@ExperimentalJsonSchemaDSL
class JsonSchemaBuilder {
  private val properties = mutableMapOf<String, JsonObject>()
  private val required = mutableListOf<String>()

  fun rawProperty(name: String, isRequired: Boolean, property: JsonObject) {
    properties[name] = property

    if (isRequired) {
      required += name
    }
  }

  inline fun <reified R> property(
    name: String,
    isRequired: Boolean = false,
    build: JsonSchemaPropertyBuilder<R>.() -> Unit
  ) {
    rawProperty(name, isRequired, JsonSchemaPropertyBuilder<R>(R::class.jsonType).apply(build).build())
  }

  inline fun <reified R> property(
    property: KProperty<R>,
    isRequired: Boolean = false,
    build: JsonSchemaPropertyBuilder<R>.() -> Unit
  ) {
    property(property.name, isRequired, build)
  }

  fun build(): JsonObject = json {
    "properties" to JsonObject(properties)

    if (required.isNotEmpty()) {
      "required" to required
    }
  }
}

@ExperimentalJsonSchemaDSL
class JsonSchemaPropertyBuilder<R>(val type: JsonType) {
  val contents = mutableMapOf<String, JsonElement>("type" to type.json)
  var description by JsonSchemaProperty("", JsonElement::content, ::JsonPrimitive)

  fun build() = JsonObject(contents)
}

@ExperimentalJsonSchemaDSL
var JsonSchemaPropertyBuilder<String>.enum by JsonSchemaProperty(
  default = listOf(),
  get = { it.jsonArray.map(JsonElement::content) },
  set = { JsonArray(it.map(::JsonPrimitive)) }
)

@ExperimentalJsonSchemaDSL
internal class JsonSchemaProperty<T>(
  private val default: T,
  private val get: (JsonElement) -> T,
  private val set: (T) -> JsonElement
) : ReadWriteProperty<JsonSchemaPropertyBuilder<*>, T> {
  override fun getValue(thisRef: JsonSchemaPropertyBuilder<*>, property: KProperty<*>): T {
    return thisRef.contents[property.name]?.let(get) ?: default
  }

  override fun setValue(thisRef: JsonSchemaPropertyBuilder<*>, property: KProperty<*>, value: T) {
    thisRef.contents[property.name] = set(value)
  }
}

@ExperimentalJsonSchemaDSL
inline fun jsonSchema(build: JsonSchemaBuilder.() -> Unit): JsonObject {
  return JsonSchemaBuilder().apply(build).build()
}