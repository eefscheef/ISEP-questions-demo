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


fun main2() {
    val input = """
        *** 
        <!-- Automatically generated ID for database reference. Do not modify!-->
        <!-- id: unique-question-id-12345!-->
        type: multiple-choice
        tags: 
        - Frontend Developer 
        - Backend Developer
        description: What is the difference between a stack and a queue?
        options: 
        - [ ] A stack is FIFO, a queue is LIFO.
        - [x] A stack is LIFO, a queue is FIFO.
        - [ ] Both are FIFO.
        - [ ] Both are LIFO.
        ***
        
        *** 
        <!-- Automatically generated ID for database reference. Do not modify!-->
        <!-- id: unique-question-id-35842!-->
        type: open
        tags: 
        - Deezend 
        - developer 
        - Reee
        description: What is the difference between a stack and a queue?
        
        ***
        """

    val questionParser = QuestionParser()
    println(questionParser.parseQuestions(input))
}