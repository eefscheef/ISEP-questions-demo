package question

data class MultipleChoiceQuestion(
    override val id: String?,
    override val tags: List<String>,
    override val description: String,
    val options: List<Option> // List of options for the multiple-choice question
) : Question() {
    override val type: QuestionType = QuestionType.MULTIPLE_CHOICE

}
