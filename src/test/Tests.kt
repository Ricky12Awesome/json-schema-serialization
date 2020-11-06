import com.github.ricky12awesome.jss.JsonSchema
import com.github.ricky12awesome.jss.buildJsonSchema
import com.github.ricky12awesome.jss.globalJson
import com.github.ricky12awesome.jss.stringifyToSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

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
  data class A(val text: String)
  data class B(val number: String)
}

class Tests {
  val json = globalJson

  @Test
  fun `annotated schema`() {
    val schema = buildJsonSchema(TestData.serializer())
    val schemaAsText = """
      {
        "${"$"}schema": "http://json-schema.org/draft-07/schema",
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
                "type": "string"
              },
              "value": {
                "type": "object"
              }
            },
            "required": [
              "type",
              "value"
            ]
          }
        },
        "required": [
          "text",
          "enum",
          "specialEnum"
        ]
      }
    """

//    println(json.encodeToString(JsonObject.serializer(), schema))

    assertEquals(schema, schemaAsText.trimIndent().let(json::parseToJsonElement))
  }
}
