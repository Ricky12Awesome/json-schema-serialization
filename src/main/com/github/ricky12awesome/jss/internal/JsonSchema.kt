package com.github.ricky12awesome.jss.internal

import com.github.ricky12awesome.jss.JsonSchema.Description
import com.github.ricky12awesome.jss.JsonSchema.StringEnum
import com.github.ricky12awesome.jss.JsonSchema.IntRange
import com.github.ricky12awesome.jss.JsonSchema.FloatRange
import com.github.ricky12awesome.jss.JsonSchema.Pattern
import com.github.ricky12awesome.jss.JsonType
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

@PublishedApi
internal inline val SerialDescriptor.jsonLiteral
  inline get() = kind.jsonType.json

@PublishedApi
internal val SerialKind.jsonType: JsonType
  get() = when (this) {
    StructureKind.LIST -> JsonType.ARRAY
    StructureKind.MAP -> JsonType.OBJECT_MAP
    PolymorphicKind.SEALED -> JsonType.OBJECT_SEALED
    PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG,
    PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> JsonType.NUMBER
    PrimitiveKind.STRING, PrimitiveKind.CHAR, SerialKind.ENUM -> JsonType.STRING
    PrimitiveKind.BOOLEAN -> JsonType.BOOLEAN
    else -> JsonType.OBJECT
  }

internal inline fun <reified T> List<Annotation>.lastOfInstance(): T? {
  return filterIsInstance<T>().lastOrNull()
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaObject(definitions: JsonSchemaDefinitions): JsonObject {
  val properties = mutableMapOf<String, JsonElement>()
  val required = mutableListOf<JsonPrimitive>()

  elementDescriptors.forEachIndexed { index, child ->
    val name = getElementName(index)
    val annotations = getElementAnnotations(index)

    properties[name] = child.createJsonSchema(annotations, definitions)

    if (!isElementOptional(index)) {
      required += JsonPrimitive(name)
    }
  }

  return jsonSchemaElement(annotations) {
    if (properties.isNotEmpty()) {
      it["properties"] = JsonObject(properties)
    }

    if (required.isNotEmpty()) {
      it["required"] = JsonArray(required)
    }
  }
}

internal fun SerialDescriptor.jsonSchemaObjectMap(definitions: JsonSchemaDefinitions): JsonObject {
  return jsonSchemaElement(annotations, skipNullCheck = false) {
    val (key, value) = elementDescriptors.toList()

    require(key.kind == PrimitiveKind.STRING) {
      "cannot have non string keys in maps"
    }

    it["additionalProperties"] = value.createJsonSchema(getElementAnnotations(1), definitions)
  }
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaObjectSealed(definitions: JsonSchemaDefinitions): JsonObject {
  val properties = mutableMapOf<String, JsonElement>()
  val required = mutableListOf<JsonPrimitive>()
  val anyOf = mutableListOf<JsonElement>()

  val (_, value) = elementDescriptors.toList()

  properties["type"] = buildJson {
    it["type"] = JsonType.STRING.json
    it["enum"] = value.elementNames
  }

  required += JsonPrimitive("type")

  if (isNullable) {
    anyOf += buildJson { nullable ->
      nullable["type"] = "null"
    }
  }

  value.elementDescriptors.forEachIndexed { index, child ->
    val schema = child.createJsonSchema(value.getElementAnnotations(index), definitions)
    val newSchema = schema.mapValues { (name, element) ->
      if (element is JsonObject && name == "properties") {
        val prependProps = mutableMapOf<String, JsonElement>()

        prependProps["type"] = buildJson {
          it["const"] = child.serialName
        }

        JsonObject(prependProps + element)
      } else {
        element
      }
    }

    anyOf += JsonObject(newSchema)
  }

  return jsonSchemaElement(annotations, skipNullCheck = true, applyDefaults = false) {
    if (properties.isNotEmpty()) {
      it["properties"] = JsonObject(properties)
    }

    if (anyOf.isNotEmpty()) {
      it["anyOf"] = JsonArray(anyOf)
    }

    if (required.isNotEmpty()) {
      it["required"] = JsonArray(required)
    }
  }
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaArray(
  annotations: List<Annotation> = listOf(),
  definitions: JsonSchemaDefinitions
): JsonObject {
  return jsonSchemaElement(annotations) {
    val type = getElementDescriptor(0)

    it["items"] = type.createJsonSchema(getElementAnnotations(0), definitions)
  }
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaString(
  annotations: List<Annotation> = listOf()
): JsonObject {
  return jsonSchemaElement(annotations) {
    val pattern = annotations.lastOfInstance<Pattern>()?.pattern ?: ""
    val enum = annotations.lastOfInstance<StringEnum>()?.values ?: arrayOf()

    if (pattern.isNotEmpty()) {
      it["pattern"] = pattern
    }

    if (enum.isNotEmpty()) {
      it["enum"] = enum.toList()
    }
  }
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaNumber(
  annotations: List<Annotation> = listOf()
): JsonObject {
  return jsonSchemaElement(annotations) {
    val value = when (kind) {
      PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> annotations
        .lastOfInstance<FloatRange>()
        ?.let { it.min as Number to it.max as Number }
      PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG -> annotations
        .lastOfInstance<IntRange>()
        ?.let { it.min as Number to it.max as Number }
      else -> error("$kind is not a Number")
    }

    value?.let { (min, max) ->
      it["minimum"] = min
      it["maximum"] = max
    }
  }
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaBoolean(
  annotations: List<Annotation> = listOf()
): JsonObject {
  return jsonSchemaElement(annotations)
}

@PublishedApi
internal fun SerialDescriptor.createJsonSchema(
  annotations: List<Annotation>,
  definitions: JsonSchemaDefinitions
): JsonObject {
  val key = JsonSchemaDefinitions.Key(this, annotations)

  return when (kind.jsonType) {
    JsonType.NUMBER -> definitions.get(key) { jsonSchemaNumber(annotations) }
    JsonType.STRING -> definitions.get(key) { jsonSchemaString(annotations) }
    JsonType.BOOLEAN -> definitions.get(key) { jsonSchemaBoolean(annotations) }
    JsonType.ARRAY -> definitions.get(key) { jsonSchemaArray(annotations, definitions) }
    JsonType.OBJECT -> definitions.get(key) { jsonSchemaObject(definitions) }
    JsonType.OBJECT_MAP -> definitions.get(key) { jsonSchemaObjectMap(definitions) }
    JsonType.OBJECT_SEALED -> definitions.get(key) { jsonSchemaObjectSealed(definitions) }
  }
}

@PublishedApi
internal fun JsonObjectBuilder.applyJsonSchemaDefaults(
  descriptor: SerialDescriptor,
  annotations: List<Annotation>,
  skipNullCheck: Boolean = false
) {
  if (descriptor.isNullable && !skipNullCheck) {
    this["if"] = buildJson {
      it["type"] = descriptor.jsonLiteral
    }
    this["else"] = buildJson {
      it["type"] = "null"
    }
  } else {
    this["type"] = descriptor.jsonLiteral
  }

  if (descriptor.kind == SerialKind.ENUM) {
    this["enum"] = descriptor.elementNames
  }

  if (annotations.isNotEmpty()) {
    val description = annotations
      .filterIsInstance<Description>()
      .joinToString("\n") {
        it.lines.joinToString("\n")
      }

    if (description.isNotEmpty()) {
      this["description"] = description
    }
  }
}

internal inline fun SerialDescriptor.jsonSchemaElement(
  annotations: List<Annotation>,
  skipNullCheck: Boolean = false,
  applyDefaults: Boolean = true,
  extra: (JsonObjectBuilder) -> Unit = {}
): JsonObject {
  return buildJson {
    if (applyDefaults) {
      it.applyJsonSchemaDefaults(this, annotations, skipNullCheck)
    }

    it.apply(extra)
  }
}

internal inline fun buildJson(builder: (JsonObjectBuilder) -> Unit): JsonObject {
  return JsonObject(JsonObjectBuilder().apply(builder).content)
}

internal class JsonObjectBuilder(
  val content: MutableMap<String, JsonElement> = linkedMapOf()
) : MutableMap<String, JsonElement> by content {
  operator fun set(key: String, value: Iterable<String>) = set(key, JsonArray(value.map(::JsonPrimitive)))
  operator fun set(key: String, value: String?) = set(key, JsonPrimitive(value))
  operator fun set(key: String, value: Number?) = set(key, JsonPrimitive(value))
}

internal class JsonSchemaDefinitions {
  private val definitions: MutableMap<String, JsonObject> = ConcurrentHashMap()
  private val creator: MutableMap<String, () -> JsonObject> = ConcurrentHashMap()

  fun getId(pair: Key): String {
    val (descriptor, annotations) = pair

    return (descriptor.hashCode().toLong() shl 32 xor annotations.hashCode().toLong())
      .toString(36)
      .replaceFirst("-", "x")
  }

  operator fun contains(descriptor: Key): Boolean = getId(descriptor) in definitions

  operator fun set(descriptor: Key, value: JsonObject) {
    definitions[getId(descriptor)] = value
  }

  operator fun get(descriptor: Key): JsonObject {
    return buildJson {
      it["\$ref"] = "#/definitions/${getId(descriptor)}"
    }
  }

  fun get(descriptor: Key, create: () -> JsonObject): JsonObject {
    val id = getId(descriptor)

    if (id !in definitions) {
      creator[id] = create
    }

    return buildJson {
      it["\$ref"] = "#/definitions/$id"
    }
  }

  fun getDefinitionsAsJsonObject(): JsonObject {
    while (creator.isNotEmpty()) {
      creator.forEach { (id, create) ->
        definitions[id] = create()
        creator.remove(id)
      }
    }

    return JsonObject(definitions)
  }

  data class Key(val descriptor: SerialDescriptor, val annotations: List<Annotation>)
}