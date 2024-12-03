package question

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
