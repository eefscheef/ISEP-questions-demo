package ut.isep

import DatabaseConfiguration
import DatabaseManager
import java.io.File

fun main(args: Array<String>) {
    val databaseConfig = DatabaseConfiguration()
    val dataSource = databaseConfig.createDataSource()
    val sessionFactory = databaseConfig.createSessionFactory(dataSource)
    val databaseManager = DatabaseManager(sessionFactory)


    val rootDir = File(System.getProperty("user.dir"))
    val configFile: File = rootDir.resolve("config.yaml")
    val assessments = AssessmentProcessor(rootDir, configFile).process()
    println(assessments)

    databaseManager.clearDatabase()
    databaseManager.uploadEntities(assessments)
}