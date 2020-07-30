package com.github.ricky12awesome.jss.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import com.github.ricky12awesome.jss.JsonSchema.Description
import com.github.ricky12awesome.jss.JsonSchema.StringEnum
import com.github.ricky12awesome.jss.JsonSchema.IntRange
import com.github.ricky12awesome.jss.JsonSchema.FloatRange
import com.github.ricky12awesome.jss.JsonSchema.Pattern
import com.github.ricky12awesome.jss.JsonType

@PublishedApi
internal inline val SerialDescriptor.jsonLiteral inline get() = kind.jsonType.json

@PublishedApi
internal val SerialKind.jsonType: JsonType
  get() = when (this) {
    StructureKind.LIST -> JsonType.ARRAY
    PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG,
    PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> JsonType.NUMBER
    PrimitiveKind.STRING, PrimitiveKind.CHAR, UnionKind.ENUM_KIND -> JsonType.STRING
    PrimitiveKind.BOOLEAN -> JsonType.BOOLEAN
    else -> JsonType.OBJECT
  }

internal inline fun <reified T> List<Annotation>.lastOfInstance(): T? {
  return filterIsInstance<T>().lastOrNull()
}

@PublishedApi
internal fun SerialDescriptor.jsonSchemaObject(): JsonObject {
  val properties = mutableMapOf<String, JsonElement>()
  val required = mutableListOf<JsonLiteral>()

  elementDescriptors().forEachIndexed { index, child ->
    val name = getElementName(index)
    val annotations = getElementAnnotations(index)

    properties[name] = child.jsonSchemaFor(annotations)

    if (!isElementOptional(index)) {
      required += JsonLiteral(name)
    }
  }

  return jsonSchemaElement(annotations) {
    if (properties.isNotEmpty()) {
      "properties" to JsonObject(properties)
    }

    if (required.isNotEmpty()) {
      "required" to JsonArray(required)
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
      "pattern" to pattern
    }

    if (enum.isNotEmpty()) {
      "enum" to enum.toList()
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
      "minimum" to min
      "maximum" to max
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
    "if" to buildJson {
      "type" to descriptor.jsonLiteral
    }
    "else" to buildJson {
      "type" to "null"
    }
  } else {
    "type" to descriptor.jsonLiteral
  }

  if (descriptor.kind == UnionKind.ENUM_KIND) {
    "enum" to descriptor.elementNames()
  }

  if (annotations.isNotEmpty()) {
    val description = annotations
      .filterIsInstance<Description>()
      .joinToString("\n") {
        it.lines.joinToString("\n")
      }

    if (description.isNotEmpty()) {
      "description" to description
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
  infix fun String.to(value: JsonElement) = content.set(this, value)
  infix fun String.to(value: List<String>) = to(JsonArray(value.map(::JsonPrimitive)))
  infix fun String.to(value: String?) = to(JsonPrimitive(value))
  infix fun String.to(value: Number?) = to(JsonPrimitive(value))
}

