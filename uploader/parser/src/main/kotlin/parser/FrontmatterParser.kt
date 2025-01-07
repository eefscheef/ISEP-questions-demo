package parser

import Config
import QuestionParsingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue


class FrontmatterParser(private val config: Config) {
    private val objectMapper = ObjectMapper(YAMLFactory())

    /**
     * @throws QuestionParsingException
     */
    fun parseQuestion(filePath: String): Frontmatter {
        val frontmatter = QuestionFileHandler.split(filePath).first
        val metadata: Frontmatter = objectMapper.readValue(frontmatter)
        metadata.id = QuestionFileHandler.getQuestionID(filePath)

        metadata.tags.forEach { tag ->
            if (tag !in config.tagOptions) {
                throw QuestionParsingException("Invalid tag provided: $tag is not present in config file")
            }
        }
        return metadata
    }


}
