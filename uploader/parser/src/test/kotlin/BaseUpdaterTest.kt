package ut.isep


import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

abstract class BaseUpdaterTest {

    @TempDir
    lateinit var tempDir: Path

    protected fun createTestFile(dirname: String, filename: String, content: String? = null): File {
        val tempFile = tempDir.resolve("$dirname/$filename").toFile()
        tempFile.parentFile.mkdirs()
        tempFile.createNewFile()
        content?.let {tempFile.writeText(it)}
        return tempFile
    }
}

