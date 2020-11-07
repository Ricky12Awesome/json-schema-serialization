@file:OptIn(ExperimentalSerializationApi::class)
package com.github.ricky12awesome.jss

import kotlinx.serialization.*
import com.github.ricky12awesome.jss.internal.jsonSchemaObject
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

/**
 * Global Json object for basic serialization. uses Stable Configuration.
 */
val globalJson by lazy {
  Json {
    prettyPrintIndent = "  "
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
  }
}

/**
 * Represents the type of a json property
 */
enum class JsonType(raw: String) {
  ARRAY("array"),
  NUMBER("number"),
  STRING("string"),
  BOOLEAN("boolean"),
  OBJECT("object"),
  MAP("map");

  val json = JsonPrimitive(raw)

  override fun toString(): String = json.content
}

@Target()
annotation class JsonSchema {
  /**
   * Description of this property
   */
  @SerialInfo
  @Repeatable
  @Retention(AnnotationRetention.BINARY)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class Description(val lines: Array<out String>)

  /**
   * Enum-like values for non-enum string
   */
  @SerialInfo
  @Retention(AnnotationRetention.BINARY)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class StringEnum(val values: Array<out String>)

  /**
   * Minimum and Maximum values using whole numbers
   *
   * Only works when [SerialKind] is any of
   * [PrimitiveKind.BYTE], [PrimitiveKind.SHORT], [PrimitiveKind.INT], [PrimitiveKind.LONG]
   */
  @SerialInfo
  @Retention(AnnotationRetention.BINARY)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class IntRange(val min: Long, val max: Long)

  /**
   * Minimum and Maximum values using floating point numbers
   *
   * Only works when [SerialKind] is [PrimitiveKind.FLOAT] or [PrimitiveKind.DOUBLE]
   */
  @SerialInfo
  @Retention(AnnotationRetention.BINARY)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class FloatRange(val min: Double, val max: Double)

  /**
   * [pattern] to use on this property
   *
   * Only works when [SerialKind] is [PrimitiveKind.STRING]
   */
  @SerialInfo
  @Retention(AnnotationRetention.BINARY)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class Pattern(val pattern: String)
}


@Deprecated(
  message = "Use encodeWithSchema instead",
  replaceWith = ReplaceWith("this.encodeWithSchema(serializer, value, url)"),
  level = DeprecationLevel.ERROR
)
fun <T> Json.stringifyWithSchema(serializer: SerializationStrategy<T>, value: T, url: String): String {
  return encodeWithSchema(serializer,value, url)
}

/**
 * Adds a `$schema` property with the provided [url] that points to the Json Schema,
 * this can be a File location or a HTTP URL
 *
 * This is so when you serialize your [value] it will use [url] as it's Json Schema for code completion.
 */
fun <T> Json.encodeWithSchema(serializer: SerializationStrategy<T>, value: T, url: String): String {
  val json = encodeToJsonElement(serializer, value) as JsonObject
  val append = mapOf("\$schema" to JsonPrimitive(url))

  return encodeToString(JsonObject.serializer(), JsonObject(append + json))
}

@Deprecated(
  message = "Use encodeToSchema instead",
  replaceWith = ReplaceWith("this.encodeToSchema(descriptor)"),
  level = DeprecationLevel.ERROR
)
fun Json.stringifyToSchema(descriptor: SerialDescriptor): String = encodeToSchema(descriptor)

/**
 * Stringifies the provided [descriptor] with [buildJsonSchema]
 */
fun Json.encodeToSchema(descriptor: SerialDescriptor): String {
  return encodeToString(JsonObject.serializer(), buildJsonSchema(descriptor))
}

@Deprecated(
  message = "Use encodeToSchema instead",
  replaceWith = ReplaceWith("this.encodeToSchema(serializer)"),
  level = DeprecationLevel.ERROR
)
fun Json.stringifyToSchema(serializer: SerializationStrategy<*>): String = encodeToSchema(serializer)

/**
 * Stringifies the provided [serializer] with [buildJsonSchema],
 * same as doing `json.stringifyToSchema(serializer.descriptor)`
 */
fun Json.encodeToSchema(serializer: SerializationStrategy<*>): String {
  return encodeToSchema(serializer.descriptor)
}

/**
 * Creates a Json Schema using the provided [descriptor]
 */
fun buildJsonSchema(descriptor: SerialDescriptor): JsonObject {
  val append = mapOf("\$schema" to JsonPrimitive("http://json-schema.org/draft-07/schema"))

  return JsonObject(append + descriptor.jsonSchemaObject())
}

/**
 * Creates a Json Schema using the provided [serializer],
 * same as doing `jsonSchema(serializer.descriptor)`
 */
fun buildJsonSchema(serializer: SerializationStrategy<*>) = buildJsonSchema(serializer.descriptor)
