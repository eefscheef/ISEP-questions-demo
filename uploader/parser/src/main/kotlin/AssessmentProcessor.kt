package ut.isep

import Config
import question.Question
import QuestionParser
import java.io.File

import ut.isep.management.model.entity.Assessment
import ut.isep.management.model.entity.Section

class AssessmentProcessor(val rootDir: File, val config: Config) {


    private fun getQuestionDirectories(): Array<File> {
        return rootDir.listFiles { file ->
            file.isDirectory &&
                    file.name != "uploader" &&
                    !file.name.startsWith(".")
        } ?: emptyArray()
    }

    private fun parseMarkDownFilesInDir(
        parser: QuestionParser,
        questionDir: File
    ): Map<String, Section> {
        val topic = questionDir.name
        val mdFiles = questionDir.listFiles { file -> file.extension == "md" } ?: emptyArray()
        val questions: List<Question> = mdFiles.map { mdFile ->
            parser.parseQuestion(mdFile.readText())
        }
        val questionsByTag = questions.flatMap { question ->
            question.tags.map { tag -> tag to question }
        }.groupBy({ it.first }, { it.second })

        return questionsByTag.mapValues { (_, questions) ->
            questions.toSection(topic)
        }
    }

    private fun List<Question>.toSection(topic: String): Section {
        return Section(
            title = topic,
            assignments = this.map { question ->
                question.toEntity()
            }
        )
    }

    fun process(): List<Assessment> {
        val questionDirs = getQuestionDirectories()
        val parser = QuestionParser(config)
        val tagsToSections: Map<String, List<Section>> = questionDirs.map { questionDir ->
            parseMarkDownFilesInDir(parser, questionDir)
        }
        .flatMap { it.entries } // List<String, Section>
        .groupBy({ it.key }, { it.value }) // Map<String, List<Section>>

        return tagsToSections.map {(tag, sections) ->
            val assessment = Assessment(id = 0, tag = tag, sections = sections.toMutableList())
            assessment.sections.forEach {
                section -> section.assessment = assessment
            }
            assessment
        }
    }
}
