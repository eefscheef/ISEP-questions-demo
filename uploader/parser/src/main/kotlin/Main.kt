package ut.isep

import java.io.File

fun main() {
    val rootDir = File("..") // for gradle, running from parser/
//    val rootDir = File(".") // for IntelliJ, when running from ISEP-questions/
    val configFile: File = rootDir.resolve("config.yaml")
    val assessments = AssessmentProcessor(rootDir, configFile).process()
    println(assessments)
}
