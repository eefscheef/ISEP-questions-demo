import org.hibernate.Session
import org.hibernate.SessionFactory
import ut.isep.management.model.entity.*


class DatabaseManager(private val sessionFactory: SessionFactory) {

    fun getSession(): Session {
        return sessionFactory.openSession()
    }

    fun clearDatabase() {
        getSession().use { session ->
            try {
                session.beginTransaction()

                // Raw SQL queries for truncating tables
                session.createNativeMutationQuery("TRUNCATE TABLE assessment CASCADE").executeUpdate()
                session.createNativeMutationQuery("TRUNCATE TABLE section CASCADE").executeUpdate()
                session.createNativeMutationQuery("TRUNCATE TABLE assignment CASCADE").executeUpdate()

                // Commit the transaction
                session.transaction.commit()
            } catch (e: Exception) {
                // Rollback in case of an error
                session.transaction.rollback()
                throw e
            }
        }
    }


    fun uploadEntities(entities: List<BaseEntity<*>>) {
        sessionFactory.openSession().use { session ->
            val transaction = session.beginTransaction()
            try {
                entities.forEach { session.persist(it) }
                transaction.commit()
            } catch (exception: Exception) {
                transaction.rollback()
                throw exception
            }
        }
    }

    fun findAssessmentsByAssignmentIds(
        assignmentIds: List<Long>
    ): List<Assessment> {
        if (assignmentIds.isEmpty()) return emptyList()
        sessionFactory.openSession().use { session ->
            val cb = session.criteriaBuilder
            val query = cb.createQuery(Assessment::class.java)
            val assessmentRoot = query.from(Assessment::class.java)
            val sectionJoin = assessmentRoot.join<Assessment, Section>("sections")
            val assignmentJoin = sectionJoin.join<Section, Assignment>("assignments")
            query.select(assessmentRoot).distinct(true)
                .where(assignmentJoin.get<Long>("id").`in`(assignmentIds))

            return session.createQuery(query).resultList
        }
    }

    fun findAssessmentsByAssignmentId(assignmentId: Long): List<Assessment> {
        return findAssessmentsByAssignmentIds(listOf(assignmentId))
    }

    fun findAssignmentsByIds(assignmentIds: List<Long>): List<Assignment> {
        if (assignmentIds.isEmpty()) return emptyList()

        sessionFactory.openSession().use { session ->
            val cb = session.criteriaBuilder
            val query = cb.createQuery(Assignment::class.java)
            val assignmentRoot = query.from(Assignment::class.java)

            query.select(assignmentRoot)
                .where(assignmentRoot.get<Long>("id").`in`(assignmentIds))

            val typedQuery = session.createQuery(query)
            return typedQuery.resultList
        }
    }

    fun filterInactiveTags(tags: List<String>): List<String> {
        if (tags.isEmpty()) return emptyList()
        // Open a Hibernate session
        sessionFactory.openSession().use { session ->
            val cb = session.criteriaBuilder
            val query = cb.createQuery(Assessment::class.java)
            val assessmentRoot = query.from(Assessment::class.java)

            // Build the query to find all active assessments with a tag in the provided list
            query.select(assessmentRoot)
                .where(
                    cb.and(
                        cb.equal(assessmentRoot.get<Boolean>("active"), true),
                        assessmentRoot.get<AssessmentID>("id").get<String>("tag").`in`(tags)
                    )
                )
            // Execute the query and collect the matching tags
            val existingTags = session.createQuery(query)
                .resultList
                .mapNotNull { it.id.tag }
            // Return the tags that do not have a corresponding active assessment
            return tags.filterNot { it in existingTags }
        }
    }

    fun getLatestAssessment(tag: String): Assessment {
        getSession().use {session ->
            val cb = session.criteriaBuilder
            val query = cb.createQuery(Assessment::class.java)
            val assessmentRoot = query.from(Assessment::class.java)

            // Build the query to find all active assessments with a tag in the provided list
            query.select(assessmentRoot)
                .where(
                    cb.and(
                        cb.equal(assessmentRoot.get<Boolean>("latest"), true),
                        assessmentRoot.get<AssessmentID>("id").get<String>("tag").equalTo(tag)
                    )
                )
            // Execute the query and collect the matching tags
            return session.createQuery(query).singleResultOrNull
        }
    }

}
