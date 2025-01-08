package ut.isep

import Config
import DatabaseManager
import parser.Frontmatter
import parser.FrontmatterParser
import parser.QuestionFileHandler
import ut.isep.management.model.entity.*

class AssessmentUpdater(
    private val dbManager: DatabaseManager,
    private val parser: FrontmatterParser,
    private val commitHash: String
) {
    private val tagToUpdatedAssessment: MutableMap<String, Assessment> = mutableMapOf()

    fun addedQuestionFiles(addedFilenames: List<String>): AssessmentUpdater {
        return handleAddedAssignments(parseAssignments(addedFilenames).map { it.toNewAssignment() })
    }

    fun deletedQuestionFiles(deletedFilenames: List<String>): AssessmentUpdater {
        val ids = deletedFilenames.mapNotNull { filename ->
            QuestionFileHandler.getQuestionID(filename)
        }
        return handleDeletedAssignments(ids)
    }

    fun modifiedQuestionFiles(modifiedFilenames: List<String>): AssessmentUpdater {
        return processModifiedAssignments(parseAssignments(modifiedFilenames))
    }

    fun modifiedConfig(config: Config) {
        val inactiveAssessments: List<String> = dbManager.filterInactiveTags(config.tagOptions)
        val newAssessments = inactiveAssessments.map { tag ->
            Assessment(
                AssessmentID(
                    tag,
                    commitHash
                )
            ) // create empty assessments for the tags that are in config, but have no active assessments
        }
        dbManager.uploadEntities(newAssessments)
    }

    fun updatedAssessments(): List<Assessment> {
        return tagToUpdatedAssessment.values.toList()
    }

    fun upload() {
        return dbManager.uploadEntities(updatedAssessments())
    }

    private fun handleDeletedAssignments(deletedQuestionIds: List<Long>): AssessmentUpdater {
        if (deletedQuestionIds.isEmpty()) return this

        val assessmentsToUpdate = dbManager.findAssessmentsByAssignmentIds(deletedQuestionIds)
        assessmentsToUpdate.forEach { assessment ->
            deletedQuestionIds.forEach { deletedQuestionId ->
                deleteAssignmentFromAssessment(assessment, deletedQuestionId)
            }
        }
        return this
    }

    private fun deleteAssignmentFromAssessment(
        assessment: Assessment,
        deletedQuestionId: Long
    ) {
        val tag = assessment.id.tag ?: throw IllegalStateException("Retrieved null tag from Assessments table")
        val updatedAssessment = tagToUpdatedAssessment.getOrPut(tag) {
            assessment.copyWithoutCloningAssignments()
        }
        updatedAssessment.sections.forEach { section ->
            section.assignments.removeIf { assignment -> assignment.id == deletedQuestionId }
        }
    }

    private fun addAssignmentToAssessment(
        assessment: Assessment,
        assignment: Assignment,
    ) {
        val tag = assessment.id.tag ?: throw IllegalStateException("Retrieved null tag from Assessments table")
        val updatedAssessment = tagToUpdatedAssessment.getOrPut(tag) {
            assessment.copyWithoutCloningAssignments()
        }
        val sectionsToUpdate = updatedAssessment.sections.filter { section ->
            section.title == assignment.sectionTitle
        }
        if (sectionsToUpdate.isEmpty()) {
            val newSection = Section(title = assignment.sectionTitle).apply { this.addAssignment(assignment) }
            assessment.addSection(newSection)
        } else if (sectionsToUpdate.size > 1) {
            throw IllegalStateException("Assessments should not have multiple sections with the same title")
        } else {
            sectionsToUpdate.forEach { section ->
                section.addAssignment(assignment)
            }
        }
    }

    private fun getLatestAssessmentByTag(assessmentTag: String): Assessment {
        return tagToUpdatedAssessment[assessmentTag]
            ?: dbManager.getLatestAssessment(assessmentTag)
    }

    private fun processModifiedAssignments(frontmatters: List<Frontmatter>): AssessmentUpdater {
        if (frontmatters.isEmpty()) return this

        val ids: List<Long> = frontmatters.map {
            it.id ?: throw IllegalStateException(
                "Could not parse ID for modified question file at ${it.filePath}"
            )
        }
        val existingAssignments: Map<Long, Assignment> = dbManager.findAssignmentsByIds(ids).associateBy(Assignment::id)
        val differentAssignments: MutableList<Assignment> = mutableListOf()

        for (frontmatter: Frontmatter in frontmatters) {
            val existingAssignment = existingAssignments[frontmatter.id]
                ?: throw IllegalStateException(
                    "For modified file with ID ${frontmatter.id} there is no existing assignment in the database"
                )

            val updatedAssignment = frontmatter.toModifiedAssignment()
            if (updatedAssignment.assignmentType != existingAssignment.assignmentType || updatedAssignment.filePath != existingAssignment.filePath) {
                differentAssignments.add(existingAssignment)
            }
            val existingTags = existingAssignment.sections.map { it.assessment!!.id.tag!! }
            val addedTags: List<String> = frontmatter.tags - existingTags.toSet()
            addedTags.forEach { tag ->
                addAssignmentToAssessment(getLatestAssessmentByTag(tag), updatedAssignment)
            }
            val removedTags: List<String> = existingTags - frontmatter.tags.toSet()
            removedTags.forEach { tag ->
                deleteAssignmentFromAssessment(getLatestAssessmentByTag(tag), updatedAssignment.id)
            }
        }
        return handleModifiedAssignments(differentAssignments)
    }

    private fun handleModifiedAssignments(modifiedAssignments: List<Assignment>): AssessmentUpdater {

        modifiedAssignments.forEach { changedAssignment ->
            val affectedAssessments = dbManager.findAssessmentsByAssignmentId(changedAssignment.id)
            affectedAssessments.forEach { assessment ->
                val tag = assessment.id.tag ?: throw IllegalStateException("Retrieved null tag from Assessments table")
                val updatedAssessment = tagToUpdatedAssessment.getOrPut(tag) {
                    assessment.copyWithoutCloningAssignments()
                }
                updatedAssessment.sections.forEach { section ->
                    section.assignments.replaceAll {
                        if (it.id == changedAssignment.id) changedAssignment.apply { it.id = 0 } else it
                    }
                }
            }
        }
        return this
    }

    private fun handleAddedAssignments(addedAssignments: List<Assignment>): AssessmentUpdater {
        val newAssessments = mutableListOf<Assessment>()
        addedAssignments.forEach { assignment ->
            val assessments = dbManager.findAssessmentsByAssignmentIds(listOfNotNull(assignment.id))
            assessments.forEach { assessment ->
                val newSections = assessment.sections.map { section ->
                    Section(
                        title = section.title,
                        assignments = section.assignments.toMutableList().apply { add(assignment) }
                    )
                }
                newAssessments.add(Assessment(AssessmentID(assessment.id.tag, commitHash), newSections.toMutableList()))
            }
        }
        return this
    }

    private fun Frontmatter.toModifiedAssignment(): Assignment {
        if (filePath == null) {
            throw IllegalStateException("Frontmatter object must have filepath before being converted to modified Assignment")
        }
        val assignmentType = AssignmentType.valueOf(type)
        return id?.let { Assignment(id = it, filePath = filePath, assignmentType = assignmentType) }
            ?: throw IllegalStateException("Frontmatter object must have id before being converted to modified Assignment")
    }

    private fun Frontmatter.toNewAssignment(): Assignment {
        if (filePath == null) {
            throw IllegalStateException("Frontmatter object must have filepath before being converted to entity")
        }
        val assignmentType = AssignmentType.valueOf(type)
        return Assignment(filePath = filePath, assignmentType = assignmentType)
    }

    private fun Assessment.copyWithoutCloningAssignments(): Assessment {
        val newAssessment = Assessment(
            id = AssessmentID(this.id.tag, commitHash),
            sections = mutableListOf(), // Temporarily empty; will be populated below
        )
        val clonedSections = sections.map { section ->
            //TODO check if these sections are loaded or not.
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
            parser.parseQuestion(filename)
        }
    }
}