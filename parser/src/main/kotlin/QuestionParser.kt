import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue

class QuestionParser {

    data class Frontmatter @JsonCreator constructor(
        @JsonProperty("id") val id: String?,
        @JsonProperty("type") val type: String,
        @JsonProperty("tags") val tags: List<String>
    )

    private val objectMapper = ObjectMapper(YAMLFactory())

    fun parseQuestion(input: String): Question {
        // Split the input into frontmatter and body sections
        val parts = input.split("---").map { it.trim() }.filter { it.isNotEmpty() }

        // Ensure we have both frontmatter and body
        require(parts.size == 2) { "Invalid question format. Must contain frontmatter and body." }

        val frontmatter = parts[0]
        val body = parts[1]

        // Parse the frontmatter using Jackson
        val metadata: Frontmatter = objectMapper.readValue(frontmatter)
        val type: QuestionType = QuestionType.fromString(metadata.type)
        return when (type) {
            QuestionType.MULTIPLE_CHOICE -> parseMultipleChoiceQuestion(body, metadata)
            QuestionType.OPEN -> parseOpenQuestion(body, metadata)
        }
    }

    fun parseMultipleChoiceQuestion(body: String, metadata: Frontmatter): MultipleChoiceQuestion {
        val descriptionRegex = """^(.*?)(?=\n- |\$)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val description = descriptionRegex.find(body)?.groups?.get(1)?.value?.trim() ?: ""


        val optionsRegex = """-\s*

\[([x ])]\s*(.*?)\s*$""".toRegex()
        val options = optionsRegex.findAll(body).map {
            Option(
                text = it.groups[2]?.value?.trim() ?: "",
                isCorrect = it.groups[1]?.value == "x"
            )
        }.toList()
        return MultipleChoiceQuestion(
            id = null, // Assuming IDs are not in frontmatter
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
