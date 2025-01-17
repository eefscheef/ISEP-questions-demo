package ut.isep

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import ut.isep.management.model.entity.Assignment
import ut.isep.management.model.entity.AssignmentType

class PostgresTest : BaseIntegrationTest() {

    @Test
    fun `test inserting and retrieving an assignment`() {
        // Arrange
        val assignment = Assignment(baseFilePath = "testPath", assignmentType = AssignmentType.OPEN, availablePoints = 2)
        // Act
        TestQueryHelper.persistEntity(assignment, session)
        // Assert
        val result = TestQueryHelper.fetchSingle<Assignment>(session) ?: fail("Expected an assignment to be persisted")
        assertEquals(assignment.baseFilePath, result.baseFilePath)
        assertEquals(assignment.assignmentType, result.assignmentType)
        assertEquals(assignment.availablePoints, result.availablePoints)
    }
}