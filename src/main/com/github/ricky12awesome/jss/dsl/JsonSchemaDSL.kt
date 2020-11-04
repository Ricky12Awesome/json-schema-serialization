package com.github.ricky12awesome.jss.dsl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import com.github.ricky12awesome.jss.JsonType
import com.github.ricky12awesome.jss.buildJsonSchema
import com.github.ricky12awesome.jss.merge
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalJsonSchemaDSL

@ExperimentalJsonSchemaDSL
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class JsonSchemaApply(val custom: KClass<out JsonSchemaCustom>)

@ExperimentalJsonSchemaDSL
interface JsonSchemaCustom {
  val build: JsonSchemaBuilder<*>.() -> Unit
}

@ExperimentalJsonSchemaDSL
val KClass<*>.jsonType: JsonType
  get() = when (this) {
    List::class -> JsonType.ARRAY
    Byte::class, Short::class, Int::class, Long::class,
    Float::class, Double::class, Number::class -> JsonType.NUMBER
    String::class, Char::class, Enum::class -> JsonType.STRING
    Boolean::class -> JsonType.BOOLEAN
    else -> JsonType.OBJECT
  }

@ExperimentalJsonSchemaDSL
class JsonSchemaBuilder<T>(serializer: KSerializer<T>) : JsonSchemaPropertyBuilder<T>(JsonType.OBJECT, serializer) {
  private val baseSchema = buildJsonSchema(serializer)
  private val properties = mutableMapOf<String, JsonElement>()
  private val required = mutableSetOf<JsonElement>()

  init {
    (baseSchema.get("properties") as? JsonObject )?.let {
      properties += it
    }

    baseSchema["required"]?.let {
      required.addAll(it as? JsonArray ?: emptyList())
    }
  }

  fun rawProperty(name: String, isRequired: Boolean? = null, value: JsonObject) {
    properties.merge(name, value)

    if (isRequired != null) {
      val nameElement = JsonPrimitive(name)
      val hasRequired = nameElement in required

      if (isRequired && !hasRequired) {
        required += nameElement
      }

      if (!isRequired && hasRequired) {
        required -= nameElement
      }
    }
  }

  inline fun <reified T> property(
    name: String,
    serializer: KSerializer<T>,
    isRequired: Boolean? = null,
    builder: JsonSchemaPropertyBuilder<T>.() -> Unit
  ) {
    rawProperty(name, isRequired, JsonSchemaPropertyBuilder(T::class.jsonType, serializer).apply(builder).build())
  }

  inline fun <reified T> property(
    name: String,
    isRequired: Boolean? = null,
    builder: JsonSchemaPropertyBuilder<T>.() -> Unit
  ) {
    rawProperty(
      name, isRequired, JsonSchemaPropertyBuilder(T::class.jsonType, serializer<T>()).apply(builder).build()
    )
  }

  inline fun <reified T> property(
    property: KProperty<T>,
    isRequired: Boolean? = null,
    builder: JsonSchemaPropertyBuilder<T>.() -> Unit
  ) {
    property(property.name, isRequired, builder)
  }

  inline fun <reified T> propertyObject(
    name: String,
    serializer: KSerializer<T>,
    isRequired: Boolean? = null,
    builder: JsonSchemaBuilder<T>.() -> Unit
  ) {
    rawProperty(name, isRequired, buildJsonSchema(serializer, builder))
  }

  inline fun <reified T> propertyObject(
    name: String,
    isRequired: Boolean? = null,
    builder: JsonSchemaBuilder<T>.() -> Unit
  ) {
    rawProperty(name, isRequired, buildJsonSchema(serializer(), builder))
  }

  inline fun <reified T> propertyObject(
    property: KProperty<T>,
    isRequired: Boolean? = null,
    builder: JsonSchemaBuilder<T>.() -> Unit
  ) {
    propertyObject(property.name, isRequired, builder)
  }

  override fun build(): JsonObject {
    contents["properties"] = JsonObject(properties)

    if (required.isNotEmpty()) {
      contents["required"] = JsonArray(required.toList())
    }

    return super.build()
  }
}

@ExperimentalJsonSchemaDSL
inline fun buildJsonSchema(build: JsonSchemaBuilder<JsonObject>.() -> Unit): JsonObject {
  return JsonSchemaBuilder(JsonObject.serializer()).apply(build).build()
}

@ExperimentalJsonSchemaDSL
inline fun <T> buildJsonSchema(serializer: KSerializer<T>, build: JsonSchemaBuilder<T>.() -> Unit): JsonObject {
  return JsonSchemaBuilder(serializer).apply(build).build()
}

@ExperimentalJsonSchemaDSL
inline fun <reified T> buildJsonSchemaOf(build: JsonSchemaBuilder<T>.() -> Unit): JsonObject {
  return JsonSchemaBuilder(serializer<T>()).apply(build).build()
}