import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import parser.FrontmatterParser
import parser.QuestionParser
import ut.isep.AssessmentParser
import ut.isep.AssessmentUpdater
import java.io.File
import kotlin.system.exitProcess


const val USAGE = """
Usage:
  validate [--all] [<file>...]
  upload [--all] [--deleted <file>...] [--added <file>...] [--updated <file>...] --commit <hash>

Options:
  -h --help     Show this screen.
  --all -a      Validate or upload all questions in the repository (mutually exclusive with <file>...).
  --commit      Specify the commit hash for the upload operation. This option is required when using the `upload` command.

Commands:
  validate      Validate the specified question files.
  upload        Parse the specified files and upload them to the database. Requires a commit hash using `--commit <hash>`.

Arguments:
  <file>... List of files to process. Cannot be used with --all.
"""
const val CONFIG_FILEPATH = "config.yaml"
private val configFile = File(CONFIG_FILEPATH)
private val config: Config = parseConfig()

private fun parseConfig(): Config {
    return ObjectMapper(YAMLFactory()).readValue(configFile, Config::class.java)
}

val parser = QuestionParser(FrontmatterParser(config))

private fun printUsageAndExit(): Nothing {
    println(USAGE)
    exitProcess(1)
}

private fun validateAll() {

}

private fun getQuestionID(filenames: List<String>): List<Long> {
    val regex = """.*?_qid(\d+)""".toRegex()

    return filenames.mapNotNull { filename ->
        regex.find(filename)?.groupValues?.get(1)?.toLongOrNull()
    }
}

fun validate(files: List<String>) {
    files.forEach { filename ->
        try {
            parser.parseQuestion(filename)
        } catch (e: QuestionParsingException) {
            throw FileParsingException("Invalid question file", filename, e)
        }
    }
}

private fun uploadAll(commitHash: String) {
    val databaseConfig = DatabaseConfiguration(AzureDatabaseConfigProvider())
    val dataSource = databaseConfig.createDataSource()
    val sessionFactory = databaseConfig.createSessionFactory(dataSource)
    val databaseManager = DatabaseManager(sessionFactory)

    val assessmentProcessor = AssessmentParser(File("."), parser)
    databaseManager.clearDatabase()
    databaseManager.uploadEntities(assessmentProcessor.parseAll(commitHash))
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
    val parsedArguments = parseUploadArguments(arguments)
    if (processEntireRepo) {
        uploadAll(parsedArguments.commitHash ?: printUsageAndExit()) // Must provide commit hash
    } else {
        validateUploadArguments(parsedArguments)
        upload(parsedArguments)
    }
}

fun parseUploadArguments(arguments: List<String>): Arguments {
    val deletedFiles = mutableListOf<String>()
    val addedFiles = mutableListOf<String>()
    val updatedFiles = mutableListOf<String>()
    var commitHash: String? = null

    var currentList: MutableList<String>? = null
    var i = 0

    while (i < arguments.size) {
        when (val arg = arguments[i]) {
            "--deleted" -> currentList = deletedFiles
            "--added" -> currentList = addedFiles
            "--updated" -> currentList = updatedFiles
            "--commit" -> {
                if (i + 1 < arguments.size) {
                    commitHash = arguments[i + 1]
                    i++ // Skip the next argument since it's the hash
                } else {
                    printUsageAndExit() // Handle missing hash
                }
            }
            else -> {
                if (arg.startsWith("--")) {
                    printUsageAndExit()
                } else {
                    currentList?.add(arg) ?: printUsageAndExit() // No option provided
                }
            }
        }
        i++
    }

    return Arguments(
        deletedFiles = deletedFiles,
        addedFiles = addedFiles,
        updatedFiles = updatedFiles,
        commitHash = commitHash
    )
}


data class Arguments(
    val deletedFiles: List<String>,
    val addedFiles: List<String>,
    val updatedFiles: List<String>,
    val commitHash: String?
)

fun validateUploadArguments(arguments: Arguments) {
    if (arguments.deletedFiles.isEmpty() && arguments.addedFiles.isEmpty() && arguments.updatedFiles.isEmpty()) {
        println("Error: At least one of --deleted, --added, or --updated must be provided with files.")
        printUsageAndExit()
    }
}


fun upload(arguments: Arguments) {
    val databaseConfig = DatabaseConfiguration(AzureDatabaseConfigProvider())
    val dataSource = databaseConfig.createDataSource()
    val sessionFactory = databaseConfig.createSessionFactory(dataSource)
    val dbManager = DatabaseManager(sessionFactory)
    validateUploadArguments(arguments)
    println("Uploading changes:")
    if (arguments.deletedFiles.isNotEmpty()) {
        println("Deleted files: ${arguments.deletedFiles}")

    }
    if (arguments.addedFiles.isNotEmpty()) {
        println("Added files: ${arguments.addedFiles}")
    }

    if (arguments.updatedFiles.isNotEmpty()) {
        println("Updated files: ${arguments.updatedFiles}")
    }
}


