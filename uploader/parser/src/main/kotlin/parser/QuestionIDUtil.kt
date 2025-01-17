package parser

object QuestionIDUtil {
    private val QUESTION_ID_REGEX = """(.*?)_qid(\d+).md""".toRegex()

    fun parseQuestionID(filename: String): Long? {
        return QUESTION_ID_REGEX.find(filename)?.groupValues?.get(2)?.toLongOrNull()
    }

    fun removeQuestionIDIfPresent(filename: String): String {
        return QUESTION_ID_REGEX.replace(filename) { matchResult ->
            // Only keep the part before "_qid{id}.md", then append .md
            matchResult.groupValues[1] + ".md"
        }
    }

    fun injectQuestionID(filePath: String, id: Long): String {
        require(filePath.endsWith(".md")) { "File path must end with .md" }
        return filePath.substringBeforeLast(".md", ) + "_qid$id.md"
    }
}
