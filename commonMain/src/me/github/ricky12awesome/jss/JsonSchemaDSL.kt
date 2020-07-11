package me.github.ricky12awesome.jss

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import me.github.ricky12awesome.jss.internal.json
import kotlin.math.max
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalJsonSchemaDSL

@ExperimentalJsonSchemaDSL
val KClass<*>.jsonType: JsonType
  get() = when (this) {
    List::class -> JsonType.ARRAY
    Byte::class, Short::class, Int::class, Long::class,
    Float::class, Double::class, Number::class -> JsonType.NUMBER
    String::class, Char::class, Enum::class -> JsonType.STRING
    Boolean::class -> JsonType.BOOLEAN
    else -> JsonType.OBJECT
  }

@ExperimentalJsonSchemaDSL
class JsonSchemaBuilder {
  private val properties = mutableMapOf<String, JsonObject>()
  private val required = mutableListOf<String>()

  fun rawProperty(name: String, isRequired: Boolean, property: JsonObject) {
    properties[name] = property

    if (isRequired) {
      required += name
    }
  }

  @OptIn(ImplicitReflectionSerializer::class)
  inline fun <reified T> builderOf() = JsonSchemaPropertyBuilder(T::class.jsonType, serializer<T>())

  inline fun <reified T> property(
    name: String,
    isRequired: Boolean = false,
    build: JsonSchemaPropertyBuilder<T>.() -> Unit
  ) {
    rawProperty(name, isRequired, builderOf<T>().apply(build).build())
  }

  inline fun <reified T> property(
    property: KProperty<T>,
    isRequired: Boolean = false,
    build: JsonSchemaPropertyBuilder<T>.() -> Unit
  ) {
    property(property.name, isRequired, build)
  }

  fun build(): JsonObject = json {
    "properties" to JsonObject(properties)

    if (required.isNotEmpty()) {
      "required" to required
    }
  }
}

@ExperimentalJsonSchemaDSL
var <T> JsonSchemaPropertyBuilder<T>.minimum: T?
    where T : Comparable<T>, T : Number
    by JsonSchemaPropertyElement(
      default = null,
      get = { it.doubleOrNull as T? },
      set = ::JsonPrimitive
    )

@ExperimentalJsonSchemaDSL
var <T> JsonSchemaPropertyBuilder<T>.maximum: T?
    where T : Comparable<T>, T : Number
    by JsonSchemaPropertyElement(
      default = null,
      get = { it.doubleOrNull as T? },
      set = ::JsonPrimitive
    )

@ExperimentalJsonSchemaDSL
var <T> JsonSchemaPropertyBuilder<T>.range: ClosedRange<T>?
    where T : Comparable<T>, T : Number
  get() = minimum?.let { min ->
    maximum?.let { max ->
      min.rangeTo(max)
    }
  }
  set(value) {
    minimum = value?.start
    maximum = value?.endInclusive
  }


@ExperimentalJsonSchemaDSL
class JsonSchemaPropertyBuilder<T>(
  type: JsonType,
  private val serializer: KSerializer<T>
) {
  private val json get() = Json(JsonConfiguration.Stable)
  val contents: MutableMap<String, JsonElement> = mutableMapOf("type" to type.json)

  var description by JsonSchemaPropertyElement("", JsonElement::content, ::JsonPrimitive)

  var enum by JsonSchemaPropertyElement<List<T>>(
    default = listOf(),
    get = { json.fromJson(ListSerializer(serializer), it) },
    set = { json.toJson(ListSerializer(serializer), it) }
  )

  var default by JsonSchemaPropertyElement<T?>(
    default = null,
    get = { json.fromJson(serializer, it) },
    set = { it?.let { json.toJson(serializer, it) } ?: JsonNull }
  )

  fun build() = JsonObject(contents)
}

@ExperimentalJsonSchemaDSL
internal class JsonSchemaPropertyElement<T>(
  private val default: T,
  private val get: (JsonElement) -> T,
  private val set: (T) -> JsonElement
) : ReadWriteProperty<JsonSchemaPropertyBuilder<*>, T> {
  override fun getValue(thisRef: JsonSchemaPropertyBuilder<*>, property: KProperty<*>): T {
    return thisRef.contents[property.name]?.let(get) ?: default
  }

  override fun setValue(thisRef: JsonSchemaPropertyBuilder<*>, property: KProperty<*>, value: T) {
    val element = set(value)

    if (!element.isNull ||
      (element is JsonArray && element.isNotEmpty()) ||
      (element is JsonObject && element.isNotEmpty())
    ) {
      thisRef.contents[property.name] = element
    }
  }
}

@ExperimentalJsonSchemaDSL
inline fun jsonSchema(build: JsonSchemaBuilder.() -> Unit): JsonObject {
  return JsonSchemaBuilder().apply(build).build()
}