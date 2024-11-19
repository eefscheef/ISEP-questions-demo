# Repository Asserberus Questions

This repository stores the questions in markdown and coding specific files. Coding questions consist of several directories with supporting files, such as src, test, images, and answers. Question 
maintainers should have access to this repository to add/change questions. Be aware that, the pipeline will fail if you don't use proper file formatting and repository structure. An example of a 
proper repository structure is given down below followed by an example of a valid `questions.md` formatted file.

Multiple-Choice and open questions should  be placed in the `questions.md` files in the root of a specific topic, such as 'General' or 'Database' in the example down below. Specific questions 
regarding a coding question should be placed inside the `question<nr>` directory in the `question.md` file.

In the file `config.yaml`, **tag**, **assessment**, and **question** options are stored which are allowed to be used in the markdown files.

Example repository structure: 
```txt
REPOSITORY_ROOT
├── General
│   └── questions.md
├── Database
│   ├── questions.md
│   └── code
│       ├── SQL
│       │   ├── question1
│       │   │   ├── question.md
│       │   │   ├── src
│       │   │   ├── images
│       │   │   ├── test
│       │   │   └── answers 
│       │   └── question2
│       │   │   ├── ...
│       │   │   ...
│       └── Java
│           ├── ...
│           ...
├── Encryption
│   └── ... 
├── config.yaml
└── README.md
```

Example file formatting (MultipleChoice / Open) of `questions.md`:
```markdown
***
<!-- Automatically generated ID for database reference. Do not modify!-->
<!-- id: unique-question-id-12345!-->
type: MultipleChoice

tags:
  - Frontend Developer
  - Backend Developer

description: What is the difference between a stack and a queue?

options:
- [ ] A stack is FIFO, a queue is LIFO.
- [x] A stack is LIFO, a queue is FIFO.
- [ ] Both are FIFO.
- [ ] Both are LIFO.
***
***
<!-- Automatically generated ID for database reference. Do not modify!-->
<!-- id: unique-question-id-35842!-->

type: Open

tags: 
  - Deezend 
  - developer
  - Reee

description: What is the difference between a stack and a queue?
***
```

Example file formatting (Coding) of `question.md`:
```markdown
***
<!-- Automatically generated ID for database reference. Do not modify!-->
<!-- id: unique-question-id-247462!-->
type: Coding

tags:
  - Frontend Developer

description: Improve the following code file?

... TODO: add the right information
***
```