package com.github.ricky12awesome.jss.internal

import com.github.ricky12awesome.jss.JsonSchema.Description
import com.github.ricky12awesome.jss.JsonSchema.StringEnum
import com.github.ricky12awesome.jss.JsonSchema.IntRange
import com.github.ricky12awesome.jss.JsonSchema.FloatRange
import com.github.ricky12awesome.jss.JsonSchema.Pattern
import com.github.ricky12awesome.jss.JsonType
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

@PublishedApi
internal inline val SerialDescriptor.jsonLiteral
  inline get() = kind.jsonType.json

@PublishedApi
internal val SerialKind.jsonType: JsonType
  get() = when (this) {
    StructureKind.LIST -> JsonType.ARRAY
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
internal fun SerialDescriptor.jsonSchemaObject(): JsonObject {
  val properties = mutableMapOf<String, JsonElement>()
  val required = mutableListOf<JsonPrimitive>()
  val anyOf = mutableListOf<JsonElement>()

  val isSealed = kind == PolymorphicKind.SEALED

  if (isSealed) {
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
      val schema = child.jsonSchemaFor(value.getElementAnnotations(index))
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
  } else {
    elementDescriptors.forEachIndexed { index, child ->
      val name = getElementName(index)
      val annotations = getElementAnnotations(index)

      properties[name] = child.jsonSchemaFor(annotations)

      if (!isElementOptional(index)) {
        required += JsonPrimitive(name)
      }
    }
  }

  return jsonSchemaElement(annotations, isSealed, !isSealed) {
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
internal fun SerialDescriptor.jsonSchemaArray(annotations: List<Annotation> = listOf()): JsonObject {
  return jsonSchemaElement(annotations) {
    val type = getElementDescriptor(0)

    it["items"] = type.jsonSchemaFor(getElementAnnotations(0))
  }
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaString(annotations: List<Annotation> = listOf()): JsonObject {
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
internal fun SerialDescriptor.jsonSchemaNumber(annotations: List<Annotation> = listOf()): JsonObject {
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
internal fun SerialDescriptor.jsonSchemaBoolean(annotations: List<Annotation> = listOf()): JsonObject {
  return jsonSchemaElement(annotations)
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaFor(annotations: List<Annotation> = listOf()): JsonObject {
  return when (kind.jsonType) {
    JsonType.ARRAY -> jsonSchemaArray(annotations)
    JsonType.NUMBER -> jsonSchemaNumber(annotations)
    JsonType.STRING -> jsonSchemaString(annotations)
    JsonType.BOOLEAN -> jsonSchemaBoolean(annotations)
    JsonType.OBJECT -> jsonSchemaObject()
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

// Since built-in json dsl isn't inlined, i made my own
internal inline fun buildJson(builder: (JsonObjectBuilder) -> Unit): JsonObject {
  return JsonObject(JsonObjectBuilder().apply(builder).content)
}

// Since built-in one is internal i have to do this
internal class JsonObjectBuilder(
  val content: MutableMap<String, JsonElement> = linkedMapOf()
) : MutableMap<String, JsonElement> by content {
  operator fun set(key: String, value: Iterable<String>) = set(key, JsonArray(value.map(::JsonPrimitive)))
  operator fun set(key: String, value: String?) = set(key, JsonPrimitive(value))
  operator fun set(key: String, value: Number?) = set(key, JsonPrimitive(value))
}

