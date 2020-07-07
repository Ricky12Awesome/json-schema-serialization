package me.github.ricky12awesome.jss

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonObject
import me.github.ricky12awesome.jss.internal.jsonSchemaObject

@Target()
annotation class JsonSchema {
  /**
   * Description of this property
   */
  @SerialInfo
  @Repeatable
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class Description(val lines: Array<out String>)

  /**
   * Enum-like values for non-enum string
   */
  @SerialInfo
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class StringEnum(val values: Array<out String>)

  /**
   * Minimum and Maximum values using whole numbers
   *
   * Only works when [SerialKind] is any of
   * [PrimitiveKind.BYTE], [PrimitiveKind.SHORT], [PrimitiveKind.INT], [PrimitiveKind.LONG]
   */
  @SerialInfo
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class IntRange(val min: Long, val max: Long)

  /**
   * Minimum and Maximum values using floating point numbers
   *
   * Only works when [SerialKind] is [PrimitiveKind.FLOAT] or [PrimitiveKind.DOUBLE]
   */
  @SerialInfo
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class FloatRange(val min: Double, val max: Double)

  /**
   * [pattern] to use on this property
   *
   * Only works when [SerialKind] is [PrimitiveKind.STRING]
   */
  @SerialInfo
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class Pattern(val pattern: String)
}

/**
 * Adds a `$schema` property with the provided [url] that points to the Json Schema,
 * this can be a File location or a HTTP URL
 *
 * This is so when you serialize your [value] it will use [url] as it's Json Schema for code completion.
 */
fun <T> Json.stringifyWithSchema(serializer: SerializationStrategy<T>, value: T, url: String): String {
  val json = toJson(serializer, value) as JsonObject
  val append = mapOf("\$schema" to JsonLiteral(url))

  return stringify(JsonObject.serializer(), JsonObject(append + json))
}

/**
 * Stringifies the provided [descriptor] with [jsonSchema]
 */
fun Json.stringifyToSchema(descriptor: SerialDescriptor): String {
  return stringify(JsonObject.serializer(), jsonSchema(descriptor))
}

/**
 * Stringifies the provided [serializer] with [jsonSchema],
 * same as doing `json.stringifyToSchema(serializer.descriptor)`
 */
fun Json.stringifyToSchema(serializer: SerializationStrategy<*>): String {
  return stringifyToSchema(serializer.descriptor)
}

/**
 * Creates a Json Schema using the provided [descriptor]
 */
fun jsonSchema(descriptor: SerialDescriptor): JsonObject {
  val append = mapOf("\$schema" to JsonLiteral("http://json-schema.org/draft-07/schema"))

  return JsonObject(append + descriptor.jsonSchemaObject())
}

/**
 * Creates a Json Schema using the provided [serializer],
 * same as doing `jsonSchema(serializer.descriptor)`
 */
fun jsonSchema(serializer: SerializationStrategy<*>) = jsonSchema(serializer.descriptor)
