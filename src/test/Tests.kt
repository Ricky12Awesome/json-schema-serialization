import com.github.ricky12awesome.jss.JsonSchema
import com.github.ricky12awesome.jss.buildJsonSchema
import com.github.ricky12awesome.jss.encodeToSchema
import com.github.ricky12awesome.jss.globalJson
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
  val nested: TestSubData? = null,
  val sealed: TestSealed? = null,
  val mapToInt: Map<String, Int> = mapOf(),
  val mapToString: Map<String, String> = mapOf()
)

@Serializable
data class TestSubData(
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
data class TestNested2(
  val element: TestNested1? = null
)

@Serializable
data class TestNested1(
  val element: TestNested2? = null
)

class Tests {
  val json = globalJson

  @Test
  fun `annotated schema`() {
//    println(json.encodeToSchema(TestData.serializer()))
  }

  @Test
  fun `test sealed`() {
//    println(json.encodeToSchema(TestSealed.serializer()))
  }

  @Test
  fun `test nested`() {
//    println(json.encodeToSchema(TestNested1.serializer()))
  }
}
