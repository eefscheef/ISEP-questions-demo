package ut.isep

import DatabaseConfiguration
import QueryExecutor
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import javax.sql.DataSource

abstract class BaseIntegrationTest {

    companion object {
        @JvmStatic
        protected lateinit var sessionFactory: SessionFactory

        @BeforeAll
        @JvmStatic
        fun startContainer() {
            val dbConfig = DatabaseConfiguration(TestDatabaseConfigProvider)
            val dataSource: DataSource = dbConfig.createDataSource()
            this.sessionFactory = dbConfig.createSessionFactory(dataSource)
        }

        @AfterAll
        @JvmStatic
        fun stopContainer() {
            sessionFactory.close()
        }
    }

    protected lateinit var session: Session
    protected lateinit var queryExecutor: QueryExecutor

    @BeforeEach
    protected open fun openSession() {
        this.session = sessionFactory.openSession()
        this.queryExecutor = QueryExecutor(session)
        clearDatabase()
    }

    @AfterEach
    protected open fun closeSession() {
        queryExecutor.closeSession()
    }

    protected fun clearDatabase() = queryExecutor.withTransaction {
        queryExecutor.clearDatabase()
    }
}

