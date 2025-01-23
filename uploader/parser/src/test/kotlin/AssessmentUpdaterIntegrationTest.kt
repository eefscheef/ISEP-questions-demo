package ut.isep

import AssessmentUpdater
import QueryExecutor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import parser.Config
import ut.isep.management.model.entity.*
import kotlin.io.path.pathString

class AssessmentUpdaterIntegrationTest : BaseIntegrationTest() {
    @BeforeEach
    override fun openSession() {
        super.openSession()
    }

    @Test
    fun `should handle added question files correctly`() {
        // Arrange
        val topic = "ImaginaryProgramming"
        val filename = "file1.md"
        val content = """
            ---
            type: "multiple-choice"
            tags: 
            - tag1
            availablePoints: 2
            ---
            Some content here.
        """.trimIndent()
        val testFile = createTestFile(topic, filename, content)
        val assessment = Assessment(AssessmentID(tag = "tag1", gitCommitHash = "coolhash"), latest = true)
        TestQueryHelper.persistEntity(assessment, session)

        // Act
        val assessmentUpdater =
            AssessmentUpdater(sessionFactory, "commitHash")
        assessmentUpdater.updateAssessments(addedFilenames = (listOf(testFile.path)))
        // Assert an assignment was uploaded
        val retrievedAssignment = TestQueryHelper.fetchSingle<Assignment>(session) ?: fail("Expected one assignment")
        // Assert the basePath was uploaded correctly
        assertEquals(retrievedAssignment.baseFilePath, tempDir.pathString + "/" + topic + "/" + filename)
        // Assert the assignment type was uploaded correctly
        assertEquals(AssignmentType.MULTIPLE_CHOICE, retrievedAssignment.assignmentType)
        // Assert the availablePoints was uploaded correctly
        assertEquals(2, retrievedAssignment.availablePoints)
        // Assert old file no longer exists
        assertFalse(testFile.isFile)
        // Assert there is one new file which includes the qid of the assignment
        val newTestFiles = tempDir.resolve(topic).toFile().listFiles()
            ?: fail("The assignment file is no longer stored in the expected dir ${tempDir.resolve(topic).pathString}")
        assertEquals(1, newTestFiles.size)
        assertEquals(
            tempDir.pathString + "/" + topic + "/" + "file1_qid${retrievedAssignment.id}.md",
            newTestFiles[0].path
        )

    }

    @Test
    fun `should handle deleted question files correctly`() {
        // Given
        val assignment =
            Assignment(baseFilePath = "file2_qid1234.md", assignmentType = AssignmentType.OPEN, availablePoints = 2)
        val assessment = Assessment(
            id = AssessmentID(tag = "tag2", gitCommitHash = "commit"),
            latest = true
        ).apply {
            addSection(Section(title = "section1").apply {
                addAssignment(assignment)
            })
        }
        TestQueryHelper.persistEntity(assessment, session)

        // When
        println("assignment id :${assignment.id}")
        val assessmentUpdater = AssessmentUpdater(sessionFactory, "commitHash")

        assessmentUpdater.updateAssessments(deletedFilenames = listOf("file2_qid${assignment.id}.md"))

        // Then
        val retrievedAssessment = QueryExecutor(session).getLatestAssessment("tag2")
        assertEquals(0, retrievedAssessment.sections.sumOf { it.assignments.size })
    }

    @Test
    fun `should handle modified question files correctly`() {
        // Arrange: add existing assignment in an assessment to database.
        var existingAssignment =
            Assignment(baseFilePath = "file3.md", assignmentType = AssignmentType.MULTIPLE_CHOICE, availablePoints = 2)
        existingAssignment = TestQueryHelper.persistEntity(existingAssignment, session)
        val existingAssessment = Assessment(AssessmentID("tag3", "previousHash"), latest = true).apply {
            addSection(Section(title = "TestTopic").apply {
                addAssignment(existingAssignment)
            })
        }
        TestQueryHelper.persistEntity(existingAssessment, session)


        val topic = "TestTopic"
        // Arrange: Update filename to store the autogenerated ID
        val filename = "file3_qid${existingAssignment.id}.md"
        val content = """
            ---
            type: "open"
            tags:
            -  "tag3"
            ---
            Modified content here.
        """.trimIndent()
        val testFile = createTestFile(topic, filename, content)

        val assessmentUpdater = AssessmentUpdater(sessionFactory, "commitHash")

        // When
        assessmentUpdater.updateAssessments(modifiedFilenames = listOf(testFile.path))

        // Then
        val assignments = TestQueryHelper.fetchAll<Assignment>(session)
        assertEquals(2, assignments.size)
        assertEquals(AssignmentType.MULTIPLE_CHOICE, assignments[0].assignmentType)
        assertEquals(AssignmentType.OPEN, assignments[1].assignmentType)
    }

    @Test
    fun `should handle modified config correctly`() {
        // Arrange one non-active assignment in the database, and an active tag in the config
        val existingInactiveAssessment = Assessment(
            id = AssessmentID("inactiveTag", "previousCommitHash"),
            latest = false
        )
        val existingActiveAssessment = Assessment(
            id = AssessmentID("activeTag", "previousCommitHash"),
            latest = true
        )
        TestQueryHelper.persistEntity(existingInactiveAssessment, session)
        TestQueryHelper.persistEntity(existingActiveAssessment, session)
        val config = Config(tagOptions = listOf("newAssessment"), questionOptions = listOf("open", "multipleChoice"))
        val assessmentUpdater = AssessmentUpdater(sessionFactory, "newCommitHash")

        // Act: Assessmentupdater updates the database to upload an assessment with the active tag
        assessmentUpdater.updateAssessments(config = config)
        session.close()
        val session = sessionFactory.openSession()
        // Assert: we find one active and one inactive assessment, sharing a tag (but not commit hashes)
        val assessments = TestQueryHelper.fetchAll<Assessment>(session)
        assertEquals(3, assessments.size)
        val retrievedOldInactiveAssessment = assessments[0]
        val retrievedOldActiveAssessment = assessments[2]
        val retrievedNewActiveAssessment = assessments[1]

        assertEquals(existingInactiveAssessment.id, retrievedOldInactiveAssessment.id)
        assertFalse(retrievedOldInactiveAssessment.latest)

        assertEquals(existingActiveAssessment.id, retrievedOldActiveAssessment.id)
        assertFalse(retrievedOldActiveAssessment.latest)

        assertEquals("newAssessment", retrievedNewActiveAssessment.id.tag!!)
        assertNotEquals(retrievedNewActiveAssessment.id.gitCommitHash!!, retrievedOldInactiveAssessment.id.gitCommitHash!!)
        assertTrue(retrievedNewActiveAssessment.latest)
        session.close()
    }

    @Test
    fun `should upload updated assessments`() {
        // Given
        val existingAssignment = Assignment(
            baseFilePath = "file4.md", assignmentType = AssignmentType.MULTIPLE_CHOICE, availablePoints = 2
        ).apply {
            TestQueryHelper.persistEntity(this, session)
        }
        val assessment = Assessment(
            id = AssessmentID(tag = "tag4", gitCommitHash = "testCommit"),
            latest = true
        ).apply {
            addSection(Section(title = "section1").apply {
                addAssignment(existingAssignment)
            })
        }
        TestQueryHelper.persistEntity(assessment, session)
        val topic = "Topic1"
        // Arrange: Update filename to store the autogenerated ID
        val filename = "file3_qid${existingAssignment.id}.md"
        // Modify assessment
        val content = """
            ---
            type: "open"
            tags:
            -  "tag4"
            ---
            Updated content here.
        """.trimIndent()
        val testFile = createTestFile(topic, filename, content)

        val assessmentUpdater =
            AssessmentUpdater(sessionFactory, "commitHash")

        // When
        assessmentUpdater.updateAssessments(modifiedFilenames = listOf(testFile.path))

        // Then
        val updatedAssessments = TestQueryHelper.fetchAll<Assessment>(session)
        assertEquals(2, updatedAssessments.size)
        val updatedAssignment = updatedAssessments[1].sections[0].assignments[0]
        assertEquals(AssignmentType.OPEN, updatedAssignment.assignmentType)
    }

    @Test
    fun `should update commit hash for assessments with the given old hash`() {
        // Arrange
        val oldHash = "oldhash1234"
        val newHash = "newhash1234"

        // Create sample assessments with the old hash
        val assessment1 = Assessment(id = AssessmentID(tag = "tag1", gitCommitHash = oldHash), latest = true)
        val assessment2 = Assessment(id = AssessmentID(tag = "tag2", gitCommitHash = oldHash), latest = true)
        TestQueryHelper.persistEntity(assessment1, session)
        TestQueryHelper.persistEntity(assessment2, session)
        session.close()

        // Act: Create AssessmentUpdater and call updateHash
        val assessmentUpdater = AssessmentUpdater(sessionFactory, oldHash)
        assessmentUpdater.updateHash(newHash)
        val session = sessionFactory.openSession()
        // Assert: Verify that both assessments have their commit hash updated
        val updatedAssessments = TestQueryHelper.fetchAll<Assessment>(session)
        assertEquals(2, updatedAssessments.size)
        assertEquals(newHash, updatedAssessments[0].id.gitCommitHash, "Assessment 1 commit hash was not updated")
        assertEquals(newHash, updatedAssessments[1].id.gitCommitHash, "Assessment 2 commit hash was not updated")
        session.close()
    }

    @Test
    fun `should not fail when no assessments match the given commit hash`() {
        // Arrange
        val nonExistingHash = "nonexistenthash"

        // Act: Call updateHash with a hash that doesn't exist in the database
        val assessmentUpdater = AssessmentUpdater(sessionFactory, nonExistingHash)
        assessmentUpdater.updateHash("newhash1234")  // This should do nothing but not fail

        // Assert: No assessments should be changed
        val assessments = TestQueryHelper.fetchAll<Assessment>(session)
        val unmodifiedAssessments = assessments.filter { it.id.gitCommitHash == nonExistingHash }
        assertTrue(unmodifiedAssessments.isEmpty(), "No assessments should have the original commit hash")
    }

    @Test
    fun `should update commit hash for multiple assessments with the same hash`() {
        // Arrange
        val oldHash = "oldhash1234"
        val newHash = "newhash1234"

        // Create multiple assessments with the same old hash
        val assessment1 = Assessment(id = AssessmentID(tag = "tag1", gitCommitHash = oldHash), latest = true)
        val assessment2 = Assessment(id = AssessmentID(tag = "tag2", gitCommitHash = oldHash), latest = true)
        val assessment3 = Assessment(id = AssessmentID(tag = "tag3", gitCommitHash = oldHash), latest = true)
        TestQueryHelper.persistEntity(assessment1, session)
        TestQueryHelper.persistEntity(assessment2, session)
        TestQueryHelper.persistEntity(assessment3, session)

        // Act: Call updateHash to change the hash
        val assessmentUpdater = AssessmentUpdater(sessionFactory, oldHash)
        assessmentUpdater.updateHash(newHash)

        // Assert: Verify all three assessments have their commit hash updated
        val updatedAssessments = TestQueryHelper.fetchAll<Assessment>(session)
        assertEquals(3, updatedAssessments.size)

        assertEquals(newHash, updatedAssessments[0].id.gitCommitHash, "Assessment 1 commit hash was not updated")
        assertEquals(newHash, updatedAssessments[1].id.gitCommitHash, "Assessment 2 commit hash was not updated")
        assertEquals(newHash, updatedAssessments[2].id.gitCommitHash, "Assessment 3 commit hash was not updated")
    }
    @Test
    fun `should only update commit hash for active assessments`() {
        // Arrange
        val oldHash = "oldhash1234"
        val newHash = "newhash1234"

        // Create one active assessment and one inactive (latest = false)
        val activeAssessment = Assessment(id = AssessmentID(tag = "tag1", gitCommitHash = oldHash), latest = true)
        val inactiveAssessment = Assessment(id = AssessmentID(tag = "tag2", gitCommitHash = oldHash), latest = false)
        TestQueryHelper.persistEntity(activeAssessment, session)
        TestQueryHelper.persistEntity(inactiveAssessment, session)

        // Act: Call updateHash
        val assessmentUpdater = AssessmentUpdater(sessionFactory, oldHash)
        assessmentUpdater.updateHash(newHash)
        session.close()
        val session = sessionFactory.openSession()
        // Assert: Ensure that only the active assessment has its hash updated
        val updatedAssessments = TestQueryHelper.fetchAll<Assessment>(session)
        assertEquals(2, updatedAssessments.size)
        val updatedInactiveAssessment = updatedAssessments[0]
        assertEquals("tag2", updatedInactiveAssessment.id.tag!!)
        val updatedActiveAssessment = updatedAssessments[1]
        assertEquals("tag1", updatedActiveAssessment.id.tag!!)

        assertEquals(newHash, updatedActiveAssessment.id.gitCommitHash, "Active assessment's commit hash was not updated")
        assertEquals(oldHash, updatedInactiveAssessment.id.gitCommitHash, "Inactive assessment's commit hash should not be updated")
        session.close()
    }

}
