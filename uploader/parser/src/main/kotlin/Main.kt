package ut.isep

import Config
import question.Question
import QuestionParser
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
            file.name != "uploader" &&
            !file.name.startsWith(".")
    } ?: emptyArray()
}

fun processMarkdownFilesInDir(parser: QuestionParser,
                              questionDir: File,
                              questionsByTag: Map<String, MutableList<Question>>) {
    val mdFiles = questionDir.listFiles { file -> file.extension == "md" } ?: emptyArray()
    println("Topic: ${questionDir.name}")

    val allQuestions: List<Question> = mdFiles.map { mdFile ->
        println("Questions in ${mdFile.name}:")
        val content = mdFile.readText()
        parser.parseQuestion(content)
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
    val config = parseConfig("../config.yaml")
    val tagToQuestions: Map<String, MutableList<Question>> = config.tagOptions.associateWith { mutableListOf() }

    val questionDirs = getQuestionDirectories(File(".."))

    val parser = QuestionParser()

    questionDirs.forEach { questionDir ->
        processMarkdownFilesInDir(parser, questionDir, tagToQuestions)
    }
}
