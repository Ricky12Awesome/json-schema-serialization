import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import me.github.ricky12awesome.jss.*

@ExperimentalJsonSchemaDSL
internal fun main() {
  val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))
  val serializedSchema = json.stringifyToSchema(Test.serializer())
  val serializedJson = json.stringifyWithSchema(Test.serializer(), Test(), "test.schema.json")
  val schema = jsonSchema {
    property(Test::text) {
      description = "Testing"
      enum = listOf("Testing")
    }
  }

  println("Testing for Kotlin/JS")
  println("Value: $serializedJson")
  println("Schema: $serializedSchema")
  println("Schema DSL: ${json.stringify(JsonObject.serializer(), schema)}")
}