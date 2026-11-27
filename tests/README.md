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

## Users and Security automated tests — Lana

The security tests use JUnit 5, Spring Boot Test and MockMvc.

### Run the tests on Windows

Open PowerShell in the UniPlace project folder:

```powershell
cd portal
.\mvnw.cmd "-Dtest=SecurityTest,UserServiceTest" "-Djacoco.skip=true" test

```

If JAVA_HOME is not configured, set it to your installed JDK folder before running Maven.

### Security regression tests

| Test case | Automated method | Expected behaviour | Current result |
| --- | --- | --- | --- |
| USR-TC-01 | UserServiceTest.passwordChangeRejectsPasswordWithoutDigit | Reject passwords without digits | FAIL |
| USR-TC-08 | SecurityTest.recruiterCannotViewAnotherCompanyApplication | Block cross-company application access | FAIL |
| USR-TC-09 | SecurityTest.studentCannotViewAnotherStudentsApplication | Block access to another student's application | FAIL |

These tests currently fail because they reproduce known security defects.

JaCoCo is skipped because version 0.8.12 has compatibility issues with Java 25.