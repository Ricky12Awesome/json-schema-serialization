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
  implementation("com.github.Ricky12Awesome:json-schema-serialization:0.6.4")
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
  val sealed: TestSealed? = null,
  val mapToInt: Map<String, Int> = mapOf(),
  val mapToString: Map<String, String> = mapOf()
)

@Serializable
data class TestDataNested(
  val list: List<String>,
  val sealedList: List<TestSealed> = listOf(),
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

Definition names is defined by hash codes
```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "$ref": "#/definitions/183o58zq3y39d",
  "definitions": {
    "1lm2l56gwqosh": {
      "type": "object",
      "properties": {
        "number": {
          "$ref": "#/definitions/18bamjjy12bk1"
        }
      },
      "required": [
        "number"
      ]
    },
    "xr8yxhep2sxt": {
      "type": "number"
    },
    "1tl79ageuufi9": {
      "type": "number"
    },
    "x1du17sdz8114v": {
      "type": "object",
      "properties": {
        "text": {
          "$ref": "#/definitions/1vq26amqu3nl"
        }
      },
      "required": [
        "text"
      ]
    },
    "1mlzhftji1bsx": {
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
          "type": "null"
        },
        {
          "$ref": "#/definitions/x1du17sdz8114v"
        },
        {
          "$ref": "#/definitions/1lm2l56gwqosh"
        }
      ],
      "required": [
        "type"
      ]
    },
    "1vq26aqksuhq": {
      "type": "string",
      "enum": [
        "First",
        "Second",
        "Third"
      ]
    },
    "183o58zq3y39d": {
      "type": "object",
      "properties": {
        "text": {
          "$ref": "#/definitions/1vq26aoiy36e"
        },
        "enum": {
          "$ref": "#/definitions/q3i1cuoc7b41"
        },
        "specialEnum": {
          "$ref": "#/definitions/1vq26aqksuhq"
        },
        "rangeInt": {
          "$ref": "#/definitions/xr8yxhq1teec"
        },
        "rangeDouble": {
          "$ref": "#/definitions/18bamjk7pmmal"
        },
        "nested": {
          "$ref": "#/definitions/9nzmu3mcary9"
        },
        "sealed": {
          "$ref": "#/definitions/1mlzhftji1bsx"
        },
        "mapToInt": {
          "$ref": "#/definitions/xu5yz9hf9k933"
        },
        "mapToString": {
          "$ref": "#/definitions/x1q1hw0o77sydb"
        }
      },
      "required": [
        "text",
        "enum",
        "specialEnum"
      ]
    },
    "1vq26aoiy36e": {
      "type": "string",
      "description": "Line 1\nLine 2"
    },
    "xr8yxhq1teec": {
      "type": "number",
      "minimum": 0,
      "maximum": 100
    },
    "1pvlhtlq9v2m9": {
      "type": "number"
    },
    "x1q1hw0o77sydb": {
      "type": "object",
      "additionalProperties": {
        "$ref": "#/definitions/1vq26amqu3nl"
      }
    },
    "q3i1cuoc7b41": {
      "type": "string",
      "enum": [
        "A",
        "B",
        "C"
      ]
    },
    "18bamjk7pmmal": {
      "type": "number",
      "minimum": 0.0,
      "maximum": 1.0
    },
    "1vq26amqu3nl": {
      "type": "string"
    },
    "xskpehk1nvgf": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/x75g0ny0xekfz"
      }
    },
    "x134bf5ifs1z3": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/1vq26amqu3nl"
      }
    },
    "9nzmu3mcary9": {
      "if": {
        "type": "object"
      },
      "else": {
        "type": "null"
      },
      "properties": {
        "list": {
          "$ref": "#/definitions/x134bf5ifs1z3"
        },
        "sealedList": {
          "$ref": "#/definitions/xskpehk1nvgf"
        },
        "rangeFloat": {
          "$ref": "#/definitions/1pvlhtlq9v2m9"
        },
        "rangeLong": {
          "$ref": "#/definitions/1tl79ageuufi9"
        }
      },
      "required": [
        "list"
      ]
    },
    "x75g0ny0xekfz": {
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
          "$ref": "#/definitions/x1du17sdz8114v"
        },
        {
          "$ref": "#/definitions/1lm2l56gwqosh"
        }
      ],
      "required": [
        "type"
      ]
    },
    "xu5yz9hf9k933": {
      "type": "object",
      "additionalProperties": {
        "$ref": "#/definitions/xr8yxhep2sxt"
      }
    },
    "18bamjjy12bk1": {
      "type": "number"
    }
  }
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
