package me.github.ricky12awesome.jss

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonObject
import me.github.ricky12awesome.jss.internal.jsonSchemaObject

@Target()
annotation class JsonSchema {
  @SerialInfo
  @Repeatable
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class Description(val lines: Array<out String>)

  @SerialInfo
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class StringEnum(val values: Array<out String>)

  @SerialInfo
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class IntRange(val min: Long, val max: Long)

  @SerialInfo
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class FloatRange(val min: Double, val max: Double)

  @SerialInfo
  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class Pattern(val pattern: String)
}


fun <T> Json.stringifyWithSchema(serializer: SerializationStrategy<T>, value: T, url: String): String {
  val json = toJson(serializer, value) as JsonObject
  val append = mapOf("\$schema" to JsonLiteral(url))

  return stringify(JsonObject.serializer(), JsonObject(append + json))
}

fun jsonSchema(serializer: KSerializer<*>) = jsonSchema(serializer.descriptor)

fun jsonSchema(descriptor: SerialDescriptor): JsonObject {
  val append = mapOf("\$schema" to JsonLiteral("http://json-schema.org/draft-07/schema"))

  return JsonObject(append + descriptor.jsonSchemaObject())
}







