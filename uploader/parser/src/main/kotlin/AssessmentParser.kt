import parser.QuestionParser
import parser.question.Question
import ut.isep.management.model.entity.Assessment
import ut.isep.management.model.entity.Section
import java.io.File

class AssessmentParser(private val questionDir: File, private val parser: QuestionParser) {

    private fun getQuestionDirectories(): Array<File> {
        return questionDir.listFiles { file ->
            file.isDirectory &&
                    !file.name.startsWith(".")
        } ?: emptyArray()
    }

    /**
     * @throws QuestionParsingException
     */
    private fun parseQuestionFilesInDir(questionDir: File): Map<String, Section> {
        val topic = questionDir.name
        val mdFiles = questionDir.listFiles { file -> file.extension == "md" } ?: emptyArray()
        val questions: MutableList<Question> = mdFiles.map { mdFile ->
            parser.parseFile(mdFile)
        }.toMutableList()

        val codingDirs = questionDir.listFiles { file -> file.isDirectory } ?: emptyArray()
        val codingQuestions = codingDirs.map { codingDir ->
            val files = codingDir.listFiles { file -> file.extension == "md" }
            parser.parseFile(
                files?.get(0)
                    ?: throw QuestionParsingException(
                        message = "No markdown file found in question directory",
                        context = "Directory: ${codingDir.path}"
                    )
            )
        }

        questions.addAll(codingQuestions)
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
            }.toMutableList()
        )
    }

    /**
     * @throws QuestionParsingException
     */
    fun parseAll(commitHash: String? = null): List<Assessment> {
        val questionDirs = getQuestionDirectories()
        val tagsToSections: Map<String, List<Section>> = questionDirs.map { questionDir ->
            parseQuestionFilesInDir(questionDir)
        }
            .flatMap { it.entries } // List<String, Section>
            .groupBy({ it.key }, { it.value }) // Map<String, List<Section>>

        return tagsToSections.map { (tag, sections) ->
            val assessment =
                Assessment(
                    tag = tag,
                    gitCommitHash = commitHash,
                    sections = sections.toMutableList(),
                    latest = true
                )
            assessment.sections.forEach { section ->
                section.assessment = assessment
            }
            assessment
        }
    }
}
