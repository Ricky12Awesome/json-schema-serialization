import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.toUtf8Bytes
import me.github.ricky12awesome.jss.stringifyToSchema
import me.github.ricky12awesome.jss.stringifyWithSchema
import java.nio.file.Files
import java.nio.file.Paths

internal fun main() {
  val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))
  val serializedSchema = json.stringifyToSchema(Test.serializer())
  val serializedJson = json.stringifyWithSchema(Test.serializer(), Test(), "test.schema.json")

  println("Testing for Kotlin/JVM")
  println("Value: $serializedJson")
  println("Schema: $serializedSchema")

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
