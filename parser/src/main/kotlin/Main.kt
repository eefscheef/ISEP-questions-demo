package ut.isep

import java.io.File

fun main() {
    val rootDir = File(".") // Parent directory of `parser`

    // Filter topic directories: skip `parser`
    val topicDirs = rootDir.listFiles {
        file -> file.isDirectory &&
            file.name != "parser" &&
            !file.name.startsWith(".")
    } ?: emptyArray()

    val parser = QuestionParser()

    // Process each topic directory
    for (topicDir in topicDirs) {
        val topicName = topicDir.name
        val mdFiles = topicDir.listFiles { file -> file.extension == "md" } ?: emptyArray()

        println("Topic: $topicName")
        for (mdFile in mdFiles) {
            val content = mdFile.readText()
            val questions = parser.parseQuestions(content)

            println("Questions in ${mdFile.name}:")
            for (question in questions) {
                println(question)
            }
        }
        println("=".repeat(50)) // Separator for readability
    }
}
