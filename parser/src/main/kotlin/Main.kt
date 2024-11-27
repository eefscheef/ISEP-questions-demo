package ut.isep

import Config
import Question
import java.io.File

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory


fun parseConfig(filePath: String): Config {
    val mapper = ObjectMapper(YAMLFactory())
    return mapper.readValue(File(filePath), Config::class.java)
}

fun getQuestionDirectories(rootDir: File): Array<File> {
    return rootDir.listFiles {
        file -> file.isDirectory &&
            file.name != "parser" &&
            !file.name.startsWith(".")
    } ?: emptyArray()
}

fun processMarkdownFiles(parser: QuestionParser,
                         questionDir: File,
                         questionsByTag: Map<String, MutableList<Question>>) {
    val mdFiles = questionDir.listFiles { file -> file.extension == "md" } ?: emptyArray()
    println("Topic: ${questionDir.name}")

    val allQuestions: List<Question> = mdFiles.flatMap { mdFile ->
        println("Questions in ${mdFile.name}:")
        val content = mdFile.readText()
        parser.parseQuestions(content)
    }
    
    for (question in allQuestions) {
            println(question)
            question.tags.forEach {tag ->
                questionsByTag.get(tag)?.add(question) ?:
                throw Exception("Unknown tag provided: ${tag} in question ${question}")
            }
        }
    println("=".repeat(50)) // Separator for readability
}


fun main() {
    val config = parseConfig("config.yaml")
    val questionsByTag: Map<String, MutableList<Question>> = config.tagOptions.associateWith { mutableListOf() }

    val questionDirs = getQuestionDirectories(File("."))

    val parser = QuestionParser()

    questionDirs.forEach { questionDir ->
        processMarkdownFiles(parser, questionDir, questionsByTag)
    }
}
