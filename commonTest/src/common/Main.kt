package common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import me.github.ricky12awesome.jss.JsonSchema
import me.github.ricky12awesome.jss.stringifyToSchema
import me.github.ricky12awesome.jss.stringifyWithSchema
import writeTextToFile

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

fun main() {
  val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))
  val serializedSchema = json.stringifyToSchema(Test.serializer())
  val serializedJson = json.stringifyWithSchema(Test.serializer(), Test(), "test.schema.json")

  writeTextToFile("testOutput/test.json", serializedJson)
  writeTextToFile("testOutput/test.schema.json", serializedSchema)
}
