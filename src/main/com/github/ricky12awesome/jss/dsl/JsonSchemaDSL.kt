package com.github.ricky12awesome.jss.dsl

import com.github.ricky12awesome.jss.JsonType
import kotlin.reflect.KClass

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
