import java.nio.file.Files
import java.nio.file.Paths

actual fun writeTextToFile(file: String, text: String) {
  val path = Paths.get(file)

  if (path.parent != null && Files.notExists(path.parent)) {
    Files.createDirectories(path.parent)
  }

  if (Files.notExists(path)) {
    Files.writeString(path, text)
  } else {
    Files.newBufferedWriter(path).use {
      it.write(text)
    }
  }
}