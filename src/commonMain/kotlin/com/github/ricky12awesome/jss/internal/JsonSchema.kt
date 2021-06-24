package com.github.ricky12awesome.jss.internal

import com.github.ricky12awesome.jss.JsonSchema
import com.github.ricky12awesome.jss.JsonSchema.*
import com.github.ricky12awesome.jss.JsonSchema.IntRange
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
      it["additionalProperties"] = JsonPrimitive(false)
    }

    if (required.isNotEmpty()) {
      it["required"] = JsonArray(required)
    }
  }
}

internal fun SerialDescriptor.jsonSchemaObjectMap(definitions: JsonSchemaDefinitions): JsonObject {
  return jsonSchemaElement(annotations, skipNullCheck = false) { it ->
    val (key, value) = elementDescriptors.toList()

    require(key.kind == PrimitiveKind.STRING) {
      "cannot have non string keys in maps, ${this.serialName} $key $value"
    }

    val filteredAnnotation = annotations.filter { annotation ->
      when(annotation) {
        is Description, is Definition, is NoDefinition -> false
        else -> true
      }
    }

    it["additionalProperties"] = value.createJsonSchema(getElementAnnotations(1) + filteredAnnotation, definitions)
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

  return jsonSchemaElement(annotations, skipNullCheck = true, skipTypeCheck = true) {
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

    val filteredAnnotation = annotations.filter { annotation ->
      when(annotation) {
        is Description, is Definition, is NoDefinition -> false
        else -> true
      }
    }

    it["items"] = type.createJsonSchema(getElementAnnotations(0) + filteredAnnotation, definitions)
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
  val combinedAnnotations = annotations + this.annotations
  val key = JsonSchemaDefinitions.Key(this, combinedAnnotations)

  return when (kind.jsonType) {
    JsonType.NUMBER -> definitions.get(key) { jsonSchemaNumber(combinedAnnotations) }
    JsonType.STRING -> definitions.get(key) { jsonSchemaString(combinedAnnotations) }
    JsonType.BOOLEAN -> definitions.get(key) { jsonSchemaBoolean(combinedAnnotations) }
    JsonType.ARRAY -> definitions.get(key) { jsonSchemaArray(combinedAnnotations, definitions) }
    JsonType.OBJECT -> definitions.get(key) { jsonSchemaObject(definitions) }
    JsonType.OBJECT_MAP -> definitions.get(key) { jsonSchemaObjectMap(definitions) }
    JsonType.OBJECT_SEALED -> definitions.get(key) { jsonSchemaObjectSealed(definitions) }
  }
}

@PublishedApi
internal fun JsonObjectBuilder.applyJsonSchemaDefaults(
  descriptor: SerialDescriptor,
  annotations: List<Annotation>,
  skipNullCheck: Boolean = false,
  skipTypeCheck: Boolean = false
) {
  if (descriptor.isNullable && !skipNullCheck) {
    this["if"] = buildJson {
      it["type"] = descriptor.jsonLiteral
    }
    this["else"] = buildJson {
      it["type"] = "null"
    }
  } else {
    if (!skipTypeCheck) {
      this["type"] = descriptor.jsonLiteral
    }
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
  skipTypeCheck: Boolean = false,
  applyDefaults: Boolean = true,
  extra: (JsonObjectBuilder) -> Unit = {}
): JsonObject {
  return buildJson {
    if (applyDefaults) {
      it.applyJsonSchemaDefaults(this, annotations, skipNullCheck, skipTypeCheck)
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

internal class JsonSchemaDefinitions(private val isEnabled: Boolean = true) {
  private val definitions: MutableMap<String, JsonObject> = mutableMapOf()
  private val creator: MutableMap<String, () -> JsonObject> = mutableMapOf()

  fun getId(key: Key): String {
    val (descriptor, annotations) = key

    return annotations
      .lastOfInstance<Definition>()?.id
      ?.takeIf(String::isNotEmpty)
      ?: (descriptor.hashCode().toLong() shl 32 xor annotations.hashCode().toLong())
        .toString(36)
        .replaceFirst("-", "x")
  }

  fun canGenerateDefinitions(key: Key): Boolean {
    return key.annotations.any {
      it !is JsonSchema.NoDefinition && it is Definition
    }
  }

  operator fun contains(key: Key): Boolean = getId(key) in definitions

  operator fun set(key: Key, value: JsonObject) {
    definitions[getId(key)] = value
  }

  operator fun get(key: Key): JsonObject {
    val id =  getId(key)

    return key.descriptor.jsonSchemaElement(key.annotations, skipNullCheck = true, skipTypeCheck = true) {
      it["\$ref"] = "#/definitions/$id"
    }
  }

  fun get(key: Key, create: () -> JsonObject): JsonObject {
    if (!isEnabled && !canGenerateDefinitions(key)) return create()

    val id = getId(key)

    if (id !in definitions) {
      println("adding definition callback '$id'")
      creator[id] = create
    }

    return get(key)
  }

  fun getDefinitionsAsJsonObject(): JsonObject {
    while (creator.keys != definitions.keys) {
      creator.filter { (id, create) ->
        id !in definitions
      }.forEach { (id, create) ->
        println("creating definition '$id'")
        definitions[id] = create()
//        creator.remove(id)
      }
    }

    return JsonObject(definitions)
  }

  data class Key(val descriptor: SerialDescriptor, val annotations: List<Annotation>)
}