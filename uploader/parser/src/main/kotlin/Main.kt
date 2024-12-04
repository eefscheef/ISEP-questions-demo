package ut.isep

import java.io.File

fun main() {
    val rootDir = File(System.getProperty("user.dir"))
    val configFile: File = rootDir.resolve("config.yaml")
    val assessments = AssessmentProcessor(rootDir, configFile).process()
    println(assessments)
}
