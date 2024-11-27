sealed class Question {
    abstract val id: String? // Unique identifier for the question, can be null for new questions
    abstract val type: QuestionType
    abstract val tags: List<String>
    abstract val description: String
}

enum class QuestionType(val type: String) {
    MULTIPLE_CHOICE("multiple-choice"),
    OPEN("open");

    companion object {
        fun fromString(type: String): QuestionType {
            return entries.find { it.type == type }
                ?: throw IllegalArgumentException("Unknown type: $type")
        }
    }
}


data class MultipleChoiceQuestion(
    override val id: String?,
    override val tags: List<String>,
    override val description: String,
    val options: List<Option> // List of options for the multiple-choice question
) : Question() {
    override val type: QuestionType = QuestionType.MULTIPLE_CHOICE
}

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
