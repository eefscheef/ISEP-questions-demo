package question

data class OpenQuestion(
    override val id: String?,
    override val tags: List<String>,
    override val description: String
) : Question() {
    override val type: QuestionType = QuestionType.OPEN
}

data class Option(
    val text: String, // Option text
    val isCorrect: Boolean // True if the option is correct, otherwise false
)
