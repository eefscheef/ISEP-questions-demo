import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess


const val USAGE = """
Usage:
  validate [--all] [<file>...]
  upload [--all] [--deleted <file>...] [--added <file>...] [--updated <file>...]

Options:
  -h --help     Show this screen.
  --all -a      Validate or upload all questions in the repository (mutually exclusive with <file>...).

Commands:
  validate      Validate the specified question files.
  upload        Parse the specified files and upload them to the database.

Arguments:
  <file>... List of files to process. Cannot be used with --all.
"""
const val CONFIG_FILEPATH = "config.yaml"

val configFile = File(CONFIG_FILEPATH)
val parser = QuestionParser(configFile)

private fun printUsageAndExit() {
    println(USAGE)
    exitProcess(1)
}

private fun validateAll() {

}

fun validate(files: List<String>) {
    files.forEach { filename ->
        try {
            parser.parseFile(File(filename))
        } catch (e: QuestionParsingException) {
            throw FileParsingException("Invalid question file", filename, e)
        }
    }
}

private fun uploadAll() {

}


fun main(args: Array<String>) {
    if (args.size < 2) {
        printUsageAndExit()
    }
    val processEntireRepo = args[1] == "--all" || args[1] == "-a"
    if (processEntireRepo && args.size != 2) { // Can't provide filenames with --all flag set
        printUsageAndExit()
    }

    val command = args[0]
    val arguments = args.drop(1).toMutableList()

    when (command) {
        "help", "--help", "-h" -> printUsageAndExit()
        "validate" -> handleValidateCommand(processEntireRepo, arguments)
        "upload" -> handleUploadCommand(processEntireRepo, arguments)
        else -> printUsageAndExit()
    }
    exitProcess(0)
}

fun handleValidateCommand(processEntireRepo: Boolean, filenames: List<String>) {
    if (processEntireRepo) {
        validateAll()
    } else {
        validate(filenames)
    }
}

fun handleUploadCommand(processEntireRepo: Boolean, arguments: List<String>) {
    if (processEntireRepo) {
        uploadAll()
    } else {
        val (deletedFiles, addedFiles, updatedFiles) = parseUploadArguments(arguments)
        validateUploadArguments(deletedFiles, addedFiles, updatedFiles)
        upload(deletedFiles, addedFiles, updatedFiles)
    }
}

fun parseUploadArguments(arguments: List<String>): Triple<List<String>, List<String>, List<String>> {
    val deletedFiles = mutableListOf<String>()
    val addedFiles = mutableListOf<String>()
    val updatedFiles = mutableListOf<String>()

    var currentList: MutableList<String>? = null

    for (arg in arguments) {
        when (arg) {
            "--deleted" -> currentList = deletedFiles
            "--added" -> currentList = addedFiles
            "--updated" -> currentList = updatedFiles
            else -> if (arg.startsWith("--")) {
                printUsageAndExit()
            } else {
                currentList?.add(arg) ?: printUsageAndExit() // No option provided
            }
        }
    }

    return Triple(deletedFiles, addedFiles, updatedFiles)
}

fun validateUploadArguments(deletedFiles: List<String>, addedFiles: List<String>, updatedFiles: List<String>) {
    if (deletedFiles.isEmpty() && addedFiles.isEmpty() && updatedFiles.isEmpty()) {
        println("Error: At least one of --deleted, --added, or --updated must be provided with files.")
        printUsageAndExit()
    }
}


fun upload(deletedFiles: List<String>, addedFiles: List<String>, updatedFiles: List<String>) {
    println("Uploading changes:")
    if (deletedFiles.isNotEmpty()) println("Deleted files: $deletedFiles")
    if (addedFiles.isNotEmpty()) println("Added files: $addedFiles")
    if (updatedFiles.isNotEmpty()) println("Updated files: $updatedFiles")
}


