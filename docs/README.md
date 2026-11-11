# PantryWise documentation

PantryWise is an Android app (Java + XML) that helps a household reduce food waste. This folder
documents what the project is, what is implemented, how to run it, and how it is specified, designed and
built. It was written for the Wilfrid Laurier course **CP470 - Android Programming** (Group 7).

> Status: **complete** for the follow-up delivery. Every document is written and was verified against the code.

## How to read it

| If you want to... | Read | Status |
|---|---|---|
| Understand what PantryWise is and who built it | [01 - Project overview](01-project-overview.md) | written |
| See what is implemented and what is not | [02 - Features and status](02-features-status.md) | written |
| Run the app and the backend on your machine | [03 - Getting started](03-getting-started.md) | written |
| Know what comes next | [04 - Roadmap](04-roadmap.md) | written |
| Read the requirements (use cases, user stories, state and activity diagrams) | [Specification](engineering/01-specification.md) | written |
| Read the design (class list, class and object diagrams, data model) | [Design](engineering/02-design.md) | written |
| See the architecture diagrams | [Architecture](engineering/03-architecture.md) | written |
| Understand the AI kitchen assistant and recipe engine | [AI engine](engineering/04-ai-engine.md) | written |
| Learn how the project is tested | [Testing](engineering/05-testing.md) | written |

## Conventions

- **Diagrams** are PNG images in [`diagrams/img/`](diagrams/img/), so they show in any Markdown viewer without a renderer.
  Their Mermaid sources are in [`diagrams/src/`](diagrams/src/). See [`diagrams/README.md`](diagrams/README.md) to regenerate them.
- **Feature status** uses three words: **Implemented** (visible in the code and covered by a test or a manual check),
  **Partial** (some acceptance criteria are missing) and **Planned** (not started).
- Classes are named by their simple name (for example `PantryRepository`) so the text survives refactors.
  The Javadoc of the code can be generated with `gradlew generateJavadoc` (see [Getting started](03-getting-started.md)).
- The documents describe the branch `feature/integration-followup` on top of commit `0fba295`, checked against the code and the tests on 2026-11-11 (341 Android and 71 backend unit tests passing).

## Folder layout

```
docs/
  README.md                    this index
  01-project-overview.md
  02-features-status.md
  03-getting-started.md
  04-roadmap.md
  engineering/
    01-specification.md
    02-design.md
    03-architecture.md
    04-ai-engine.md
    05-testing.md
  diagrams/
    img/
    src/
    README.md
```
