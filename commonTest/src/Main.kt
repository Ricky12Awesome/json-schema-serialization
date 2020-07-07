import kotlinx.serialization.Serializable
import me.github.ricky12awesome.jss.JsonSchema

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
