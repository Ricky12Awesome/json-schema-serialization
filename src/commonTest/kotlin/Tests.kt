import com.github.ricky12awesome.jss.JsonSchema
import com.github.ricky12awesome.jss.JsonSchema.*
import com.github.ricky12awesome.jss.dsl.*
import com.github.ricky12awesome.jss.encodeToSchema
import com.github.ricky12awesome.jss.globalJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

@Serializable
data class Config(
  @Description(arrayOf("Name for this project."))
  val name: String = "",
  @Description(arrayOf("Theme for this project."))
  val theme: Theme = Theme()
)

@Serializable
sealed class ThemeColor {
  @Serializable @SerialName("HEX")
  data class HEX(
    @Pattern("#[0-9a-fA-F]{2,6}") val hex: String
  ) : ThemeColor()

  @Serializable @SerialName("RGB")
  data class RGB(
    @JsonSchema.IntRange(0, 255) val r: Int,
    @JsonSchema.IntRange(0, 255) val g: Int,
    @JsonSchema.IntRange(0, 255) val b: Int
  ) : ThemeColor()

  @Serializable @SerialName("HSV")
  data class HSV(
    @JsonSchema.IntRange(1, 360) val h: Int,
    @FloatRange(0.0, 1.0) val s: Double,
    @FloatRange(0.0, 1.0) val v: Double
  ) : ThemeColor()

  @Serializable @SerialName("HSL")
  data class HSL(
    @JsonSchema.IntRange(1, 360) val h: Int,
    @FloatRange(0.0, 1.0) val s: Double,
    @FloatRange(0.0, 1.0) val l: Double
  ) : ThemeColor()
}

@Serializable
data class Theme(
  @Description(arrayOf("Primary color for this theme."))
  @Definition("ThemeColor") val primary: ThemeColor = ThemeColor.RGB(128, 128, 128),
  @Description(arrayOf("Secondary color for this theme."))
  @Definition("ThemeColor") val secondary: ThemeColor = ThemeColor.HSV(0, 0.0, 0.3),
  @Description(arrayOf("Accent color for this theme."))
  @Definition("ThemeColor") val accent: ThemeColor = ThemeColor.HSL(0, 0.0, 0.8),
  @Description(arrayOf("Background color for this theme."))
  @Definition("ThemeColor") val background: ThemeColor = ThemeColor.HEX("#242424")
)

class Tests {
  val json = globalJson

  @Test
  fun annotated_schema() {
    println(json.encodeToSchema(Config.serializer(), false))
  }

  @Test
  @ExperimentalJsonSchemaDSL
  fun schema_dsl() {
    val schema = buildSchema {
      val data by definitions(PropertyType.Object) {
        properties {
          property("name", PropertyType.String) {
            description = "Name of something"
          }

          property("type", PropertyType.String) {
            enum = listOf("A", "B", "C")
          }
        }
      }

      properties {
        property("array", PropertyType.Array(PropertyType.Object)) {
          description = "Some array with random data"
          minItems = 1
          maxItems = 64

          items {
            reference(data)
          }
        }

        property("map", PropertyType.ObjectMap(PropertyType.Object)) {
          description = "Some map with random data"

          propertyNames {
            pattern = Regex("^(\\d*)\$")
          }

          additionalProperties {
            reference(data)
          }
        }
      }
    }

    println(json.encodeToString(JsonObject.serializer(), schema))
  }
}
