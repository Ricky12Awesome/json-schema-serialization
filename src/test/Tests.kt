import com.github.ricky12awesome.jss.JsonSchema.*
import com.github.ricky12awesome.jss.JsonSchema.IntRange
import com.github.ricky12awesome.jss.encodeToSchema
import com.github.ricky12awesome.jss.globalJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Serializable
data class Config(
  @Description(["Name for this project."])
  val name: String = "",
  @Description(["Theme for this project."])
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
    @IntRange(0, 255) val r: Int,
    @IntRange(0, 255) val g: Int,
    @IntRange(0, 255) val b: Int
  ) : ThemeColor()

  @Serializable @SerialName("HSV")
  data class HSV(
    @IntRange(1, 360) val h: Int,
    @FloatRange(0.0, 1.0) val s: Double,
    @FloatRange(0.0, 1.0) val v: Double
  ) : ThemeColor()

  @Serializable @SerialName("HSL")
  data class HSL(
    @IntRange(1, 360) val h: Int,
    @FloatRange(0.0, 1.0) val s: Double,
    @FloatRange(0.0, 1.0) val l: Double
  ) : ThemeColor()
}

@Serializable
data class Theme(
  @Description(["Primary color for this theme."]) @Definition("ThemeColor")
  val primary: ThemeColor = ThemeColor.RGB(128, 128, 128),
  @Description(["Secondary color for this theme."]) @Definition("ThemeColor")
  val secondary: ThemeColor = ThemeColor.HSV(0, 0.0, 0.3),
  @Description(["Accent color for this theme."]) @Definition("ThemeColor")
  val accent: ThemeColor = ThemeColor.HSL(0, 0.0, 0.8),
  @Description(["Background color for this theme."]) @Definition("ThemeColor")
  val background: ThemeColor = ThemeColor.HEX("#242424")
)

class Tests {
  val json = globalJson

  @Test
  fun `annotated schema`() {
    println(json.encodeToSchema(Config.serializer(), false))
  }

  @Test
  fun `schema dsl`() {

  }
}
