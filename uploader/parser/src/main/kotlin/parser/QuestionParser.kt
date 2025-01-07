package parser

import QuestionParsingException
import question.*
import ut.isep.management.model.entity.AssignmentType


class QuestionParser(private val frontmatterParser: FrontmatterParser) {

    /**
     * @throws QuestionParsingException
     */
    fun parseQuestion(filePath: String): Question{
        try {
            val metadata: Frontmatter = frontmatterParser.parseQuestion(filePath)
            val body: String = QuestionFileHandler.split(filePath).second
            val type: AssignmentType = AssignmentType.fromString(metadata.type)
            return when (type) {
                AssignmentType.MULTIPLE_CHOICE -> parseMultipleChoiceQuestion(body, metadata, filePath)
                AssignmentType.OPEN -> parseOpenQuestion(body, metadata, filePath)
                AssignmentType.CODING -> TODO()
            }
        } catch (e: Exception) {
            throw QuestionParsingException(
                "Failed to parse question.",
                "Input: ${filePath}\nError: ${e.message}"
            )
        }
    }


    fun parseMultipleChoiceQuestion(body: String, metadata: Frontmatter, filePath: String): MultipleChoiceQuestion {
        val descriptionRegex = """^(.*?)(?=\n- |\$)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val optionsRegex = """-\s*\[([xX ])]\s*(.*?)\s*$""".toRegex(RegexOption.MULTILINE)

        // Extract description using regex and default to an empty string if not found
        val description = descriptionRegex.find(body)?.groupValues?.getOrNull(1)?.trim().orEmpty()

        // Extract options from the body
        val options = optionsRegex.findAll(body).map { matchResult ->
            val (isChecked, text) = matchResult.destructured
            MultipleChoiceQuestion.Option(
                text = text.trim(),
                isCorrect = isChecked.equals("x", ignoreCase = true)
            )
        }.toList()

        // Return the parsed question
        return MultipleChoiceQuestion(
            id = metadata.id,
            tags = metadata.tags,
            description = description,
            filePath = filePath,
            options = options
        )
    }

    fun parseOpenQuestion(body: String, metadata: Frontmatter, filePath: String): OpenQuestion {
        return OpenQuestion(
            id = metadata.id,
            filePath = filePath,
            tags = metadata.tags,
            description = body, // Open questions have only description in their body
        )
    }
}
