package ut.isep

import QueryExecutor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ut.isep.management.model.entity.*

class QueryExecutorTest : BaseIntegrationTest() {

    @Test
    fun `test uploadEntities persists assignments, sections with assessment`() {
        // Arrange
        val assignment1 =
            Assignment(baseFilePath = "Topic/filename_qid123", assignmentType = AssignmentType.OPEN, availablePoints = 5)
        val assignment2 = Assignment(
            baseFilePath = "Database/jpa_qid12",
            assignmentType = AssignmentType.MULTIPLE_CHOICE,
            availablePoints = 5
        )
        val assignment3 = Assignment(
            baseFilePath = "Management/scrum_qid9",
            assignmentType = AssignmentType.MULTIPLE_CHOICE,
            availablePoints = 5
        )
        val assignment4 =
            Assignment(baseFilePath = "Cooking/frikandel_qid2", assignmentType = AssignmentType.OPEN, availablePoints = 5)

        val section1 = Section(title = "section 1").apply {
            addAssignment(assignment1)
            addAssignment(assignment2)
        }
        val section2 = Section(title = "section 2").apply {
            addAssignment(assignment3)
            addAssignment(assignment4)
        }
        val assessment =
            Assessment(AssessmentID(tag = "testAssessment", gitCommitHash = "somehash1234"), latest = true).apply {
                addSection(section1)
                addSection(section2)
            }

        // Act
        queryExecutor.withTransaction {
            queryExecutor.uploadEntities(listOf(assessment))
        }

        // Assert assignments persisted correctly
        val retrievedAssignments = TestQueryHelper.fetchAll<Assignment>(session)
        val storedAssignments = listOf(assignment1, assignment2, assignment3, assignment4)
        assertEquals(4, retrievedAssignments.size, "Expected 4 assignments in the database")
        (0 .. 3).forEach {i ->
            assertEquals(retrievedAssignments[i].assignmentType, storedAssignments[i].assignmentType)
            assertEquals(retrievedAssignments[i].baseFilePath, storedAssignments[i].baseFilePath)
            assertEquals(retrievedAssignments[i].sectionTitle, storedAssignments[i].sectionTitle)
            // Assert that assignments were assigned an ID by the database
            assertNotEquals(retrievedAssignments[i].id, storedAssignments[i].id)
        }


        // Verify sections
        val retrievedSections = TestQueryHelper.fetchAll<Section>(session)
        retrievedSections.forEach {section ->
            assertNotEquals(section.id, 0) // an ID was assigned in DB
        }
        assertEquals(2, retrievedSections.size, "Expected 2 sections in the database")

        // Verify assessments
        val retrievedAssessment = TestQueryHelper.fetchSingle<Assessment>(session)
            ?: fail("Expected a single assessment in the database")
        assertNotEquals(retrievedAssessment.id, 0) // an ID was assigned in DB
        assertEquals(assessment.id, retrievedAssessment?.id, "Assessment ID mismatch")
    }

    @Test
    fun `test uploadEntities persists multiple assignments`() {
        // Arrange
        val assignments = listOf(
            Assignment(baseFilePath = "testPath1", assignmentType = AssignmentType.OPEN, availablePoints = 5),
            Assignment(baseFilePath = "testPath2", assignmentType = AssignmentType.MULTIPLE_CHOICE, availablePoints = 10)
        )

        // Act
        queryExecutor.withTransaction {
            uploadEntities(assignments)
        }

        // Assert
        val result = TestQueryHelper.fetchAll<Assignment>(session)
        assertEquals(2, result.size, "Expected 2 assignments in the database")
    }

    @Test
    fun `test clearDatabase deletes assignments, sections, assessments`() {
        // Arrange: Add some sample data to the database
        val assignment = Assignment(baseFilePath = "testPath", assignmentType = AssignmentType.OPEN, availablePoints = 2)
        val assessment = Assessment(AssessmentID(tag = "test", gitCommitHash = "hash"), latest = true).apply {
            addSection(Section(title = "Sample Section").apply {
                addAssignment(assignment)
            })
        }
        TestQueryHelper.persistEntity(assessment, session)

        // Act: Clear the database
        clearDatabase()

        // Assert: Verify that the database is empty
        val assignments = TestQueryHelper.fetchAll<Assignment>(session)
        assertTrue(assignments.isEmpty(), "Expected no assignments in the database")
    }

    @Test
    fun `test findAssessmentsByAssignmentIds`() {
        // Arrange
        val assignment1 = Assignment(baseFilePath = "path1", assignmentType = AssignmentType.OPEN, availablePoints = 3)
        val assignment2 =
            Assignment(baseFilePath = "path2", assignmentType = AssignmentType.MULTIPLE_CHOICE, availablePoints = 7)
        val section = Section(title = "Test Section")
        val assessment = Assessment(
            id = AssessmentID(tag = "test tag", gitCommitHash = "test commit hash"),
            latest = true
        ).apply {
            addSection(section.apply {
                addAssignment(assignment1)
                addAssignment(assignment2)
            })
        }
        TestQueryHelper.persistEntity(assessment, session)

        // Act
        val result = queryExecutor.findAssessmentsByAssignmentIds(listOf(assignment1.id, assignment2.id))

        // Assert
        assertEquals(1, result.size, "Expected 1 assessment associated with the given assignments")
    }

    @Test
    fun `test getLatestAssessments only returns latest=true assessment`() {
        // Arrange
        val activeAssessment = Assessment(
            id = AssessmentID(tag = "activeTag", gitCommitHash = "newerHash"),
            latest = true,
        )
        val inactiveAssessment = Assessment(
            id = AssessmentID(tag = "inactiveTag", gitCommitHash = "olderHash"),
            latest = false,
        )
        TestQueryHelper.persistEntity(activeAssessment, session)
        TestQueryHelper.persistEntity(inactiveAssessment, session)

        // Act
        val retrievedAssessments = queryExecutor.getLatestAssessments()
        // Assert
        assertEquals(1, retrievedAssessments.size)
        assertEquals("activeTag", retrievedAssessments[0].id.tag!!)
        assertTrue(retrievedAssessments[0].latest)
    }

    @Test
    fun `test getLatestAssessment`() {
        // Arrange
        val latestAssessment = Assessment(
            id = AssessmentID(tag = "latestTag", gitCommitHash = "latestHash"),
            latest = true,
        )
        val olderAssessment = Assessment(
            id = AssessmentID(tag = "latestTag", gitCommitHash = "olderHash"),
            latest = false
        )
        TestQueryHelper.persistEntity(latestAssessment, session)
        TestQueryHelper.persistEntity(olderAssessment, session)

        // Act
        val result = queryExecutor.getLatestAssessment("latestTag")

        // Assert
        assertNotNull(result, "Expected a non-null latest assessment")
        assertTrue(result.latest, "Expected the assessment to be marked as latest")
    }

    @Test
    fun `test findAssignmentsByIds`() {
        // Arrange
        val assignment1 = Assignment(baseFilePath = "test1", assignmentType = AssignmentType.OPEN, availablePoints = 5)
        val assignment2 =
            Assignment(baseFilePath = "test2", assignmentType = AssignmentType.MULTIPLE_CHOICE, availablePoints = 10)
        TestQueryHelper.persistEntity(assignment1, session)
        TestQueryHelper.persistEntity(assignment2, session)

        // Act
        val result = queryExecutor.findAssignmentsByIds(listOf(assignment1.id, assignment2.id))

        // Assert
        assertEquals(2, result.size, "Expected 2 assignments in the result")
        assertTrue(result.any { it.baseFilePath == "test1" }, "Expected assignment1 in the result")
        assertTrue(result.any { it.baseFilePath == "test2" }, "Expected assignment2 in the result")
    }
}
