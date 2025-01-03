import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import question.*
import ut.isep.management.model.entity.AssignmentType
import java.io.File
import java.io.IOException


class QuestionParser(private val configFile: File) {

    data class Frontmatter @JsonCreator constructor(
        @JsonProperty("id") val id: String?,
        @JsonProperty("type") val type: String,
        @JsonProperty("tags") val tags: List<String>
    )

    private val objectMapper = ObjectMapper(YAMLFactory())
    private val config: Config = parseConfig()

    private fun parseConfig(): Config {
        return objectMapper.readValue(configFile, Config::class.java)
    }

    fun parseFile(file: File): Question {
        if (!file.exists()) {
            throw FileParsingException("No such file", file.name)
        }
        if (!file.canRead()) {
            throw FileParsingException("Cannot read file", file.name)
        }
        return try {
            parseQuestion(file.readText(), file.name)
        } catch (e: IOException) {
            throw FileParsingException("An I/O error occurred: ${e.message}", file.name)
        }
    }

    /**
     * @throws QuestionParsingException
     */
    fun parseQuestion(input: String, filePath: String): Question{
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
            val type: AssignmentType = AssignmentType.fromString(metadata.type)
            return when (type) {
                AssignmentType.MULTIPLE_CHOICE -> parseMultipleChoiceQuestion(body, metadata, filePath)
                AssignmentType.OPEN -> parseOpenQuestion(body, metadata, filePath)
                AssignmentType.CODING -> TODO()
            }
        } catch (e: Exception) {
            throw QuestionParsingException("Failed to parse question.", "Input: ${input.take(100)}\nError: ${e.message}")
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
