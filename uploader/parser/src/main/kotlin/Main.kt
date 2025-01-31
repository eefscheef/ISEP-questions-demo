import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import parser.*
import java.io.File
import kotlin.system.exitProcess


const val USAGE = """
Usage:
  validate [--all] [<file>...]
  upload [--deleted <file>...] [--added <file>...] [--updated <file>...] [--config]
  hash --commit <hash>
  reset
 

Options:
  -h --help     Show this screen.
  --all -a      Validate or upload all questions in the repository (mutually exclusive with <file>...). WARNING: DELETES ALL EXISTING ASSIGNMENTS
  --commit      Specify the commit hash for the upload operation. This option is required when using the `upload` command.
  --config      Indicates that the config file was modified and new assessments might need to be created

Commands:
  validate      Validate the specified question files.
  upload        Parse the specified files and upload them to the database. Requires a commit hash using `--commit <hash>`.
  hash          Provide the uploaded assessments with the latest commit hash. 
  reset         CLEARS THE DATABASE OF ASSESSMENTS, AND THEIR ASSOCIATED INVITES, SOLUTIONS, AND RESULTS. 
                Then parses the entire repository and uploads the found assessments.
 

Arguments:
  <file>... List of files to process. Cannot be used with --all.
"""
const val CONFIG_FILEPATH = "config.yaml"
private val configFile = File(CONFIG_FILEPATH)
private val config: Config = parseConfig()

private fun parseConfig(): Config {
    return ObjectMapper(YAMLFactory()).readValue(configFile, Config::class.java)
}

private fun printUsageAndExit(): Nothing {
    println(USAGE)
    exitProcess(1)
}

private fun validateAll() {
    val validator = TagValidator(config)
    val frontmatterParser = FrontmatterParser(validator)
    val questionParser = QuestionParser(frontmatterParser)
    AssessmentParser(File("questions"), questionParser).parseAll()
    println("All questions validated")
}

fun handleValidateCommand(args: List<String>) {
    val processEntireRepo = args[0] == "--all" || args[1] == "-a"
    if (processEntireRepo && args.size > 1) { // Can't provide filenames with --all flag set
        printUsageAndExit()
    }
    if (processEntireRepo) {
        validateAll()
        return
    }
    val files = args.drop(1)
    files.forEach { filename ->
        try {
            if (File(filename).extension == "md") {
                val validator = TagValidator(config)
                val frontmatterParser = FrontmatterParser(validator)
                QuestionParser(frontmatterParser).parseFile(File(filename))
                println("Question in $filename validated")
            } else {
                println("Non-markdown file $filename ignored")
            }
        } catch (e: QuestionParsingException) {
            throw FileParsingException("Invalid question file", filename, e)
        }
    }
}


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsageAndExit()
    }

    val command = args[0]
    val arguments = args.drop(1)

    when (command) {
        "help", "--help", "-h" -> printUsageAndExit()
        "validate" -> handleValidateCommand(arguments)
        "upload" -> handleUploadCommand(arguments)
        "hash" -> handleHashCommand(arguments)
        "reset" -> handleResetCommand()
        else -> printUsageAndExit()
    }
    exitProcess(0)
}

fun handleHashCommand(arguments: List<String>) {
    if (arguments.size != 2 || arguments[0] != "--commit") {
        printUsageAndExit()
    }
    val databaseConfig = DatabaseConfiguration(AzureDatabaseConfigProvider())
    val dataSource = databaseConfig.createDataSource()
    databaseConfig.createSessionFactory(dataSource).use { sessionFactory ->
        sessionFactory.openSession().use { session ->
            val count = QueryExecutor(session).withTransaction {
                updateHashes(arguments[1])
            }
            println("Updated $count assessments with hash ${arguments[1]}")
        }
    }
}


fun handleResetCommand() {
    val assessmentProcessor = AssessmentParser(File("questions"), QuestionParser())
    val databaseConfig = DatabaseConfiguration(AzureDatabaseConfigProvider())
    val dataSource = databaseConfig.createDataSource()
    val sessionFactory = databaseConfig.createSessionFactory(dataSource)
    sessionFactory.openSession().use {session ->
        val queryExecutor = QueryExecutor(session)
        queryExecutor.withTransaction {
            clearDatabase()
            persistEntities(assessmentProcessor.parseAll())
        }
    }
}

fun handleUploadCommand(arguments: List<String>) {
    val parsedArguments = parseUploadArguments(arguments)
    validateUploadArguments(parsedArguments)
    upload(parsedArguments)
}

fun parseUploadArguments(arguments: List<String>): Arguments {
    val deletedFiles = mutableListOf<String>()
    val addedFiles = mutableListOf<String>()
    val updatedFiles = mutableListOf<String>()
    var configFlag = false
    var currentList: MutableList<String>? = null
    var i = 0

    while (i < arguments.size) {
        when (val arg = arguments[i]) {
            "--deleted" -> currentList = deletedFiles
            "--added" -> currentList = addedFiles
            "--updated" -> currentList = updatedFiles

            "--config" -> configFlag = true
            else -> {
                if (arg.startsWith("--")) {
                    printUsageAndExit()
                } else {
                    currentList?.add(arg) // No option provided
                }
            }
        }
        i++
    }

    return Arguments(
        deletedFiles = deletedFiles,
        addedFiles = addedFiles,
        updatedFiles = updatedFiles,
        isConfigChanged = configFlag
    )
}


data class Arguments(
    val deletedFiles: List<String>,
    val addedFiles: List<String>,
    val updatedFiles: List<String>,
    val isConfigChanged: Boolean = false
)

fun validateUploadArguments(arguments: Arguments) {
    if (arguments.deletedFiles.isEmpty() && arguments.addedFiles.isEmpty() && arguments.updatedFiles.isEmpty()) {
        println("No files provided. Exiting.")
        exitProcess(0)
    }
}


fun upload(arguments: Arguments) {
    val (deletedFiles, addedFiles, updatedFiles, isConfigChanged) = arguments
    val changedConfig = if (isConfigChanged) config else null
    validateUploadArguments(arguments)
    if (addedFiles.isNotEmpty()) {
        println("Added files: ${arguments.addedFiles}")
    }
    if (deletedFiles.isNotEmpty()) {
        println("Deleted files: ${arguments.deletedFiles}")
    }
    if (updatedFiles.isNotEmpty()) {
        println("Updated files: ${arguments.updatedFiles}")
    }
    val databaseConfig = DatabaseConfiguration(AzureDatabaseConfigProvider())
    val dataSource = databaseConfig.createDataSource()
    databaseConfig.createSessionFactory(dataSource).openSession().use { session ->
        val updater = AssessmentUpdater(session)
        updater.updateAssessments(addedFiles, deletedFiles, updatedFiles, changedConfig)
        println("Uploading changes:")
    }
}
