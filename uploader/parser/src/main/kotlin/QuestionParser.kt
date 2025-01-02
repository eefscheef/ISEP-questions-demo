import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import question.*

class QuestionParser(val config: Config) {

    data class Frontmatter @JsonCreator constructor(
        @JsonProperty("id") val id: String?,
        @JsonProperty("type") val type: String,
        @JsonProperty("tags") val tags: List<String>
    )

    private val objectMapper = ObjectMapper(YAMLFactory())

    /**
     * @throws QuestionParsingException
     */
    fun parseQuestion(input: String): Question{
        try {
            val parts = input.split("---").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size != 2) {
                throw QuestionParsingException("Invalid question format. Must contain frontmatter and body.")
            }

            val frontmatter = parts[0]
            val body = parts[1]

            val metadata: Frontmatter = objectMapper.readValue(frontmatter)
            metadata.tags.forEach { tag ->
                if (tag !in config.tagOptions) {
                    throw QuestionParsingException("Invalid tag provided: $tag is not present in config file")
                }
            }
            val type: QuestionType = QuestionType.fromString(metadata.type)
            return when (type) {
                QuestionType.MULTIPLE_CHOICE -> parseMultipleChoiceQuestion(body, metadata)
                QuestionType.OPEN -> parseOpenQuestion(body, metadata)
            }
        } catch (e: Exception) {
            throw QuestionParsingException("Failed to parse question.", "Input: ${input.take(100)}\nError: ${e.message}")
        }
    }


    fun parseMultipleChoiceQuestion(body: String, metadata: Frontmatter): MultipleChoiceQuestion {
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
            options = options
        )
    }

    fun parseOpenQuestion(body: String, metadata: Frontmatter): OpenQuestion {
        return OpenQuestion(
            id = metadata.id,
            tags = metadata.tags,
            description = body, // Open questions have only description in their body
        )
    }
}
