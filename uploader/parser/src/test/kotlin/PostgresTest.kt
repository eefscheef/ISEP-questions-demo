package ut.isep

import DatabaseConfiguration
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import ut.isep.management.model.entity.Assignment
import ut.isep.management.model.entity.AssignmentType
import javax.sql.DataSource
import kotlin.test.assertEquals

class PostgresTest {


    companion object {
        private lateinit var postgresContainer: PostgreSQLContainer<*>
        private lateinit var session: Session

        @BeforeAll
        @JvmStatic
        fun setup() {
            postgresContainer = PostgreSQLContainer<Nothing>("postgres:13.3").apply {
                withDatabaseName("testdb")
                withUsername("testuser")
                withPassword("testpass")
                start()
            }

            val dbConfig = DatabaseConfiguration(TestContainersDatabaseConfigProvider())
            val dataSource: DataSource = dbConfig.createDataSource()
            val sessionFactory: SessionFactory = dbConfig.createSessionFactory(dataSource)
            session = sessionFactory.openSession()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            session.close()
            postgresContainer.stop()
        }
    }

    @Test
    fun `test inserting and retrieving a user`() {
        val assignment = Assignment(filePath = "testPath", assignmentType = AssignmentType.OPEN, availablePoints = 2)
        val transaction = session.beginTransaction()
        try {
            session.persist(assignment)
            transaction.commit()
        } catch (exception: Exception) {
            transaction.rollback()
            throw exception
        }
        val cb = session.criteriaBuilder
        val query = cb.createQuery(Assignment::class.java)
        val assignmentRoot = query.from(Assignment::class.java)

        query.select(assignmentRoot)
        // Execute the query and collect the matching tags
        val result = session.createQuery(query).singleResultOrNull
        assertEquals(assignment.filePath, result.filePath)
        assertEquals(assignment.assignmentType, result.assignmentType)
        assertEquals(assignment.availablePoints, result.availablePoints)
    }
}