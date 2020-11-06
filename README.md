[![Download](https://api.bintray.com/packages/ricky12awesome/github/json-schema-serialization/images/download.svg)](https://bintray.com/ricky12awesome/github/json-schema-serialization/_latestVersion)
[![](https://jitpack.io/v/Ricky12Awesome/json-schema-serialization.svg)](https://jitpack.io/#Ricky12Awesome/json-schema-serialization)

# json-schema-serialization (jss)
Adds support for Json Schema using [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

Only Supports JVM. Once Kotlin/MPP is more stable and worth-while, then I'll start using it,
but, at its current state, I don't see much benefit to support Kotlin/MPP.

### Dependency
You would need [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) setup to use this dependency
##### Gradle / Gradle Kotlin DSL

```kotlin
repositories {
  jcenter()  
  // or maven("https://dl.bintray.com/ricky12awesome/github")
}

dependencies {
  implementation("com.github.Ricky12Awesome:json-schema-serialization:0.6.1")
}
```

### Usage

```kotlin
@Serializable
enum class TestEnum { A, B, C }

@Serializable
data class TestData(
  @JsonSchema.Description(["Line 1", "Line 2"])
  val text: String,
  val enum: TestEnum,
  @JsonSchema.StringEnum(["First", "Second", "Third"])
  val specialEnum: String,
  @JsonSchema.IntRange(0, 100)
  val rangeInt: Int = 30,
  @JsonSchema.FloatRange(0.0, 1.0)
  val rangeDouble: Double = 0.5,
  val nested: TestDataNested? = null,
  val sealed: TestSealed? = null
)

@Serializable
data class TestDataNested(
  val list: List<String>,
  val rangeFloat: Float = 20f,
  val rangeLong: Long = 20L
)

@Serializable
sealed class TestSealed {
  @Serializable
  data class A(val text: String) : TestSealed()

  @Serializable
  data class B(val number: Double) : TestSealed()
}
```
Will generate a schema using

```kotlin
globalJson.encodeToSchema(Test.serializer())
```
```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "type": "object",
  "properties": {
    "text": {
      "type": "string",
      "description": "Line 1\nLine 2"
    },
    "enum": {
      "type": "string",
      "enum": [
        "A",
        "B",
        "C"
      ]
    },
    "specialEnum": {
      "type": "string",
      "enum": [
        "First",
        "Second",
        "Third"
      ]
    },
    "rangeInt": {
      "type": "number",
      "minimum": 0,
      "maximum": 100
    },
    "rangeDouble": {
      "type": "number",
      "minimum": 0.0,
      "maximum": 1.0
    },
    "nested": {
      "if": {
        "type": "object"
      },
      "else": {
        "type": "null"
      },
      "properties": {
        "list": {
          "type": "array"
        },
        "rangeFloat": {
          "type": "number"
        },
        "rangeLong": {
          "type": "number"
        }
      },
      "required": [
        "list"
      ]
    },
    "sealed": {
      "if": {
        "type": "object"
      },
      "else": {
        "type": "null"
      },
      "properties": {
        "type": {
          "type": "string",
          "enum": [
            "TestSealed.A",
            "TestSealed.B"
          ]
        }
      },
      "anyOf": [
        {
          "type": "object",
          "properties": {
            "type": {
              "const": "TestSealed.A"
            },
            "text": {
              "type": "string"
            }
          },
          "required": [
            "text"
          ]
        },
        {
          "type": "object",
          "properties": {
            "type": {
              "const": "TestSealed.B"
            },
            "number": {
              "type": "number"
            }
          },
          "required": [
            "number"
          ]
        }
      ]
    }
  },
  "required": [
    "text",
    "enum",
    "specialEnum"
  ]
}
```

### Some Features I'm thinking about adding

- Json Schema DSL
- Json Schema Data Object instead of using `JsonObject`

### Json Schema DSL
Json Schema DSL might look something like

```kotlin
val schema = jsonSchema {
  property(Test::text) { // Type-safe naming
    description = "This is some text"
    enum = listOf("A", "B", "C") // can only be used on strings
  }
  
  property(Test::int) {
    description = "I'm an int"
    range = 0..33 // only on numbers
  }
  
  property(Test::float) {
    description = "I'm a float"
    range = 0f..33f // or 0.0...33.0 for double
  }
  
  property<String>("raw") { // Raw properties
    description = "I'm a raw property"
    enum = listOf("D", "E", "F")
  }
}
```
