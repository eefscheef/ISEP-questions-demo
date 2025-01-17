package parser

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class Frontmatter @JsonCreator constructor(
    @JsonProperty("type") val type: String,
    @JsonProperty("tags") val tags: List<String>,
    @JsonProperty("availablePoints") val availablePoints: Int = 1
) {
    lateinit var originalFilePath: String
    val baseFilePath: String
        get() = QuestionIDUtil.removeQuestionIDIfPresent(originalFilePath)
    var id: Long? = null
}