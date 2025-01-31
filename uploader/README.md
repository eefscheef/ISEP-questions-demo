# Parser

Not very intuitively named application (it doesn't contain a parser) that is used in the pipelines. AssessmentParser.kt can 
be used to compile all the question files into a list of Assessment entities that you could
persist to the database. AssessmentUpdater is meant to be used in the pipeline to synchronize the database
with updates to the question repository. They can be invoked with `./gradlew run ` with the following args:
```
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
```
Note that reset requires that none of the files in your repository have a _qid. Also, you should 
always commit any filename changes created by upload or reset, and then call the hash command with the 
latest commit. If you do not do this, the created Assessment entities will have their commitHash set to 
NULL and their assignments will be unreachable.

`upload`, `reset`, and `hash` calls require the environment variables DB_USERNAME, DB_PASSWORD, and optionally JDBC_URL 
to point to custom database

# Shared-entities

This contains the JPA entities that can be used to interface with the database. The question file parser also lives
here because it needs to be shared with all projects that have access to the database. The database only contains filenames,
so it is up to the consumer to make a call to the GitHub REST API and parse the resulting output.