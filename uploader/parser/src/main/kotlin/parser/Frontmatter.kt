package parser

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class Frontmatter @JsonCreator constructor(
    @JsonProperty("type") val type: String,
    @JsonProperty("tags") val tags: List<String>,
) {
    val filePath: String? = null
    var id: Long? = null
}