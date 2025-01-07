package parser

import FileParsingException
import java.io.File
import java.io.IOException

object QuestionFileHandler {

    fun readFile(file: File): String {
        if (!file.exists()) {
            throw FileParsingException("No such file", file.name)
        }
        if (!file.canRead()) {
            throw FileParsingException("Cannot read file", file.name)
        }
        return try {
            file.readText()
        } catch (e: IOException) {
            throw FileParsingException("An I/O error occurred: ${e.message}", file.name)
        }
    }

    fun split(fileName: String): Pair<String, String> {
        val input = readFile(File(fileName))
        val parts = input.split("---", limit = 3).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size != 2) {
            throw FileParsingException("Invalid question format. Must contain frontmatter and body.", fileName)
        }
        val frontmatter = parts[0]
        val body = parts[1]
        return frontmatter to body
    }

    fun getQuestionID(filename: String): Long? {
        val regex = """.*?_qid(\d+)""".toRegex()
        return regex.find(filename)?.groupValues?.get(1)?.toLongOrNull()
    }
}