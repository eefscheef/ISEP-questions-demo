class QuestionParsingException(message: String, val context: String? = null, cause: Exception? = null) : Exception(message, cause) {
    override fun toString(): String {
        return "QuestionParsingException: $message${context?.let { " | Context: $it" } ?: ""}"
    }
}

class FileParsingException(message: String, val filename: String, cause: Exception? = null) : Exception(message, cause) {
    override fun toString(): String {
        return "FileParsingException: $message | Filename: $filename "
    }
}
