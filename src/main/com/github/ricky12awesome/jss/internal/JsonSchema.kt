@file:OptIn(ExperimentalSerializationApi::class)

package com.github.ricky12awesome.jss.internal

import com.github.ricky12awesome.jss.JsonSchema.Description
import com.github.ricky12awesome.jss.JsonSchema.StringEnum
import com.github.ricky12awesome.jss.JsonSchema.IntRange
import com.github.ricky12awesome.jss.JsonSchema.FloatRange
import com.github.ricky12awesome.jss.JsonSchema.Pattern
import com.github.ricky12awesome.jss.JsonType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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

  elementDescriptors.forEachIndexed { index, child ->
    val name = getElementName(index)
    val annotations = getElementAnnotations(index)

    properties[name] = child.jsonSchemaFor(annotations)

    if (!isElementOptional(index)) {
      required += JsonPrimitive(name)
    }
  }

  return jsonSchemaElement(annotations) {
    if (properties.isNotEmpty()) {
      this["properties"] = JsonObject(properties)
    }

    if (required.isNotEmpty()) {
      this["required"] = JsonArray(required)
    }
  }
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaArray(annotations: List<Annotation> = listOf()): JsonObject {
  return jsonSchemaElement(annotations)
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaString(annotations: List<Annotation> = listOf()): JsonObject {
  return jsonSchemaElement(annotations) {
    val pattern = annotations.lastOfInstance<Pattern>()?.pattern ?: ""
    val enum = annotations.lastOfInstance<StringEnum>()?.values ?: arrayOf()

    if (pattern.isNotEmpty()) {
      this["pattern"] = pattern
    }

    if (enum.isNotEmpty()) {
      this["enum"] = enum.toList()
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
      this["minimum"] = min
      this["maximum"] = max
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
  annotations: List<Annotation>
) {
  if (descriptor.isNullable) {
    this["if"] = buildJson {
      this["type"] = descriptor.jsonLiteral
    }
    this["else"] = buildJson {
      this["type"] = "null"
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
  extra: JsonObjectBuilder.() -> Unit = {}
): JsonObject {
  return buildJson {
    applyJsonSchemaDefaults(this@jsonSchemaElement, annotations)
    extra()
  }
}

// Since built-in json dsl isn't inlined, i made my own
internal inline fun buildJson(builder: JsonObjectBuilder.() -> Unit): JsonObject {
  return JsonObject(JsonObjectBuilder().apply(builder).content)
}

// Since built-in one is internal i have to do this
internal class JsonObjectBuilder {
  internal val content: MutableMap<String, JsonElement> = linkedMapOf()

  operator fun set(key: String, value: JsonElement) = content.set(key, value)
  operator fun set(key: String, value: Iterable<String>) = set(key, JsonArray(value.map(::JsonPrimitive)))
  operator fun set(key: String, value: String?) = set(key, JsonPrimitive(value))
  operator fun set(key: String, value: Number?) = set(key, JsonPrimitive(value))
}

