package ut.isep

import org.testcontainers.containers.PostgreSQLContainer

object PostgresTestContainer {
    val instance: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:17.2").apply {
            withDatabaseName("testdb")
            withUsername("testuser")
            withPassword("testpass")
            start()
        }
    }
}
