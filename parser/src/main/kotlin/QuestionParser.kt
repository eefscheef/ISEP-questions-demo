package ut.isep

import MultipleChoiceQuestion
import OpenQuestion
import Option
import Question


class QuestionParser {

    fun parseQuestions(input: String): List<Question> {
        // Split input into blocks using "***" as the delimiter, trimming whitespace
        val questionBlocks = input.split("***").map { it.trim() }.filter { it.isNotEmpty() }
        val questions = mutableListOf<Question>()

        for (block in questionBlocks) {

            // Regex to match the ID in the format `<!-- id: some-id! -->`
            val idRegex = """<!-- id: ([^!]+)! -->""".toRegex()
            // Explanation:
            // - `<!-- id: ` matches the literal start of the ID comment
            // - `([^!]+)` captures everything up to the `!` (the ID value)
            // - `! -->` matches the end of the comment
            val id = idRegex.find(block)?.groups?.get(1)?.value

            // Regex to match the type of the question
            val typeRegex = """type: ([\w-]+)""".toRegex()
            // Explanation:
            // - `type: ` matches the literal "type:" prefix
            // - `([\w-]+)` captures one or more word characters or hyphens (e.g., "multiple-choice" or "open")
            val type = typeRegex.find(block)?.groups?.get(1)?.value?.lowercase()

            // Regex to capture tags, ending at "description:"
            val tagsRegex = """tags:\s*-\s*(.*?)description:""".toRegex(RegexOption.DOT_MATCHES_ALL)
            // Explanation:
            // - `tags:\s*` matches "tags:" followed by optional whitespace
            // - `-\s*` matches the dash starting the tags list, followed by optional whitespace
            // - `(.*?)` lazily captures everything (tags) up to "description:"
            // - `description:` ensures the regex stops at the next field
            val tags = tagsRegex.find(block)?.groups?.get(1)?.value
                ?.split("-") // Split tags by "-" delimiter
                ?.map { it.trim() } // Trim whitespace from each tag
                ?: emptyList() // Default to an empty list if no tags are found

            // Regex to capture the description, stopping at "options:" or the end of the string
            val descriptionRegex = """(?s)description:\s*(.*?)(?=\s*options:|\z)""".toRegex()
            // Explanation:
            // - `(?s)` enables dot-all mode, allowing `.` to match newlines
            // - `description:\s*` matches "description:" followed by optional whitespace
            // - `(.*?)` lazily captures everything (description content)
            // - `(?=\s*options:|\z)` is a lookahead that stops capturing when "options:" or the end of the string is encountered
            val description = descriptionRegex.find(block)?.groups?.get(1)?.value?.trim() ?: ""

            if (type == "multiple-choice") {
                // Regex to match options in the format `- [ ] Option text` or `- [x] Correct option`
                val optionsRegex = """(?m)^\s*-\s*\[([x ])]\s*(.*?)\s*$""".toRegex()
                // Explanation:
                // - `options:\s*` matches "options:" followed by optional whitespace
                // - `-\s*\[(x ]` matches the option prefix `- [x]` or `- [ ]` (capturing "x" if correct)
                // - `\s*(.*?)\n` captures the option text, stopping at the end of the line
                val options = optionsRegex.findAll(block).map {
                    Option(
                        text = it.groups[2]?.value ?: "", // Capture the option text
                        isCorrect = it.groups[1]?.value == "x" // Check if the option is marked correct
                    )
                }.toList()
                questions.add(
                    MultipleChoiceQuestion(
                        id = id,
                        tags = tags,
                        description = description,
                        options = options
                    )
                )
            } else if (type == "open") {
                questions.add(
                    OpenQuestion(
                        id = id,
                        tags = tags,
                        description = description
                    )
                )
            }
        }
        return questions
    }
}
