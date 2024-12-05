package question

import ut.isep.management.model.entity.AssignmentMultipleChoice

data class MultipleChoiceQuestion(
    override val id: String?,
    override val tags: List<String>,
    override val description: String,
    val options: List<Option> // List of options for the multiple-choice question
) : Question {
    override val type: QuestionType = QuestionType.MULTIPLE_CHOICE

    data class Option(
        val text: String, // Option text
        val isCorrect: Boolean // True if the option is correct, otherwise false
    )

    override fun toEntity(): AssignmentMultipleChoice {
        return AssignmentMultipleChoice(
            description = this.description,
            options = this.options.map {it.text},
            isMultipleAnswers = this.options.count {it.isCorrect} > 1
        )
    }
}
