package com.github.ricky12awesome.jss.dsl

import com.github.ricky12awesome.jss.JsonType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@DslMarker
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalJsonSchemaDSL

@ExperimentalJsonSchemaDSL
sealed class PropertyType<T, B : PropertyBuilder<T>>(val builder: () -> B) {
  class Number<T : kotlin.Number>(
    serializer: KSerializer<T>
  ) : PropertyType<T, NumberPropertyBuilder<T>>({ NumberPropertyBuilder(serializer) }) {
    companion object {
      inline operator fun <reified T : kotlin.Number> invoke(): Number<T> {
        return Number(serializer())
      }
    }
  }

  class Enum<E : kotlin.Enum<E>>(
    serializer: KSerializer<E>
  ) : PropertyType<E, EnumPropertyBuilder<E>>({ EnumPropertyBuilder(serializer) }) {
    companion object {
      inline operator fun <reified E : kotlin.Enum<E>> invoke(): Enum<E> = Enum(serializer())
    }
  }

  object String : PropertyType<kotlin.String, StringPropertyBuilder>(::StringPropertyBuilder)
  object Char : PropertyType<kotlin.Char, CharPropertyBuilder>(::CharPropertyBuilder)
  object Any : PropertyType<JsonElement, PropertyBuilder<JsonElement>>({ PropertyBuilder(JsonElement.serializer()) })

  class Array<T, B : PropertyBuilder<T>>(
    serializer: KSerializer<T>,
    type: PropertyType<T, B>
  ) : PropertyType<List<T>, ArrayPropertyBuilder<T, B>>({ ArrayPropertyBuilder(serializer, type) }) {
    companion object {
      inline operator fun <reified T, B : PropertyBuilder<T>> invoke(type: PropertyType<T, B>): Array<T, B> {
        return Array(serializer(), type)
      }
    }
  }

  object Object : PropertyType<JsonObject, ObjectBuilder>(::ObjectBuilder)
  object ObjectSealed : PropertyType<JsonObject, ObjectSealedBuilder>(::ObjectSealedBuilder)

  class ObjectMap<T, B : PropertyBuilder<T>>(
    serializer: KSerializer<T>,
    type: PropertyType<T, B>
  ) : PropertyType<Map<kotlin.String, T>, ObjectMapBuilder<T, B>>({ ObjectMapBuilder(serializer, type) }) {
    companion object {
      inline operator fun <reified T, B : PropertyBuilder<T>> invoke(type: PropertyType<T, B>): ObjectMap<T, B> {
        return ObjectMap(serializer(), type)
      }
    }
  }

  inline fun build(body: B.() -> Unit): JsonObject {
    return build(builder(), body)
  }

  inline fun build(builder: B, body: B.() -> Unit): JsonObject {
    return builder.run {
      body()
      build()
    }
  }

  inline fun build(definitions: ObjectBuilder.Definitions?, body: B.() -> Unit): JsonObject {
    return when (this) {
      Object -> build(ObjectBuilder(definitions, false) as B, body)
      else -> build(body)
    }
  }
}

inline class DefinitionReference(val id: String) {
  val url get() = JsonPrimitive("#/definitions/$id")
}

open class PropertyBuilder<T>(
  val serializer: KSerializer<T>,
  val type: JsonType? = null
) {
  val data: MutableMap<String, JsonElement> = mutableMapOf()

  init {
    type?.let {
      data["type"] = it.json
    }
  }

  fun reference(id: DefinitionReference) {
    data["\$ref"] = id.url
  }

  fun referenceUnsafe(id: String) {
    reference(DefinitionReference(id))
  }

  @ExperimentalJsonSchemaDSL
  open fun PropertyType<T, *>.build(): JsonObject {
    return JsonObject(data)
  }
}

class StringPropertyBuilder : PropertyBuilder<String>(String.serializer(), JsonType.STRING)
class CharPropertyBuilder : PropertyBuilder<Char>(Char.serializer(), JsonType.STRING)

class EnumPropertyBuilder<E : Enum<E>>(
  serializer: KSerializer<E>
) : PropertyBuilder<E>(serializer, JsonType.STRING)

class NumberPropertyBuilder<T : Number>(
  serializer: KSerializer<T>
) : PropertyBuilder<T>(serializer, JsonType.NUMBER)

@ExperimentalJsonSchemaDSL
class ArrayPropertyBuilder<T, B : PropertyBuilder<T>>(
  serializer: KSerializer<T>,
  val itemType: PropertyType<T, B>
) : PropertyBuilder<List<T>>(ListSerializer(serializer), JsonType.ARRAY)

open class CommonObjectBuilder<T>(serializer: KSerializer<T>, type: JsonType) : PropertyBuilder<T>(serializer, type)

@ExperimentalJsonSchemaDSL
class ObjectMapBuilder<T, B : PropertyBuilder<T>>(
  serializer: KSerializer<T>,
  val valueType: PropertyType<T, B>
) : CommonObjectBuilder<Map<String, T>>(MapSerializer(String.serializer(), serializer), JsonType.OBJECT_MAP)

class ObjectSealedBuilder : CommonObjectBuilder<JsonObject>(JsonObject.serializer(), JsonType.OBJECT_SEALED)

class ObjectBuilder(
  definitions: Definitions? = null,
  private val isRoot: Boolean = true
) : CommonObjectBuilder<JsonObject>(JsonObject.serializer(), JsonType.OBJECT) {
  val definitions = definitions ?: Definitions()
  val properties = Properties(this.definitions)

  class Properties(@PublishedApi internal val definitions: Definitions) {
    val data = mutableMapOf<String, JsonElement>()
    val required = mutableListOf<String>()

    @ExperimentalJsonSchemaDSL
    inline fun <B : PropertyBuilder<*>> property(
      name: String,
      type: PropertyType<*, B>,
      isRequired: Boolean = true,
      builder: B.() -> Unit
    ) = propertyUnsafe(name, buildProperty(type, builder), isRequired)

    @ExperimentalJsonSchemaDSL
    inline fun <B : PropertyBuilder<*>> requiredProperty(
      name: String,
      type: PropertyType<*, B>,
      builder: B.() -> Unit
    ) = property(name, type, true, builder)

    @ExperimentalJsonSchemaDSL
    inline fun <B : PropertyBuilder<*>> optionalProperty(
      name: String,
      type: PropertyType<*, B>,
      builder: B.() -> Unit
    ) = property(name, type, false, builder)

    fun propertyUnsafe(name: String, element: JsonObject, isRequired: Boolean = true) {
      data[name] = element

      if (isRequired) {
        required += name
      }
    }

    fun requiredPropertyUnsafe(name: String, element: JsonObject) = propertyUnsafe(name, element, true)
    fun optionalPropertyUnsafe(name: String, element: JsonObject) = propertyUnsafe(name, element, false)

    @ExperimentalJsonSchemaDSL
    fun propertyReference(name: String, id: DefinitionReference, isRequired: Boolean = true) {
      if (isRequired) {
        required += name
      }

      return property(name, PropertyType.Any) {
        reference(id)
      }
    }

    @ExperimentalJsonSchemaDSL
    fun propertyReferenceUnsafe(name: String, id: String, isRequired: Boolean = true) {
      if (isRequired) {
        required += name
      }

      return property(name, PropertyType.Any) {
        referenceUnsafe(id)
      }
    }

    @ExperimentalJsonSchemaDSL
    inline operator fun invoke(builder: Properties.() -> Unit): Properties {
      return apply(builder)
    }
  }

  class Definitions {
    val data = mutableMapOf<String, JsonElement>()

    @ExperimentalJsonSchemaDSL
    inline fun <T, B : PropertyBuilder<T>> definition(
      id: String,
      type: PropertyType<T, B>,
      builder: B.() -> Unit
    ): DefinitionReference {
      data[id] = type.build(this, builder)

      return DefinitionReference(id)
    }

    @ExperimentalJsonSchemaDSL
    inline operator fun invoke(builder: Definitions.() -> Unit): Definitions {
      return apply(builder)
    }

    @ExperimentalJsonSchemaDSL
    operator fun <T, B : PropertyBuilder<T>> invoke(
      type: PropertyType<T, B>,
      builder: B.() -> Unit
    ): DefinitionReferenceDelegate<T, B> {
      return DefinitionReferenceDelegate(type, builder)
    }

    @ExperimentalJsonSchemaDSL
    inner class DefinitionReferenceDelegate<T, B : PropertyBuilder<T>>(
      private val type: PropertyType<T, B>,
      private val builder: B.() -> Unit
    ) : ReadOnlyProperty<Any?, DefinitionReference> {
      override fun getValue(thisRef: Any?, property: KProperty<*>): DefinitionReference {
        if (property.name !in data) {
          definition(property.name, type, builder)
        }

        return DefinitionReference(property.name)
      }
    }
  }

  @ExperimentalJsonSchemaDSL
  override fun PropertyType<JsonObject, *>.build(): JsonObject {
    data["type"] = JsonType.OBJECT.json

    if (properties.data.isNotEmpty()) {
      data["properties"] = JsonObject(properties.data)
    }

    if (properties.required.isNotEmpty()) {
      data["required"] = JsonArray(properties.required.map(::JsonPrimitive))
    }

    if (isRoot) {
      if (definitions.data.isNotEmpty()) {
        data["definitions"] = JsonObject(definitions.data)
      }
    }

    return JsonObject(data)
  }
}

@ExperimentalJsonSchemaDSL
inline fun <T, B : PropertyBuilder<T>> buildProperty(type: PropertyType<T, B>, builder: B.() -> Unit): JsonObject {
  return type.build(builder)
}

@ExperimentalJsonSchemaDSL
inline fun buildSchema(builder: ObjectBuilder.() -> Unit): JsonObject {
  return buildProperty(PropertyType.Object, builder)
}