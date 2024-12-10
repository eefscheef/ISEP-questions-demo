import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import javax.sql.DataSource
import ut.isep.management.model.entity.*

class DatabaseConfiguration {
    fun createDataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("JDBC_URL")
                ?: "jdbc:postgresql://isep-test-database.postgres.database.azure.com:5432/postgres?sslmode=require" //TODO remove once test with testcontainers added for local testing
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("DB_USERNAME")
            password = System.getenv("DB_PASSWORD")
            maximumPoolSize = 10
        }
        return HikariDataSource(config)
    }

    fun createSessionFactory(dataSource: DataSource): SessionFactory {
        val configuration = Configuration().apply {
            setProperty("hibernate.hbm2ddl.auto", "update")
            setProperty("hibernate.show_sql", "true")
            setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl")
            setProperty("hibernate.implicit_naming_strategy", "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl")

            addAnnotatedClass(Applicant::class.java)
            addAnnotatedClass(Assessment::class.java)
            addAnnotatedClass(Assignment::class.java)
            addAnnotatedClass(AssignmentCoding::class.java)
            addAnnotatedClass(AssignmentMultipleChoice::class.java)
            addAnnotatedClass(AssignmentOpen::class.java)
            addAnnotatedClass(Invite::class.java)
            addAnnotatedClass(Section::class.java)
            addAnnotatedClass(SolvedAssignment::class.java)
            addAnnotatedClass(SolvedAssignmentCoding::class.java)
            addAnnotatedClass(SolvedAssignmentMultipleChoice::class.java)
            addAnnotatedClass(SolvedAssignmentOpen::class.java)

        }

        // Build ServiceRegistry with the DataSource
        val serviceRegistry = StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.datasource", dataSource)
            .applySettings(configuration.properties)
            .build()

        // Build the SessionFactory
        return configuration.buildSessionFactory(serviceRegistry)
    }
}
