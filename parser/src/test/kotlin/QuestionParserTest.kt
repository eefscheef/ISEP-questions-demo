package ut.isep

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionParserTest {

    private val parser = QuestionParser()

    @Test
    fun `test parsing multiple-choice question`() {
        val input = """
            ***
            type: multiple-choice
            tags: 
            - Frontend Developer 
            - Backend Developer
            description: What is the difference between a stack and a queue?
            options: 
            - [ ] A stack is FIFO, a queue is LIFO.
            - [x] A stack is LIFO, a queue is FIFO.
            - [ ] Both are FIFO.
            - [ ] Both are LIFO.
            ***
        """.trimIndent()

        val questions = parser.parseQuestions(input)

        assertEquals(1, questions.size)
        val question = questions[0] as MultipleChoiceQuestion
        assertEquals("What is the difference between a stack and a queue?", question.description)
        assertEquals(4, question.options.size)
        assertTrue(question.options.any { it.text == "A stack is LIFO, a queue is FIFO." && it.isCorrect })
    }

    @Test
    fun `test parsing open question`() {
        val input = """
            ***
            type: open
            tags: 
            - Deezend 
            - developer 
            - Reee
            description: What is the difference between a stack and a queue?
            ***
        """.trimIndent()

        val questions = parser.parseQuestions(input)

        assertEquals(1, questions.size)
        val question = questions[0] as OpenQuestion
        assertEquals("What is the difference between a stack and a queue?", question.description)
    }

    @Test
    fun `test parsing multiple questions`() {
        val input = """
            ***
            type: multiple-choice
            tags: 
            - Frontend Developer 
            description: What is the difference between a stack and a queue?
            options: 
            - [ ] A stack is FIFO, a queue is LIFO.
            - [x] A stack is LIFO, a queue is FIFO.
            ***
            ***
            type: open
            tags: 
            - Developer 
            description: What is the difference between an array and a list?
            ***
        """.trimIndent()

        val questions = parser.parseQuestions(input)

        assertEquals(2, questions.size)
    }

}
