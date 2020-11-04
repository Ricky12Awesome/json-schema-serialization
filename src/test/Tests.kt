import com.github.ricky12awesome.jss.JsonSchema
import com.github.ricky12awesome.jss.buildJsonSchema
import com.github.ricky12awesome.jss.dsl.ExperimentalJsonSchemaDSL
import com.github.ricky12awesome.jss.dsl.buildJsonSchemaOf
import com.github.ricky12awesome.jss.dsl.minimum
import com.github.ricky12awesome.jss.dsl.range
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
  val nested: TestDataNested? = null
)

@Serializable
data class TestDataNested(
  val list: List<String>,
  val rangeFloat: Float = 20f,
  val rangeLong: Long = 20L
)

@ExperimentalSerializationApi
class Tests {
  val json = Json{
    prettyPrint = true
    prettyPrintIndent = "  "
  }

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
          }
        },
        "required": [
          "text",
          "enum",
          "specialEnum"
        ]
      }
    """

//    println(json.stringify(JsonObject.serializer(), schema))

    assertEquals(schemaAsText.trimIndent().let(json::parseToJsonElement), schema)
  }

  @Test
  @ExperimentalJsonSchemaDSL
  fun `dsl schema`() {
    val schema = buildJsonSchemaOf<TestData> {
      property(TestData::text) {
        default = "Default"
      }

      property(TestData::enum) {
        description = "Test Enum"
        default = TestEnum.A
      }

      property(TestData::rangeInt) {
        range = 0..100738
      }

      property(TestData::rangeDouble) {
        range = 0.0..65.423543
      }

      // atm, nullable properties don't work, so you just have to do this
      propertyObject("nested", TestDataNested.serializer()) {
        description = "Nested Data Property"
        default = TestDataNested(list = listOf("Test"))

        property(TestDataNested::rangeLong) {
          minimum = 1L
        }

        property(TestDataNested::rangeFloat) {
          minimum = 1F
        }
      }
    }
    val schemaAsText = """
      {
        "type": "object",
        "properties": {
          "text": {
            "type": "string",
            "description": "Line 1\nLine 2",
            "default": "Default"
          },
          "enum": {
            "type": "object",
            "enum": [
              "A",
              "B",
              "C"
            ],
            "description": "Test Enum",
            "default": "A"
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
            "maximum": 100738
          },
          "rangeDouble": {
            "type": "number",
            "minimum": 0.0,
            "maximum": 65.423543
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
                "type": "number",
                "minimum": 1.0
              },
              "rangeLong": {
                "type": "number",
                "minimum": 1
              }
            },
            "required": [
              "list"
            ],
            "type": "object",
            "description": "Nested Data Property",
            "default": {
              "list": [
                "Test"
              ],
              "rangeFloat": 20.0,
              "rangeLong": 20
            }
          }
        },
        "required": [
          "text",
          "enum",
          "specialEnum"
        ]
      }
    """

//    println(json.stringify(JsonObject.serializer(), schema))

    assertEquals(schemaAsText.trimIndent().let(json::parseToJsonElement), schema)
  }
}
