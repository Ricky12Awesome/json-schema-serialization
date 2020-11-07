import com.github.ricky12awesome.jss.JsonSchema
import com.github.ricky12awesome.jss.buildJsonSchema
import com.github.ricky12awesome.jss.encodeToSchema
import com.github.ricky12awesome.jss.globalJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

@Serializable
sealed class TestSealedNested(
  val elements: Map<String, TestSealedNested> = mapOf()
) {
  @Serializable
  @SerialName("A")
  data class A(val text: String) : TestSealedNested()

  @Serializable
  @SerialName("B")
  data class B(val number: Double) : TestSealedNested()
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
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "sealedList": {
                "type": "array",
                "items": {
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
                  ],
                  "required": [
                    "type"
                  ]
                }
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
            ],
            "required": [
              "type"
            ]
          },
          "mapToInt": {
            "type":"object",
            "additionalProperties": {
              "type": "number"
            }
          },
          "mapToString": {
            "type":"object",
            "additionalProperties": {
              "type": "string"
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

    println(json.encodeToString(JsonObject.serializer(), schema))

    assertEquals(schemaAsText.let(json::parseToJsonElement), schema)
  }

  @Test
  fun `test sealed`() {
    println(json.encodeToSchema(TestSealed.serializer()))
  }

  @Test
  fun `test sealed nested`() {
    println(json.encodeToSchema(TestSealedNested.serializer()))
  }

  @Test
  fun `test nested`() {
    println(json.encodeToSchema(TestDataNested.serializer()))
  }
}
