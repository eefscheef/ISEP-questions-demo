package ut.isep

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ut.isep.management.model.entity.*

class QueryExecutorTest : BaseIntegrationTest() {

    @Test
    fun `test uploadEntities persists assignments, sections with assessment`() {
        // Arrange
        val assignment1 =
           createAssignment(topic = "Topic", filename = "filename.md", type = AssignmentType.OPEN, availablePoints = 5, availableSeconds = 2343)
        val assignment2 =createAssignment(
            topic = "other topic",
            filename = "Database/jpa.md",
            type = AssignmentType.MULTIPLE_CHOICE,
            availablePoints = 5,
            availableSeconds = 300
        )
        val assignment3 =createAssignment(
            topic = "other topic",
            filename = "Management/scrum9.md",
            type = AssignmentType.MULTIPLE_CHOICE,
            availablePoints = 5,
            availableSeconds = 100000
        )
        val assignment4 =
           createAssignment(
               topic = "Some fourth topic",
               filename = "Cooking/frikandel.md",
               type = AssignmentType.OPEN,
               availablePoints = 5,
               availableSeconds = 321)

        val section1 = Section(title = "section 1").apply {
            addAssignment(assignment1)
            addAssignment(assignment2)
        }
        val section2 = Section(title = "section 2").apply {
            addAssignment(assignment3)
            addAssignment(assignment4)
        }
        val assessment =
            Assessment(tag = "testAssessment", gitCommitHash = "somehash1234", latest = true).apply {
                addSection(section1)
                addSection(section2)
            }

        // Act
        queryExecutor.withTransaction {
            queryExecutor.mergeEntities(listOf(assessment))
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
        assertNotEquals(0L, retrievedAssessment?.id, "Assessment ID not updated")
    }

    @Test
    fun `test uploadEntities persists multiple assignments`() {
        // Arrange
        val assignments = listOf(
           createAssignment(topic = "Topic", filename = "testFile1.md", type = AssignmentType.OPEN, availablePoints = 5, availableSeconds = 10),
           createAssignment(topic = "Topic", filename = "testFile2.md", type = AssignmentType.MULTIPLE_CHOICE, availablePoints = 10, availableSeconds = 30)
        )

        // Act
        queryExecutor.withTransaction {
            mergeEntities(assignments)
        }

        // Assert
        val result = TestQueryHelper.fetchAll<Assignment>(session)
        assertEquals(2, result.size, "Expected 2 assignments in the database")
    }

    @Test
    fun `test clearDatabase deletes assignments, sections, assessments`() {
        // Arrange: Add some sample data to the database
        val assignment =createAssignment(topic = "Topic 1", filename = "testFile.md", type = AssignmentType.OPEN, availablePoints = 2, availableSeconds = 23)
        val assessment = Assessment(tag = "test", gitCommitHash = "hash", latest = true).apply {
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
        val assignment1 =createAssignment(topic = "topic 1", filename = "file1.md", type = AssignmentType.OPEN, availablePoints = 3, availableSeconds = 900)
        val assignment2 =
           createAssignment(topic = "topic 2", filename = "file2.md", type = AssignmentType.MULTIPLE_CHOICE, availablePoints = 7, availableSeconds = 2134)
        val section = Section(title = "Test Section")
        val assessment = Assessment(
            tag = "test tag", gitCommitHash = "test commit hash",
            latest = true
        ).apply {
            addSection(section.apply {
                addAssignment(assignment1)
                addAssignment(assignment2)
            })
        }
        TestQueryHelper.persistEntity(assessment, session)

        // Act
        val result = queryExecutor.getLatestAssessmentByAssignmentIds(listOf(assignment1.id, assignment2.id))

        // Assert
        assertEquals(1, result.size, "Expected 1 assessment associated with the given assignments")
    }

    @Test
    fun `test getLatestAssessments only returns latest=true assessment`() {
        // Arrange
        val activeAssessment = Assessment(
            tag = "activeTag", gitCommitHash = "newerHash",
            latest = true,
        )
        val inactiveAssessment = Assessment(
            tag = "inactiveTag", gitCommitHash = "olderHash",
            latest = false,
        )
        TestQueryHelper.persistEntity(activeAssessment, session)
        TestQueryHelper.persistEntity(inactiveAssessment, session)

        // Act
        val retrievedAssessments = queryExecutor.getLatestAssessments()
        // Assert
        assertEquals(1, retrievedAssessments.size)
        assertEquals("activeTag", retrievedAssessments[0].tag!!)
        assertTrue(retrievedAssessments[0].isLatest)
    }

    @Test
    fun `test getLatestAssessment`() {
        // Arrange
        val latestAssessment = Assessment(
            tag = "latestTag", gitCommitHash = "latestHash",
            latest = true,
        )
        val olderAssessment = Assessment(
            tag = "latestTag", gitCommitHash = "olderHash",
            latest = false
        )
        TestQueryHelper.persistEntity(latestAssessment, session)
        TestQueryHelper.persistEntity(olderAssessment, session)

        // Act
        val result = queryExecutor.getLatestAssessment("latestTag")

        // Assert
        assertNotNull(result, "Expected a non-null latest assessment")
        assertTrue(result.isLatest, "Expected the assessment to be marked as latest")
    }

    @Test
    fun `test findAssignmentsByIds`() {
        // Arrange
        val assignment1 = createAssignment(topic = "test topic", filename = "test1.md", type = AssignmentType.OPEN, availablePoints = 5, availableSeconds = 3240)
        val assignment2 = createAssignment(topic = "different topic", filename = "test2.md", type = AssignmentType.MULTIPLE_CHOICE, availablePoints = 10, availableSeconds = 30)
        TestQueryHelper.persistEntity(assignment1, session)
        TestQueryHelper.persistEntity(assignment2, session)

        // Act
        val result = queryExecutor.findAssignmentsByIds(listOf(assignment1.id, assignment2.id))

        // Assert
        assertEquals(2, result.size, "Expected 2 assignments in the result")
        assertTrue(result.any { it.baseFilePath!!.endsWith("test1.md") }, "Expected assignment1 in the result")
        assertTrue(result.any { it.baseFilePath!!.endsWith("test2.md") }, "Expected assignment2 in the result")
    }

    @Test
    fun `test getLatestAssessmentsByHash returns correct assessments for valid hash`() {
        // Arrange
        val hash = "somehash1234"
        val validAssessment = Assessment(
            tag = "testTag", gitCommitHash = hash,
            latest = true
        )
        val invalidAssessment = Assessment(
            tag = "testTag", gitCommitHash = "otherhash",
            latest = null
        )

        // Persist the assessments
        TestQueryHelper.persistEntity(validAssessment, session)
        TestQueryHelper.persistEntity(invalidAssessment, session)

        // Act
        val result = queryExecutor.getLatestAssessmentsByHash(hash)

        // Assert
        assertEquals(1, result.size, "Expected only one assessment with the provided hash and latest=true")
        assertEquals(validAssessment.tag, result[0].tag, "Expected the valid assessment")
        assertTrue(result[0].isLatest, "Expected the returned assessment to be marked as latest")
    }

    @Test
    fun `test getLatestAssessmentsByHash returns empty list for non-existing hash`() {
        // Arrange
        val nonExistingHash = "nonexistenthash"

        // Act
        val result = queryExecutor.getLatestAssessmentsByHash(nonExistingHash)

        // Assert
        assertTrue(result.isEmpty(), "Expected an empty list for a non-existing hash")
    }


    @Test
    fun `test getLatestAssessmentsByHash excludes assessments where latest is false`() {
        // Arrange
        val hash = "somehash1234"
        val oldAssessment = Assessment(
            tag = "testTag", gitCommitHash = hash,
            latest = false
        )

        TestQueryHelper.persistEntity(oldAssessment, session)

        // Act
        val result = queryExecutor.getLatestAssessmentsByHash(hash)

        // Assert
        assertEquals(0, result.size, "Expected zero assessments with latest=true")
    }

    @Test
    fun `should update commit hash for latest assessments with null hashes`() {
        // Arrange
        val newHash = "SomeHash1244"
        // Create sample assessments with the old hash
        val assessment1 = Assessment(tag = "tag1", gitCommitHash = null, latest = true)
        val assessment2 = Assessment(tag = "tag2", gitCommitHash = null, latest = true)
        TestQueryHelper.persistEntity(assessment1, session)
        TestQueryHelper.persistEntity(assessment2, session)

        // Act
        queryExecutor.withTransaction{
            updateHashes(newHash)
        }
        session.close()
        session = sessionFactory.openSession()
        // Assert: Verify that both assessments have their commit hash updated
        val updatedAssessments = TestQueryHelper.fetchAll<Assessment>(session)
        assertEquals(2, updatedAssessments.size)
        assertEquals(newHash, updatedAssessments[0].gitCommitHash, "Assessment 1 commit hash was not updated")
        assertEquals(newHash, updatedAssessments[1].gitCommitHash, "Assessment 2 commit hash was not updated")
        session.close()
    }

    @Test
    fun `should not fail when no assessments have null hash`() {
        // Arrange
        val assessment1 = Assessment(tag = "tag1", gitCommitHash = "someHash", latest = true)
        val assessment2 = Assessment(tag = "tag2", gitCommitHash = "someOtherHash", latest = true)
        TestQueryHelper.persistEntity(assessment1, session)
        TestQueryHelper.persistEntity(assessment2, session)
        val newHash = "newhash1234"
        // Act
        queryExecutor.withTransaction{
            updateHashes(newHash)
        }
        session.close()
        session = sessionFactory.openSession()

        // Assert: No assessments should be changed
        val assessments = TestQueryHelper.fetchAll<Assessment>(session)
        val modifiedAssessments = assessments.filter { it.gitCommitHash == newHash }
        assertTrue(modifiedAssessments.isEmpty(), "No assessments should have the original commit hash")
    }

    @Test
    fun `should update commit hash for multiple assessments with the same hash`() {
        // Arrange
        val oldHash = "oldhash1234"
        val newHash = "newhash1234"

        // Create multiple assessments with the same old hash
        val assessment1 = Assessment(tag = "tag1", gitCommitHash = null, latest = true)
        val assessment2 = Assessment(tag = "tag2", gitCommitHash = null, latest = true)
        val assessment3 = Assessment(tag = "tag3", gitCommitHash = null, latest = true)
        TestQueryHelper.persistEntity(assessment1, session)
        TestQueryHelper.persistEntity(assessment2, session)
        TestQueryHelper.persistEntity(assessment3, session)

        // Act
        queryExecutor.withTransaction{
            updateHashes(newHash)
        }
        session.close()
        session = sessionFactory.openSession()
        // Assert: Verify all three assessments have their commit hash updated
        val updatedAssessments = TestQueryHelper.fetchAll<Assessment>(session)
        assertEquals(3, updatedAssessments.size)

        assertEquals(newHash, updatedAssessments[0].gitCommitHash, "Assessment 1 commit hash was not updated")
        assertEquals(newHash, updatedAssessments[1].gitCommitHash, "Assessment 2 commit hash was not updated")
        assertEquals(newHash, updatedAssessments[2].gitCommitHash, "Assessment 3 commit hash was not updated")
    }

    @Test
    fun `should update commit hash for assessments with sections and invites`() {
        // Arrange
        val oldHash = "oldhash1234"
        val newHash = "newhash1234"

        // Create multiple assessments with the same old hash
        val assessment1 = Assessment(tag = "tag1", gitCommitHash = null, latest = true).apply {addSection(Section(title="section 1"))}
        val assessment2 = Assessment(tag = "tag2", gitCommitHash = null, latest = true).apply {addSection(Section(title="section 2"))}
        TestQueryHelper.persistEntity(assessment1, session)
        TestQueryHelper.persistEntity(assessment2, session)
        val app1 = Applicant(name = "Aaron", preferredLanguage = "Kotlin", email = "aaron@example.com")
        val app2 = Applicant(name = "Zebediah", preferredLanguage = "F", email = "zebediah1@example.com")
        val app3 = Applicant(name = "Zebediah", preferredLanguage = "C", email = "zebediah2@example.com")


        TestQueryHelper.persistEntity(app1, session)
        TestQueryHelper.persistEntity(app2, session)
        TestQueryHelper.persistEntity(app3, session)

        // Act
        queryExecutor.withTransaction{
            updateHashes(newHash)
        }
        session.close()
        session = sessionFactory.openSession()
        // Assert: Verify all three assessments have their commit hash updated
        val updatedAssessments = TestQueryHelper.fetchAll<Assessment>(session)
        assertEquals(2, updatedAssessments.size)

        assertEquals(newHash, updatedAssessments[0].gitCommitHash, "Assessment 1 commit hash was not updated")
        assertEquals(newHash, updatedAssessments[1].gitCommitHash, "Assessment 2 commit hash was not updated")
    }
}
