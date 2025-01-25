import org.hibernate.SessionFactory
import parser.Config
import parser.Frontmatter
import parser.FrontmatterParser
import parser.QuestionIDUtil
import ut.isep.management.model.entity.Assessment
import ut.isep.management.model.entity.Assignment
import ut.isep.management.model.entity.Section
import java.io.File

class AssessmentUpdater(
    private val sessionFactory: SessionFactory,
) {
    private val parser = FrontmatterParser()
    private val queryExecutor: QueryExecutor by lazy { QueryExecutor(sessionFactory.openSession()) }
    private val tagToNewAssessment: MutableMap<String, Assessment> = mutableMapOf()
    private val frontmatterToNewAssignment: MutableMap<Frontmatter, Assignment> = mutableMapOf()
    private val deactivedAssessments: MutableSet<Assessment> = mutableSetOf()

    fun updateAssessments(
        addedFilenames: List<String> = listOf(),
        deletedFilenames: List<String> = listOf(),
        modifiedFilenames: List<String> = listOf(),
        config: Config? = null,
    ) {
        tagToNewAssessment.clear()
        frontmatterToNewAssignment.clear()
        queryExecutor.withTransaction {
            if (config != null) {
                updateConfig(config)
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
            if (modifiedFilenames.isNotEmpty()) {
                modifyAssignments(parseAssignments(modifiedFilenames))
            }
            upload()
        }
        queryExecutor.closeSession()
    }

    private fun updateConfig(config: Config) {
        val currentActiveAssessmentsByTag: Map<String, Assessment> =
            queryExecutor.getLatestAssessments().associateBy { it.tag!! }
        val currentTags: Set<String> = currentActiveAssessmentsByTag.keys
        val newTags: Set<String> = config.tagOptions.subtract(currentTags)
        // create empty assessments for the tags that are in config, but have no active assessments
        val newAssessments = newTags.associateWith { tag ->
            Assessment(
                tag = tag,
                gitCommitHash = null,
                latest = true
            )
        }
        val deletedTags: Set<String> = currentTags.subtract(config.tagOptions.toSet())
        // mark assessments whose tags were deleted from the config as inactive
        val deletedAssessments = deletedTags.associateWith { deletedTag ->
            currentActiveAssessmentsByTag[deletedTag]?.apply { latest = null }
                ?: throw IllegalStateException("Could not find assessment with tag $deletedTag to mark as latest=false")
        }

        tagToNewAssessment.putAll(newAssessments)
        tagToNewAssessment.putAll(deletedAssessments)
    }

    private fun upload(): List<Assessment> {
        queryExecutor.mergeEntities(deactivedAssessments.toList())
        queryExecutor.flush() // Flush here so deactivated assessment.latest doesn't violate unique constraint with new assessments.latest
        queryExecutor.persistEntities(frontmatterToNewAssignment.values.toList())
        return queryExecutor.mergeEntities(tagToNewAssessment.values.toList())
    }

    private fun deleteAssignments(deletedQuestionIds: List<Long>) {
        if (deletedQuestionIds.isEmpty()) return

        val assessmentsToUpdate = queryExecutor.getLatestAssessmentByAssignmentIds(deletedQuestionIds)
        assessmentsToUpdate.forEach { assessment ->
            deletedQuestionIds.forEach { deletedQuestionId ->
                deleteAssignmentFromAssessment(assessment, deletedQuestionId)
            }
        }
    }

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
            throw IllegalArgumentException("Couldn't remove assignmet with ID $deletedAssignmentId from assessment ${assessment.id}")
        }
    }

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

    private fun getLatestAssessmentByTag(assessmentTag: String): Assessment {
        println("Finding latest assessment for tag $assessmentTag")
        return tagToNewAssessment[assessmentTag]
            ?: queryExecutor.getLatestAssessment(assessmentTag)
    }

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

    private fun Assignment.persistedAttributesDifferFrom(frontmatter: Frontmatter): Boolean {
        return this.assignmentType != frontmatter.type
                || this.availablePoints != frontmatter.availablePoints
                || this.availableSeconds != frontmatter.availableSeconds
    }

    private fun Assessment.copyWithoutCloningAssignments(): Assessment {
        this.latest = null // copied assignment is no longer latest
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


    private fun parseAssignments(filenames: List<String>): List<Frontmatter> {
        return filenames.map { filename ->
            parser.parse(File(filename).readText(), filename).first
        }
    }
}