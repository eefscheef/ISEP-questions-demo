package question

import ut.isep.management.model.entity.AssignmentType

data class OpenQuestion(
    override val id: String?,
    override val tags: List<String>,
    override val description: String,
    override val filePath: String
) : Question {
    override val type: AssignmentType = AssignmentType.OPEN
}

