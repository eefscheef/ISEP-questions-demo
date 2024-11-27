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

Example file formatting of (multiple-choice question) `your-question-name.md`:
```markdown
---
type: multiple-choice
tags:
  - Frontend Developer
  - Backend Developer
---
What is the difference between a stack and a queue?

- [ ] A stack is FIFO, a queue is LIFO.
- [x] A stack is LIFO, a queue is FIFO.
- [ ] Both are FIFO.
- [ ] Both are LIFO.
```
Example file formatting (open question) of `your-question-name.md`:
```markdown
---
type: open
tags:
- Deezend
- developer
- Reee
---
What is the difference between a stack and a queue?
```

Example file formatting (coding question) of `your-question-name.md`:
```markdown
---
id: unique-question-id-247462  #Automatically generated ID for  database reference, do not modify!
type: Coding

tags:
  - Frontend Developer
---
description: Improve the following code file?

... TODO: add the right information
```

Note that after successful transfer of the questions to the database, and ID is injected into the frontmatter portion of 
the question file. This ID is used as a primary key in the questions database, and should therefore not be modified.

```markdown
---
id: unique-question-id-35842 #Automatically generated ID for database reference, do not modify!
type: open
tags:
- Deezend
- developer
- Reee
---
What is the difference between a stack and a queue?
```