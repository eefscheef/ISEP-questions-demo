package parser

import Config
import QuestionParsingException
import question.*
import ut.isep.management.model.entity.AssignmentType
import java.io.Reader


class QuestionParser(
    private val config: Config,
    private val frontmatterParser: FrontmatterParser = FrontmatterParser(config)
) {

    /**
     * @throws QuestionParsingException
     */
    fun parse(inputReader: Reader, fileName: String): Question {
        try {
            // Delegate parsing of frontmatter and body
            val (metadata, body) = frontmatterParser.parse(inputReader, fileName)
            val type: AssignmentType = AssignmentType.fromString(metadata.type)
            return when (type) {
                AssignmentType.MULTIPLE_CHOICE -> parseMultipleChoiceQuestion(body, metadata)
                AssignmentType.OPEN -> parseOpenQuestion(body, metadata)
                AssignmentType.CODING -> TODO()
            }
        } catch (e: Exception) {
            throw QuestionParsingException(
                message = "Failed to parse question.",
                context = "Input: $fileName\nError: ${e.message}",
                cause = e
            )
        }
    }

    // Parsing logic for question types remains unchanged
    private fun parseMultipleChoiceQuestion(body: String, metadata: Frontmatter): MultipleChoiceQuestion {
        val descriptionRegex = """^(.*?)(?=\n- |\$)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val optionsRegex = """-\s*\[([xX ])]\s*(.*?)\s*$""".toRegex(RegexOption.MULTILINE)

        val description = descriptionRegex.find(body)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val options = optionsRegex.findAll(body).map { matchResult ->
            val (isChecked, text) = matchResult.destructured
            MultipleChoiceQuestion.Option(
                text = text.trim(),
                isCorrect = isChecked.equals("x", ignoreCase = true)
            )
        }.toList()

        return MultipleChoiceQuestion(
            id = metadata.id,
            tags = metadata.tags,
            filePath = metadata.originalFilePath,
            description = description,
            options = options
        )
    }

    private fun parseOpenQuestion(body: String, metadata: Frontmatter): OpenQuestion {
        return OpenQuestion(
            id = metadata.id,
            tags = metadata.tags,
            filePath = metadata.originalFilePath,
            description = body
        )
    }
}
