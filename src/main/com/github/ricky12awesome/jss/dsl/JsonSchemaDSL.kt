package com.github.ricky12awesome.jss.dsl

import com.github.ricky12awesome.jss.JsonType
import com.github.ricky12awesome.jss.globalJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.internal.MapLikeSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSuperclassOf

private val stringTypes = listOf(String::class, Char::class, Enum::class)

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalJsonSchemaDSL

/**
 * Converts class type to json type
 */
@ExperimentalJsonSchemaDSL
val KClass<*>.jsonType: JsonType
  get() = when {
    Iterable::class.isSuperclassOf(this) -> JsonType.ARRAY
    Number::class.isSuperclassOf(this) -> JsonType.NUMBER
    Boolean::class.isSuperclassOf(this) -> JsonType.BOOLEAN
    stringTypes.any { it.isSuperclassOf(this) } -> JsonType.STRING
    isSealed -> JsonType.OBJECT_SEALED
    Map::class.isSuperclassOf(this) -> JsonType.OBJECT_MAP
    else -> JsonType.OBJECT
  }

//val JsonSchemaBuilder.ObjectMapProperty.additionalProperty

@ExperimentalJsonSchemaDSL
class JsonSchemaBuilder @PublishedApi internal constructor() {
  val definitions: MutableMap<String, JsonElement> = mutableMapOf()

  fun getReferenceUri(id: String) = "#/definitions/$id"
  fun getReference(id: String) = buildJsonObject {
    put("\$ref", getReferenceUri(id))
  }



  fun build(): JsonObject {
    return buildJsonObject {

    }
  }
}

@ExperimentalJsonSchemaDSL
inline fun buildSchemaObject(builder: JsonSchemaBuilder.() -> Unit): JsonObject {
  return JsonSchemaBuilder().apply(builder).build()
}
