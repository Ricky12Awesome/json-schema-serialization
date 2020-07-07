import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import me.github.ricky12awesome.jss.stringifyToSchema
import me.github.ricky12awesome.jss.stringifyWithSchema

internal fun main() {
  val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))
  val serializedSchema = json.stringifyToSchema(Test.serializer())
  val serializedJson = json.stringifyWithSchema(Test.serializer(), Test(), "test.schema.json")

  println("Testing for Kotlin/JS")
  println("Value: $serializedJson")
  println("Schema: $serializedSchema")
}