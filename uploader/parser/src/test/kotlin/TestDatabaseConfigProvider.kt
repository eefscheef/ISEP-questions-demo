package ut.isep

import DatabaseConfigProvider


object TestDatabaseConfigProvider : DatabaseConfigProvider {
    private val container = PostgresTestContainer.instance

    override fun getJdbcUrl(): String = container.jdbcUrl
    override fun getUsername(): String = container.username
    override fun getPassword(): String = container.password
}