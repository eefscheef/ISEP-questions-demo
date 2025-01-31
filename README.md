# Demo Repository Asserberus Questions

This repository stores the questions in markdown and coding specific files. Coding questions consist of several directories with supporting files, such as src, test, images, and answers. Question 
maintainers should have access to this repository to add/change questions. Be aware that, the pipeline will fail if you don't use proper file formatting and repository structure. An example of a 
proper repository structure is given down below followed by an example of a valid `questions.md` formatted file.

Multiple-Choice and open questions should  be placed in the `questions.md` files in the root of a specific topic, such as 'General' or 'Database' in the example down below. Specific questions 
regarding a coding question should be placed in subdirectory, with a markdown question file pointing to the relevant code files.
In the file `config.yaml`, **assessment**, is stored, which defines all assessments that can be referred to in question files.
Example repository structure: 
```txt
REPOSITORY_ROOT
├── General
│   ├── question1.md
│   └── another-question.md
├── Database
│   ├── question.md
│   └── javaCodingQuestion
│       ├── question.md
│       ├── startCode.java
│       ├── test.Java
│       └── secretTest.java
├── Encryption
│   └── ... 
├── config.yaml
└── README.md
```

Example file formatting of (multiple-choice question) `your-question-name.md`:
```markdown
---
type: multiple-choice

tags:
- Java Developer
points: 1 
seconds: 600
---

What is the purpose of the final keyword in Java when applied to a class?

- [X] To prevent the class from being inherited
- [ ] To create an immutable class
- [ ] To make the class static
- [ ] To define a constant value
```
Example file formatting (open question) of `your-question-name.md`:
```markdown
---
type: open
tags:
  - Java Developer
points: 1
seconds: 600
reference-answer: |
  ArrayList: Backed by a resizable array.
  LinkedList: Uses a doubly-linked list.
---
Explain the differences between ArrayList and LinkedList in Java. When would you use one over the other?
```

Example file formatting (coding question) of `your-question-name.md`:
```markdown
---
tags:
  - Python Developer
language: Python
type: coding
code: palindrome.py
points: 3
seconds: 600
test: test_palindrome.py
secret-test: secret_test_palindrome.py
reference-code: reference_palindrome.py
---

Implement a Function to Check for Palindrome
```

Note that after successful transfer of the questions to the database, and ID is injected into the filename of a question as an appended _qid{id}.md
This ID is used as a primary key in the questions database, and should therefore not be modified.

# Pipelines
This repository contains two workflows: 
### parse-updated-questions
This workflow runs on Pull Request to main. Checks all the changed question files to see if they can be parsed by 
the QuestionParser in uploader/shared-entities. Fails if there is a parse error.
## upload-questions-update-ids
This workflow runs on successful PR to main (commit). It updates the database with the modified assignments and assessments.
See more details in the [Uploader README](uploader/parser/README.md).