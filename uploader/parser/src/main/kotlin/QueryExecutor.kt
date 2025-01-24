import org.hibernate.Session
import org.hibernate.query.criteria.HibernateCriteriaBuilder
import ut.isep.management.model.entity.*


class QueryExecutor(private val currentSession: Session) {
    private val cb: HibernateCriteriaBuilder = currentSession.criteriaBuilder

    fun clearDatabase() {
        currentSession.createNativeMutationQuery("TRUNCATE TABLE assessment CASCADE").executeUpdate()
        currentSession.createNativeMutationQuery("TRUNCATE TABLE section CASCADE").executeUpdate()
        currentSession.createNativeMutationQuery("TRUNCATE TABLE assignment CASCADE").executeUpdate()
    }

    fun <T> withTransaction(action: QueryExecutor.() -> T): T {
        val transaction = currentSession.beginTransaction()
        return try {
            val result = action() // 'this' in action refers to QueryExecutor
            transaction.commit()
            result
        } catch (exception: Exception) {
            transaction.rollback()
            throw exception
        }
    }

    fun <T: BaseEntity<*>> mergeEntities(entities: List<T>): List<T> {
        return entities.map { currentSession.merge(it) }
    }

    fun <T: BaseEntity<*>> persistEntities(entities: List<T>) {
        entities.forEach { currentSession.persist(it) }
    }


    fun getTagsOfLatestAssessmentsContainingAssignment(assignmentId: Long): List<String> {
        val query = cb.createQuery(String::class.java)
        val assessmentRoot = query.from(Assessment::class.java)

        // Join sections and assignments
        val sectionsJoin = assessmentRoot.join<Assessment, Section>("sections")
        val assignmentsJoin = sectionsJoin.join<Section, Assignment>("assignments")

        // Select assessment.assessmentID.tag where assignment.id = id and assessment.latest = true
        query.select(assessmentRoot.get("tag"))
            .distinct(true)
            .where(
                cb.and(
                    cb.equal(assignmentsJoin.get<Long>("id"), assignmentId),
                    cb.equal(assessmentRoot.get<Boolean>("latest"), true)
                )
            )
        return currentSession.createQuery(query).resultList
    }

    fun getLatestAssessmentByAssignmentIds(
        assignmentIds: List<Long>
    )
            : List<Assessment> {
        if (assignmentIds.isEmpty()) return emptyList()

        val query = cb.createQuery(Assessment::class.java)
        val assessmentRoot = query.from(Assessment::class.java)
        val sectionJoin = assessmentRoot.join<Assessment, Section>("sections")
        val assignmentJoin = sectionJoin.join<Section, Assignment>("assignments")
        query.select(assessmentRoot).distinct(true)
            .where(
                cb.and(
                    assignmentJoin.get<Long>("id").`in`(assignmentIds)
                ),
                cb.equal(assessmentRoot.get<Boolean>("latest"), true)
            )


        return currentSession.createQuery(query).resultList
    }

    fun findAssessmentsByAssignmentId(assignmentId: Long): List<Assessment> {
        return getLatestAssessmentByAssignmentIds(listOf(assignmentId))
    }

    fun findAssignmentsByIds(assignmentIds: List<Long>): List<Assignment> {
        if (assignmentIds.isEmpty()) return emptyList()

        val query = cb.createQuery(Assignment::class.java)
        val assignmentRoot = query.from(Assignment::class.java)

        query.select(assignmentRoot)
            .where(assignmentRoot.get<Long>("id").`in`(assignmentIds))

        val typedQuery = currentSession.createQuery(query)
        return typedQuery.resultList
    }

    fun getLatestAssessment(tag: String): Assessment {
        val query = cb.createQuery(Assessment::class.java)
        val assessmentRoot = query.from(Assessment::class.java)

        // Get all latest=true and tag=tag assessments
        query.select(assessmentRoot)
            .where(
                cb.and(
                    cb.equal(assessmentRoot.get<Boolean>("latest"), true),
                    assessmentRoot.get<String>("tag").equalTo(tag)
                )
            )
        return currentSession.createQuery(query).singleResult
    }

    fun getLatestAssessments(): List<Assessment> {
        val query = cb.createQuery(Assessment::class.java)
        val assessmentRoot = query.from(Assessment::class.java)

        // Get all latest=true assessments
        query.select(assessmentRoot)
            .where(
                cb.equal(assessmentRoot.get<Boolean>("latest"), true),
            )
        return currentSession.createQuery(query).resultList
    }

    fun getLatestAssessmentsByHash(hash: String): List<Assessment> {
        val query = cb.createQuery(Assessment::class.java)
        val assessmentRoot = query.from(Assessment::class.java)

        // Get all latest=true assessments
        query.select(assessmentRoot)
            .where(
                cb.and(
                    assessmentRoot.get<String>("gitCommitHash").equalTo(hash),
                    cb.equal(assessmentRoot.get<Boolean>("latest"), true)
                )
            )
        return currentSession.createQuery(query).resultList
    }

    fun updateHashes(newHash: String): Int {
        val updateAssessment = currentSession.createMutationQuery(
            """
        UPDATE Assessment a 
        SET a.gitCommitHash = :newHash 
        WHERE a.gitCommitHash IS NULL AND a.latest = true
        """
        )
        updateAssessment.setParameter("newHash", newHash)
        return updateAssessment.executeUpdate()
    }

    fun flush() = currentSession.flush()

    fun closeSession() = currentSession.close()

}
