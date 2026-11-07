# Automated tests

Say here how to run each suite, from a fresh clone, one command per suite. The Portal's unit and integration
tests live in `portal/src/test` and run with `cd portal` and `./mvnw test` (Windows: `.\mvnw.cmd test`). Suggested
folders for the other suites:

| Folder | Suite |
| --- | --- |
| `tests/api/` | Engine API tests (black-box) |
| `tests/e2e/` | End-to-end tests (for example Playwright) |
| `tests/performance/` | Performance scripts and results (JMeter `.jmx` or k6) |
| `tests/ai-eval/` | AI Assistant evaluation scripts and raw outputs |

Every test names the requirement and the designed test case it implements.
