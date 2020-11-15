@file:OptIn(ExperimentalSerializationApi::class)
package com.github.ricky12awesome.jss

import com.github.ricky12awesome.jss.internal.JsonSchemaDefinitions
import com.github.ricky12awesome.jss.internal.createJsonSchema
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
 * Represents the type of a json type
 */
enum class JsonType(jsonType: String) {
  /**
   * Represents the json array type
   */
  ARRAY("array"),

  /**
   * Represents the json number type
   */
  NUMBER("number"),

  /**
   * Represents the string type
   */
  STRING("string"),

  /**
   * Represents the boolean type
   */
  BOOLEAN("boolean"),

  /**
   * Represents the object type, this is used for serializing normal classes
   */
  OBJECT("object"),

  /**
   * Represents the object type, this is used for serializing sealed classes
   */
  OBJECT_SEALED("object"),

  /**
   * Represents the object type, this is used for serializing maps
   */
  OBJECT_MAP("object");

  val json = JsonPrimitive(jsonType)

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

  /**
   * Should this property be a definition and be referenced using [id]?
   *
   * @param id The id for this definition, this will be referenced by '#/definitions/$[id]'
   */
  @SerialInfo
  @Retention(AnnotationRetention.BINARY)
  @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class Definition(val id: String)

  /**
   * This property will not create definitions
   */
  @SerialInfo
  @Retention(AnnotationRetention.BINARY)
  @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
  annotation class NoDefinition
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
 *
 * @param generateDefinitions Should this generate definitions by default
 */
fun Json.encodeToSchema(descriptor: SerialDescriptor, generateDefinitions: Boolean = true): String {
  return encodeToString(JsonObject.serializer(), buildJsonSchema(descriptor, generateDefinitions))
}

@Deprecated(
  message = "Use encodeToSchema instead",
  replaceWith = ReplaceWith("this.encodeToSchema(serializer)"),
  level = DeprecationLevel.ERROR
)
fun Json.stringifyToSchema(serializer: SerializationStrategy<*>): String = encodeToSchema(serializer)

/**
 * Stringifies the provided [serializer] with [buildJsonSchema], same as doing
 * ```kotlin
 * json.encodeToSchema(serializer.descriptor)
 * ```
 * @param generateDefinitions Should this generate definitions by default
 */
fun Json.encodeToSchema(serializer: SerializationStrategy<*>, generateDefinitions: Boolean = true): String {
  return encodeToSchema(serializer.descriptor, generateDefinitions)
}

/**
 * Creates a Json Schema using the provided [descriptor]
 *
 * @param generateDefinitions Should this generate definitions by default
 */
fun buildJsonSchema(descriptor: SerialDescriptor, generateDefinitions: Boolean = true): JsonObject {
  val prepend = mapOf("\$schema" to JsonPrimitive("http://json-schema.org/draft-07/schema"))
  val definitions = JsonSchemaDefinitions(generateDefinitions)
  val root = descriptor.createJsonSchema(descriptor.annotations, definitions)
  val append = mapOf("definitions" to definitions.getDefinitionsAsJsonObject())

  return JsonObject(prepend + root + append)
}

/**
 * Creates a Json Schema using the provided [serializer],
 * same as doing `jsonSchema(serializer.descriptor)`
 *
 * @param generateDefinitions Should this generate definitions by default
 */
fun buildJsonSchema(serializer: SerializationStrategy<*>, generateDefinitions: Boolean = true): JsonObject {
  return buildJsonSchema(serializer.descriptor, generateDefinitions)
}
