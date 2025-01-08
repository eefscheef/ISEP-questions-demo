package ut.isep

import DatabaseConfigProvider
import org.testcontainers.containers.PostgreSQLContainer


class TestContainersDatabaseConfigProvider : DatabaseConfigProvider {
    class TestContainersPostgres : PostgreSQLContainer<TestContainersPostgres>("postgres:17.2")

    private val container = TestContainersPostgres().apply { start() }

    override fun getJdbcUrl(): String = container.jdbcUrl
    override fun getUsername(): String = container.username
    override fun getPassword(): String = container.password
    override fun isTestEnvironment(): Boolean = true
}