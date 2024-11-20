sealed class Question {
    abstract val id: String? // Unique identifier for the question, can be null for new questions
    abstract val type: QuestionType
    abstract val tags: List<String>
    abstract val description: String
}

enum class QuestionType {
    MULTIPLE_CHOICE, OPEN
}

data class MultipleChoiceQuestion(
    override val id: String?,
    override val type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    override val tags: List<String>,
    override val description: String,
    val options: List<Option> // List of options for the multiple-choice question
) : Question()

data class OpenQuestion(
    override val id: String?,
    override val type: QuestionType = QuestionType.OPEN,
    override val tags: List<String>,
    override val description: String
) : Question()

data class Option(
    val text: String, // Option text
    val isCorrect: Boolean // True if the option is correct, otherwise false
)
