[![Download](https://api.bintray.com/packages/ricky12awesome/github/json-schema-serialization/images/download.svg?version=0.2) ](https://bintray.com/ricky12awesome/github/json-schema-serialization/0.2/link)

# json-schema-serialization (jss)
Adds support for Json Schema using [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

Supports JVM and JS

I'm new to Kotlin Multiplatform, so I don't know a good way to support native, this project doesn't use any platform-specific code, it's pure common code, in it's current state you can't just depend on common code with no platofrm target (maybe you can but I don't know how to do it).

### Usage
Array Literials are not supported in JS (yet), so you have to use `arrayOf` instead of `[]`

From: [commonTest/src/Main.kt](https://github.com/Ricky12Awesome/json-schema-serialization/blob/master/commonTest/src/Main.kt)
```kotlin
@Serializable
enum class TestEnum {
  A, B, C
}

@Serializable
data class Test(
  @JsonSchema.Description(arrayOf("This is text."))
  @JsonSchema.Pattern("[A-Z][a-z]+")
  val text: String = "Text",
  val list: List<String> = listOf("one", "two", "three"),
  val enum: TestEnum = TestEnum.A,
  @JsonSchema.StringEnum(arrayOf("First", "Second", "Third"))
  val specialEnum: String = "First",
  @JsonSchema.IntRange(0, 500) val int: Int = 33,
  @JsonSchema.FloatRange(0.0, 1.0) val double: Double = 0.33,
  val bool: Boolean = false,
  @JsonSchema.Description(arrayOf("Sub Testing"))
  val sub: SubTest? = null
)

@Serializable
data class SubTest(
  val required: String,
  val list: List<Double> = listOf(1.0, 2.0, 3.0)
)
```
Will generate a schema using

From: [jvmTest/src/Main.kt](https://github.com/Ricky12Awesome/json-schema-serialization/blob/master/jvmTest/src/Main.kt)
```kotlin
val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))
val serializedSchema = json.stringifyToSchema(Test.serializer())
```
`serializedSchema`:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "type": "object",
  "properties": {
    "text": {
      "type": "string",
      "description": "This is text.",
      "pattern": "[A-Z][a-z]+"
    },
    "list": {
      "type": "array"
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
    "int": {
      "type": "number",
      "minimum": 0,
      "maximum": 500
    },
    "double": {
      "type": "number",
      "minimum": 0.0,
      "maximum": 1.0
    },
    "bool": {
      "type": "boolean"
    },
    "sub": {
      "if": {
        "type": "object"
      },
      "else": {
        "type": "null"
      },
      "properties": {
        "required": {
          "type": "string"
        },
        "list": {
          "type": "array"
        }
      },
      "required": [
        "required"
      ]
    }
  },
  "required": [
  ]
}
```
