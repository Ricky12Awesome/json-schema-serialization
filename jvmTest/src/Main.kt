import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.toUtf8Bytes
import me.github.ricky12awesome.jss.*
import java.nio.file.Files
import java.nio.file.Paths

@ExperimentalJsonSchemaDSL
internal fun main() {
  val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))
  val serializedSchema = json.stringifyToSchema(Test.serializer())
  val serializedJson = json.stringifyWithSchema(Test.serializer(), Test(), "test.schema.json")
  val schema = jsonSchema {
    property(Test::text) {
      description = "Testing"
      default = "whatever"
      enum = listOf("Testing")
    }

    property<Int>("test") {
      default = 3
      enum = listOf(1, 1, 1)
    }
  }

  println("Testing for Kotlin/JVM")
  println("Value: $serializedJson")
  println("Schema: $serializedSchema")
  println("Schema DSL: ${json.stringify(JsonObject.serializer(), schema)}")

//  writeTextToFile("testOutput/test.json", serializedJson)
//  writeTextToFile("testOutput/test.schema.json", serializedSchema)
}

fun writeTextToFile(file: String, text: String) {
  val path = Paths.get(file)

  if (path.parent != null && Files.notExists(path.parent)) {
    Files.createDirectories(path.parent)
  }

  if (Files.notExists(path)) {
    Files.write(path, text.toUtf8Bytes())
  } else {
    Files.newBufferedWriter(path).use {
      it.write(text)
    }
  }
}
