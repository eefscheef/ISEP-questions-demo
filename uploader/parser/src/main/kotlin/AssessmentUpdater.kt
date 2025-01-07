package ut.isep

import Config
import DatabaseManager
import parser.Frontmatter
import parser.FrontmatterParser
import parser.QuestionFileHandler
import ut.isep.management.model.entity.*

class AssessmentUpdater(private val dbManager: DatabaseManager, private val parser: FrontmatterParser, private val commitHash: String) {
    private val tagToUpdatedAssessment: MutableMap<String, Assessment> = mutableMapOf()

    fun addedQuestionFiles(addedFilenames: List<String>): AssessmentUpdater {
        return handleAddedAssignments(parseAssignments(addedFilenames).map{it.toNewAssignment()})
    }

    fun deletedQuestionFiles(deletedFilenames: List<String>): AssessmentUpdater {
        val ids = deletedFilenames.mapNotNull {filename ->
            QuestionFileHandler.getQuestionID(filename)
        }
        return handleDeletedAssignments(ids)
    }

    fun modifiedQuestionFiles(modifiedFilenames: List<String>): AssessmentUpdater {
        return handleModifiedAssignments(parseAssignments(modifiedFilenames))
    }

    fun modifiedConfig(config: Config) {
        val inactiveAssessments: List<String> = dbManager.filterInactiveTags(config.tagOptions)
        val newAssessments = inactiveAssessments.map { tag ->
            Assessment(AssessmentID(tag, commitHash)) // create empty assessments for the tags that are in config, but have no active assessments
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
            deletedQuestionIds.forEach {deletedQuestionId ->
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
        updatedAssessment.sections.first {section ->
            section.title == assignment.sections.first().title
        }
        updatedAssessment.sections.forEach { section ->
            section.assignments.add
        }
    }



    private fun handleModifiedAssignments(frontmatters: List<Frontmatter>): AssessmentUpdater {
        if (frontmatters.isEmpty()) return this

        val ids: List<Long> = frontmatters.map {
            it.id ?: throw IllegalStateException("Modified question files should always have a valid ID in their filename")
        }
        val existingAssignments: Map<Long, Assignment> = dbManager.findAssignmentsByIds(ids).associateBy(Assignment::id)
        // All assignments whose persistent properties differ from the already stored Assignment's properties
        val differentAssignments: MutableList<Assignment> = mutableListOf()

        for (frontmatter: Frontmatter in frontmatters) {
            val existingAssignment = existingAssignments[frontmatter.id]
                ?: throw IllegalStateException("Modified question files should always have a valid ID in their filename")

            val updatedAssignment = frontmatter.toModifiedAssignment()
            if (existingAssignment != updatedAssignment) {
                val updatedTags = existingAssignment.sections.map{it.assessment!!.id.tag!!}
                val newTags: List<String> = frontmatter.tags - updatedTags.toSet()
                val removedTags: List<String> = updatedTags - frontmatter.tags.toSet()
                differentAssignments.add(existingAssignment)
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
                        if (it.id == changedAssignment.id) changedAssignment.apply {it.id = 0} else it
                    }
                }
            }
        }
        return this
    }

    private fun handleAddedAssignments(addedAssignments: List<Assignment>): AssessmentUpdater {
        val newAssessments = mutableListOf<Assessment>()
        addedAssignments.forEach { question ->
            question.tags.forEach { tag ->
                val assessments = dbManager.findAssessmentsByAssignmentIds(listOfNotNull(question.id))
                assessments.forEach { assessment ->
                    val newSections = assessment.sections.map { section ->
                        Section(
                            title = section.title,
                            assignments = section.assignments.toMutableList().apply { add(question.toEntity()) }
                        )
                    }
                    newAssessments.add(Assessment(AssessmentID(tag, commitHash), newSections.toMutableList()))
                }
            }
        }
        return newAssessments
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
        return filenames.map {filename ->
            parser.parseQuestion(filename)
        }
    }
}