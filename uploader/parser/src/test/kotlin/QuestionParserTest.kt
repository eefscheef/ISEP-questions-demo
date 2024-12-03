package ut.isep

import question.MultipleChoiceQuestion
import question.OpenQuestion
import QuestionParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionParserTest {

    private val parser = QuestionParser()

    @Test
    fun `test parsing single answer multiple-choice question`() {
        val input = """
            ---
            id: unique-question-id-12345 #Automatically generated ID for  database reference, do not modify!
            type: multiple-choice
            tags:
              - Frontend Developer
              - Backend Developer
            ---
            What is the difference between a stack and a queue?
            
            - [ ] A stack is FIFO, a queue is LIFO.
            - [x] A stack is LIFO, a queue is FIFO.
            - [ ] Both are FIFO.
            - [ ] Both are LIFO.
        """.trimIndent()

        val question = parser.parseQuestion(input) as MultipleChoiceQuestion

        assertEquals("What is the difference between a stack and a queue?", question.description)
        assertEquals(4, question.options.size)
        assertTrue(question.options.any { it.text == "A stack is LIFO, a queue is FIFO." && it.isCorrect })
        assertTrue(question.options.filter {it.isCorrect}.size == 1)
    }

    @Test
    fun `test parsing multiple answers multiple-choice question`() {
        val input = """
            ---
            type: multiple-choice
            
            tags:
            - Backend Developer
            - System Design
            ---
            
            Why does the monolithic architecture not eat the microservice oriented architecture?
            
            - [x] Monolithic architecture is more scalable than microservices.
            - [X] Microservices architecture allows independent scaling of components, while monolithic does not.
            - [x] Microservices architecture is simpler to deploy than monolithic.
            - [ ] Monolithic architecture is easier to maintain than microservices.
            - [ ] Test.
        """.trimIndent()

        val question = parser.parseQuestion(input) as MultipleChoiceQuestion

        assertEquals("Why does the monolithic architecture not eat the microservice oriented architecture?", question.description)
        assertEquals(5, question.options.size)
        assertTrue(question.options.filter {it.isCorrect}.size == 3)
    }


    @Test
    fun `test parsing open question`() {
        val input = """
            ---
            id: unique-question-id-35842 #Automatically generated ID for  database reference, do not modify!
            type: open
            tags: 
              - Deezend 
              - developer
              - Reee
            ---
            What is the difference between a stack and a queue?
        """.trimIndent()

        val question = parser.parseQuestion(input) as OpenQuestion

        assertEquals("What is the difference between a stack and a queue?", question.description)
    }

}
