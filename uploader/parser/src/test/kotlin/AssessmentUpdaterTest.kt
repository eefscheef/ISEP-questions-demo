package ut.isep

import AssessmentUpdater
import QueryExecutor
import io.mockk.*
import io.mockk.junit5.MockKExtension
import io.mockk.impl.annotations.MockK
import org.hibernate.Session
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import parser.Config
import parser.Frontmatter
import parser.FrontmatterParser
import ut.isep.management.model.entity.Assessment
import ut.isep.management.model.entity.Assignment
import ut.isep.management.model.entity.AssignmentType
import ut.isep.management.model.entity.BaseEntity
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AssessmentUpdaterTest(
    @MockK private val session: Session,
    @MockK private val queryExecutor: QueryExecutor,
    @MockK private val parser: FrontmatterParser
) : BaseUpdaterTest() {

    private lateinit var updater: AssessmentUpdater

    @BeforeEach
    fun setUp() {
        updater = AssessmentUpdater(session, parser, queryExecutor)

        every { queryExecutor.mergeEntities<BaseEntity<*>>(any()) } returns emptyList()
        every { queryExecutor.persistEntities<BaseEntity<*>>(any()) } just Runs
        every { queryExecutor.flush() } just Runs
    }

    @Test
    fun `updateConfig should create new assessments for new tags`() {
        every { queryExecutor.getLatestAssessments() } returns emptyList()
        every { queryExecutor.mergeEntities<BaseEntity<*>>(any()) } returns emptyList()
        every { queryExecutor.persistEntities<BaseEntity<*>>(any()) } just Runs
        val updater = AssessmentUpdater(session, parser, queryExecutor)

        val config = Config(tagOptions = listOf("new-tag"))

        updater.updateAssessments(config = config)

        assertEquals(1, updater.tagToNewAssessment.size)
        assertEquals("new-tag", updater.tagToNewAssessment.keys.first())
    }

    @Test
    fun `updateAssessments should create new assessments for modified filenames`() {
        // Mock behaviors for modification
        val id = 1L
        val type = AssignmentType.OPEN
        val tag = "tag"

        val filename = createTestFile("Topic", "modified-assignment_qid$id.md").path
        val modifiedFilenames = listOf(filename)
        val frontmatter = Frontmatter(type, listOf(tag), 10, 600).apply {
            this.id = id
            this.originalFilePath = filename
        }
        every { parser.parse(any(), any()) } returns Pair(frontmatter, filename)
        every { queryExecutor.getLatestAssessments() } returns listOf(mockk())
        every { queryExecutor.findAssignmentsByIds(any())} returns listOf(Assignment(id = id))


        val assessment = Assessment(tag = tag)
        every { queryExecutor.findAssessmentsByAssignmentId(id)} returns listOf(assessment)
        every { queryExecutor.getTagsOfLatestAssessmentsContainingAssignment(id) } returns listOf(tag)

        // Call updateAssessments with modified filenames
        updater.updateAssessments(modifiedFilenames = modifiedFilenames)

        // Validate modification behavior
        verify { queryExecutor.findAssignmentsByIds(listOf(id)) }
        verify { queryExecutor.getTagsOfLatestAssessmentsContainingAssignment(id) }

        assert(updater.deactivedAssessments.contains(assessment))
        assert(updater.tagToNewAssessment.keys.contains(tag))
        assert(updater.frontmatterToNewAssignment.keys.map(Frontmatter::id).contains(id))

    }

    @Test
    fun `updateAssessments should process all changes correctly`() {
        // Mock behaviors for all cases
        val addedFilenames = listOf("new-assignment.md")
        val deletedFilenames = listOf("deleted-assignment.md")
        val modifiedFilenames = listOf("modified-assignment.md")
        val frontmatter = mockk<Frontmatter>(relaxed = true)

        every { parser.parse(any(), any()) } returns Pair(frontmatter, "")
        every { queryExecutor.getLatestAssessments() } returns listOf(mockk())
        every { queryExecutor.getLatestAssessmentByAssignmentIds(any()) } returns listOf(mockk())

        // Call updateAssessments with all changes
        updater.updateAssessments(
            addedFilenames = addedFilenames,
            deletedFilenames = deletedFilenames,
            modifiedFilenames = modifiedFilenames
        )

        // Verify all interactions
        verify { queryExecutor.mergeEntities<BaseEntity<*>>(any()) }
        verify { queryExecutor.getLatestAssessmentByAssignmentIds(any()) }
        verify { queryExecutor.findAssignmentsByIds(any()) }
        assertTrue(true)
    }


    @Test
    fun `updateAssessments should not add assessments and assignments or deactivate assessments when config is null or empty`() {
        // Call updateAssessments with no config
        updater.updateAssessments()

        // Assert no new assessments were added
        assertTrue(updater.tagToNewAssessment.isEmpty())
        assertTrue(updater.deactivedAssessments.isEmpty())
        assertTrue(updater.frontmatterToNewAssignment.isEmpty())
    }
}