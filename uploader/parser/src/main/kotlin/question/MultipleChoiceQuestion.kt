package question

import ut.isep.management.model.entity.AssignmentType

data class MultipleChoiceQuestion(
    override val id: String?,
    override val filePath: String,
    override val tags: List<String>,
    override val description: String,
    val options: List<Option> // List of options for the multiple-choice question
) : Question {
    override val type: AssignmentType = AssignmentType.MULTIPLE_CHOICE

    data class Option(
        val text: String, // Option text
        val isCorrect: Boolean // True if the option is correct, otherwise false
    )
}
