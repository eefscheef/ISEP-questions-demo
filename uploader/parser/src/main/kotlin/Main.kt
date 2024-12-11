import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess



const val USAGE = """
Usage:
  validate [--all] [<file>...]
  upload [--all] [--deleted] [<file>...] [--added] [<file>...] [--updated] [<file>...]

Options:
  -h --help     Show this screen.
  --all -a         Validate or upload all questions in the repository (mutually exclusive with <file>...).

Commands:
  validate      Validate the specified question files.
  upload        Parse the specified files and upload them to the database.
  
Arguments:
  <file>... List of files to process. Cannot be used with --all.
"""


val config = parseConfig()
val parser = QuestionParser(config)

private fun printUsageAndExit(status: Int) {
    println(USAGE)
    exitProcess(status)
}


fun main(args: Array<String>) {
    if (args.size < 2) {
        printUsageAndExit(1)
    }
    val processEntireRepo = args[1] == "--all" || args[1] == "-a"
    if (processEntireRepo && args.size != 2) { // Can't provide filenames with --all flag set
        printUsageAndExit(1)
    }
    val command = args[0]
    val filenames = args.drop(1)
    when (command) {
        "help, --help, -h" -> printUsageAndExit(0)
        "validate" -> if (processEntireRepo) validateAll() else filenames.forEach {parser.parseQuestion(readFile(it))}
        "upload" -> if (processEntireRepo) uploadAll() else {upload(filenames)}
        else -> printUsageAndExit(1)
    }
}

private fun validateAll() {

}

private fun validate (filenames: List<String>) {
    return
}

private fun uploadAll() {

}

private fun upload(filenames: List<String>) {
    return
}


fun parseConfig(): Config {
    val mapper = ObjectMapper(YAMLFactory())
    return mapper.readValue(File("config.yaml"), Config::class.java)
}

fun readFile(filename: String): String {
    val parser = QuestionParser(parseConfig())
    val file: File = File(filename)
    if (!file.exists()) {
        println("$filename: No such file $filename")
        exitProcess(1)
    }
    if (!file.canRead()) {
        println("$filename: Can't read file $filename, check your permissions")
        exitProcess(1)
    }
    return try {file.readText()} catch (e: IOException) {
        println("An I/O error occurred: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}