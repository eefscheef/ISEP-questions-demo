package ut.isep

import QuestionParser
import question.Question
import ut.isep.management.model.entity.Assessment
import ut.isep.management.model.entity.AssessmentID
import ut.isep.management.model.entity.Section
import java.io.File

class AssessmentProcessor(private val rootDir: File, private val parser: QuestionParser, private val commitHash: String) {


    private fun getQuestionDirectories(): Array<File> {
        return rootDir.listFiles { file ->
            file.isDirectory &&
                    file.name != "uploader" &&
                    !file.name.startsWith(".")
        } ?: emptyArray()
    }

    private fun parseMarkDownFilesInDir(
        questionDir: File
    ): Map<String, Section> {
        val topic = questionDir.name
        val mdFiles = questionDir.listFiles { file -> file.extension == "md" } ?: emptyArray()
        val questions: List<Question> = mdFiles.map { mdFile ->
            parser.parseQuestion(mdFile.readText(), mdFile.name)
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
        val tagsToSections: Map<String, List<Section>> = questionDirs.map { questionDir ->
            parseMarkDownFilesInDir(questionDir)
        }
            .flatMap { it.entries } // List<String, Section>
            .groupBy({ it.key }, { it.value }) // Map<String, List<Section>>

        return tagsToSections.map { (tag, sections) ->
            val assessment =
                Assessment(AssessmentID(tag = tag, gitCommitHash = commitHash), sections = sections.toMutableList())
            assessment.sections.forEach { section ->
                section.assessment = assessment
            }
            assessment
        }
    }
}
