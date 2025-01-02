class QuestionParsingException(message: String, val context: String? = null) : Exception(message) {
    override fun toString(): String {
        return "QuestionParsingException: $message${context?.let { " | Context: $it" } ?: ""}"
    }
}

class FileParsingException(message: String, val filename: String, val questionParsingException: QuestionParsingException? = null) : Exception(message) {
    override fun toString(): String {
        return "FileParsingException: $message | Filename: $filename | ${questionParsingException}"
    }
}
