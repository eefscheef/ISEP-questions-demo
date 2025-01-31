import org.hibernate.Session
import parser.Config
import parser.Frontmatter
import parser.FrontmatterParser
import parser.QuestionIDUtil
import parser.QuestionParsingException
import ut.isep.management.model.entity.Assessment
import ut.isep.management.model.entity.Assignment
import ut.isep.management.model.entity.Section
import java.io.File

/**
 * The class responsible for updating assessments, assignments, and sections in the database
 * based on changes to markdown files in the questions repository.
 *
 * This class processes added, deleted, and modified filenames, updates the database accordingly,
 * and ensures that adding/updating/deleting question files is updated in the database state
 *
 * @property session Hibernate Session used to interact with the database.
 * @property parser FrontmatterParser used to parse persisted attributes of question files
 * @property queryExecutor helper class with predefined useful queries
 */
class AssessmentUpdater(
    private val session: Session,
    private val parser: FrontmatterParser = FrontmatterParser(),
    private val queryExecutor: QueryExecutor = QueryExecutor(session)
) {
    internal val tagToNewAssessment: MutableMap<String, Assessment> = mutableMapOf()
    internal val frontmatterToNewAssignment: MutableMap<Frontmatter, Assignment> = mutableMapOf()
    internal val deactivedAssessments: MutableSet<Assessment> = mutableSetOf()

    /**
     * Updates assessments based on added, deleted, and modified filenames.
     *
     * @param addedFilenames A list of filenames that were added to the repository.
     * @param deletedFilenames A list of filenames that were deleted from the repository.
     * @param modifiedFilenames A list of filenames that were modified in the repository.
     * @param config An optional configuration object containing tag options.
     */
    fun updateAssessments(
        addedFilenames: List<String> = listOf(),
        deletedFilenames: List<String> = listOf(),
        modifiedFilenames: List<String> = listOf(),
        config: Config? = null,
    ) {
        tagToNewAssessment.clear()
        frontmatterToNewAssignment.clear()
        deactivedAssessments.clear()
        queryExecutor.apply {
            if (config != null) {
                updateConfig(config)
            }
            if (modifiedFilenames.isNotEmpty()) {
                modifyAssignments(parseAssignments(modifiedFilenames))
            }
            if (addedFilenames.isNotEmpty()) {
                addAssignments(parseAssignments(addedFilenames))
            }
            if (deletedFilenames.isNotEmpty()) {
                val ids = deletedFilenames.mapNotNull { filename ->
                    QuestionIDUtil.parseQuestionID(filename)
                }
                deleteAssignments(ids)
            }
            upload()
        }
    }

    /**
     * Updates the configuration by creating new assessments for new tags and deactivating assessments for deleted tags.
     *
     * @param config The configuration object containing tag options.
     */
    private fun updateConfig(config: Config) {
        val currentActiveAssessmentsByTag: Map<String, Assessment> =
            queryExecutor.getLatestAssessments().associateBy { it.tag!! }
        val currentTags: Set<String> = currentActiveAssessmentsByTag.keys
        val newTags: Set<String> = config.tagOptions.subtract(currentTags)
        // Create empty assessments for the tags that are in config, but have no active assessments
        val newAssessments = newTags.associateWith { tag ->
            Assessment(
                tag = tag,
                gitCommitHash = null,
                latest = true
            )
        }
        val deletedTags: Set<String> = currentTags.subtract(config.tagOptions.toSet())
        // Mark assessments whose tags were deleted from the config as inactive
        val deletedAssessments = deletedTags.map { deletedTag ->
            currentActiveAssessmentsByTag[deletedTag]
                ?: throw IllegalStateException("Could not find assessment with tag $deletedTag to mark as deactivated")
        }

        tagToNewAssessment.putAll(newAssessments)
        deactivedAssessments.addAll(deletedAssessments)
    }

    /**
     * Uploads changes to the database, including deactivating old assessments and persisting new ones.
     *
     * @return A list of newly created assessments.
     */
    private fun upload(): List<Assessment> {
        deactivedAssessments.forEach { assessment ->
            assessment.latest = null
        }
        queryExecutor.mergeEntities(deactivedAssessments.toList())
        queryExecutor.flush() // Flush here so deactivated assessment.latest doesn't violate unique constraint with new assessments.latest
        frontmatterToNewAssignment.keys.forEach { frontmatter ->
            val strippedFilename = QuestionIDUtil.removeQuestionIDIfPresent(frontmatter.originalFilePath)
            File(frontmatter.originalFilePath).renameTo(File(strippedFilename))
        }
        queryExecutor.persistEntities(frontmatterToNewAssignment.values.toList())
        return queryExecutor.mergeEntities(tagToNewAssessment.values.toList())
    }

    /**
     * Deletes assignments from assessments based on the provided list of question IDs.
     *
     * @param deletedQuestionIds A list of question IDs to delete.
     */
    private fun deleteAssignments(deletedQuestionIds: List<Long>) {
        if (deletedQuestionIds.isEmpty()) return

        val assessmentsToUpdate = queryExecutor.getLatestAssessmentByAssignmentIds(deletedQuestionIds)
        assessmentsToUpdate.forEach { assessment ->
            deletedQuestionIds.forEach { deletedQuestionId ->
                deleteAssignmentFromAssessment(assessment, deletedQuestionId)
            }
        }
    }

    /**
     * Deletes an assignment from a specific assessment.
     *
     * @param assessment The assessment from which to delete the assignment.
     * @param deletedAssignmentId The ID of the assignment to delete.
     */
    private fun deleteAssignmentFromAssessment(
        assessment: Assessment,
        deletedAssignmentId: Long
    ) {
        val tag = assessment.tag!!
        val updatedAssessment = tagToNewAssessment.getOrPut(tag) {
            assessment.copyWithoutCloningAssignments()
        }
        var removed = false
        updatedAssessment.sections.forEach { section ->
            if (section.removeAssignmentById(deletedAssignmentId)) {
                removed = true
            }
        }
        if (!removed) {
            throw IllegalArgumentException("Couldn't remove assignment with ID $deletedAssignmentId from assessment ${assessment.id}")
        }
    }

    /**
     * Adds an assignment to a specific assessment.
     *
     * @param assessment The assessment to which the assignment should be added.
     * @param assignment The assignment to add.
     */
    private fun addAssignmentToAssessment(
        assessment: Assessment,
        assignment: Assignment,
    ) {
        val tag = assessment.tag!!
        val updatedAssessment = tagToNewAssessment.getOrPut(tag) {
            assessment.copyWithoutCloningAssignments()
        }
        if (updatedAssessment.latest == null || updatedAssessment.latest == false) {
            throw IllegalStateException("Attempted to add assignment ${assignment.id} to non-latest assessment ${assessment.id}")
        }
        val sectionsToUpdate = updatedAssessment.sections.filter { section ->
            section.title == assignment.sectionTitle
        }
        if (sectionsToUpdate.isEmpty()) {
            val newSection = Section(title = assignment.sectionTitle).apply { this.addAssignment(assignment) }
            updatedAssessment.addSection(newSection)
        } else if (sectionsToUpdate.size > 1) {
            throw IllegalStateException("Assessments should not have multiple sections with the same title")
        } else {
            sectionsToUpdate[0].addAssignment(assignment)
        }
    }

    /**
     * Retrieves the latest assessment for a given tag.
     *
     * @param assessmentTag The tag for which to retrieve the latest assessment.
     * @return The latest assessment for the given tag.
     */
    private fun getLatestAssessmentByTag(assessmentTag: String): Assessment {
        println("Finding latest assessment for tag $assessmentTag")
        return tagToNewAssessment[assessmentTag]
            ?: queryExecutor.getLatestAssessment(assessmentTag)
    }

    /**
     * Modifies assignments based on changes to their frontmatter.
     *
     * @param frontmatters A list of frontmatter objects representing modified assignments.
     */
    private fun modifyAssignments(frontmatters: List<Frontmatter>) {
        if (frontmatters.isEmpty()) return

        val ids: List<Long> = frontmatters.map {
            it.id ?: throw IllegalStateException(
                "Could not find ID for modified question file at ${it.originalFilePath}"
            )
        }
        val existingAssignments: Map<Long, Assignment> =
            queryExecutor.findAssignmentsByIds(ids).associateBy(Assignment::id)

        for (frontmatter: Frontmatter in frontmatters) {
            val existingAssignment = existingAssignments[frontmatter.id]
                ?: throw IllegalStateException(
                    "For modified file with ID ${frontmatter.id} there is no existing assignment in the database"
                )
            val existingTags = queryExecutor.getTagsOfLatestAssessmentsContainingAssignment(frontmatter.id!!)

            val updatedAssignment = if (existingAssignment.persistedAttributesDifferFrom(frontmatter)) {
                createNewAssignment(frontmatter).also {
                    createNewAssessmentsWithModifiedAssignment(frontmatter.id!!, it) // This creates new assessments, so we need to get existingTags before doing this
                }
            } else {
                existingAssignment
            }

            val addedTags: List<String> = frontmatter.tags - existingTags.toSet()
            addedTags.forEach { tag ->
                addAssignmentToAssessment(getLatestAssessmentByTag(tag), updatedAssignment)
            }
            val removedTags: List<String> = existingTags - frontmatter.tags.toSet()
            removedTags.forEach { tag ->
                deleteAssignmentFromAssessment(getLatestAssessmentByTag(tag), frontmatter.id!!)
            }
        }
    }

    /**
     * Creates new assessments with a modified assignment.
     *
     * @param id The ID of the modified assignment.
     * @param newAssignment The new assignment to replace the old one.
     */
    private fun createNewAssessmentsWithModifiedAssignment(id: Long, newAssignment: Assignment) {
        val affectedAssessments = queryExecutor.findAssessmentsByAssignmentId(id)
        affectedAssessments.forEach { assessment ->
            val tag = assessment.tag!!
            val updatedAssessment = tagToNewAssessment.getOrPut(tag) {
                assessment.copyWithoutCloningAssignments()
            }
            updatedAssessment.sections.forEach { section ->
                section.assignments.replaceAll { oldAssignment ->
                    if (oldAssignment.id == id) newAssignment else oldAssignment
                }
            }
        }
    }

    /**
     * Adds new assignments to existing and new assessments using their parsed frontmatter data.
     *
     * @param frontmatters A list of frontmatter objects representing new assignments.
     */
    private fun addAssignments(frontmatters: List<Frontmatter>) {
        frontmatters.forEach { frontmatter ->
            if (frontmatter.id != null) throw FileParsingException(
                "Added files should not have a qid in their filename",
                frontmatter.originalFilePath
            )
            val affectedAssessments = frontmatter.tags.map { tag -> getLatestAssessmentByTag(tag) }
            affectedAssessments.forEach { assessment ->
                val tag = assessment.tag!!
                val updatedAssessment = tagToNewAssessment.getOrPut(tag) {
                    assessment.copyWithoutCloningAssignments()
                }
                val newAssignment = createNewAssignment(frontmatter)
                val affectedSections = updatedAssessment.sections.filter { oldSection ->
                    oldSection.title == newAssignment.sectionTitle
                }
                if (affectedSections.isNotEmpty()) {
                    affectedSections.forEach { oldSection ->
                        oldSection.addAssignment(newAssignment)
                    }
                } else {
                    updatedAssessment.addSection(Section(title = newAssignment.sectionTitle).apply {
                        addAssignment(newAssignment)
                    })
                }
            }
        }
    }

    /**
     * Creates a new assignment based on the provided frontmatter.
     *
     * @param frontmatter The frontmatter object containing assignment details.
     * @return The newly created assignment.
     */
    private fun createNewAssignment(frontmatter: Frontmatter): Assignment {
        val newAssignment = Assignment(
            baseFilePath = frontmatter.baseFilePath,
            assignmentType = frontmatter.type,
            availablePoints = frontmatter.availablePoints,
            availableSeconds = frontmatter.availableSeconds
        )
        frontmatterToNewAssignment[frontmatter] = newAssignment
        return newAssignment
    }

    /**
     * Checks if the persisted attributes of an assignment differ from its frontmatter.s
     * persisted attributes are assignmentType, availablePoints, availableSeconds
     *
     * @param frontmatter The frontmatter object to compare against.
     * @return `true` if the attributes differ, `false` otherwise.
     */
    private fun Assignment.persistedAttributesDifferFrom(frontmatter: Frontmatter): Boolean {
        return this.assignmentType != frontmatter.type
                || this.availablePoints != frontmatter.availablePoints
                || this.availableSeconds != frontmatter.availableSeconds
    }

    /**
     * Creates a copy of an assessment without cloning its assignments.
     *
     * @return A new assessment with the same tag, and new sections containing the same
     * assignments as the receiver Assessment.
     */
    private fun Assessment.copyWithoutCloningAssignments(): Assessment {
        deactivedAssessments.add(this)
        val newAssessment = Assessment(
            tag = this.tag,
            gitCommitHash = null,
            sections = mutableListOf(), // Temporarily empty; will be populated below
            latest = true
        )
        val clonedSections = sections.map { section ->
            Section(
                title = section.title,
                assignments = section.assignments.toMutableList() // Retain original assignment references
            ).also { it.assessment = newAssessment } // Point to the new assessment
        }
        newAssessment.sections.addAll(clonedSections)
        return newAssessment
    }

    /**
     * Parses a list of filenames into frontmatter objects.
     *
     * @param filenames A list of filenames to parse.
     * @return A list of frontmatter objects.
     * @throws QuestionParsingException
     */
    private fun parseAssignments(filenames: List<String>): List<Frontmatter> {
        return filenames.map { filename ->
            parser.parse(File(filename).readText(), filename).first
        }
    }
}