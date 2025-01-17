package parser

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class QuestionIDUtilTest {

    @Test
    fun parseQuestionID() {
        // Arrange
        val filePathContainingQid213 = "/home/user/Documents/Topic/QuestionFile_qid123.md"
        assertEquals(123, QuestionIDUtil.parseQuestionID(filePathContainingQid213))
    }

    @Test
    fun removeQuestionID() {
        val filePathContainingQid213 = "/home/user/Documents/Topic/QuestionFile_qid123.md"
        assertEquals("/home/user/Documents/Topic/QuestionFile.md", QuestionIDUtil.removeQuestionIDIfPresent(filePathContainingQid213))
        val filePathContainingNoQid = "/home/user/Documents/Topic/QuestionFile.md"
        assertEquals("/home/user/Documents/Topic/QuestionFile.md", QuestionIDUtil.removeQuestionIDIfPresent(filePathContainingNoQid))
        val filePathContainingQidInTheMiddle = "/home/user/Documents/Topic_qid123/QuestionFile.md"
        assertEquals("/home/user/Documents/Topic_qid123/QuestionFile.md", QuestionIDUtil.removeQuestionIDIfPresent(filePathContainingQidInTheMiddle))
    }
}