# Automated tests

This document explains how to run the available test suites
and locate their execution evidence.

## Prerequisites

- JDK 17 or 21.
- Git.
- Python 3 for the Engine API tests.
- Internet access for the first Maven dependency download.

Run commands from the repository root unless instructed otherwise.

The Student Applications tests are currently on
`qa/student-application-tests`. Until the testing PR is merged,
a fresh clone must switch to that branch.

## Setup from a fresh clone

```bash
git clone https://github.com/aishaSalmii/UniPlace.git
cd UniPlace
git switch qa/student-application-tests
```

On macOS/Linux, ensure the Maven wrapper is executable:

```bash
chmod +x portal/mvnw
```

Do not repeat the clone command if you already have a working copy.

## 1. Student Applications Portal tests

**Location:**
`portal/src/test/java/com/halcyon/uniplace/acceptance/StudentApplicationsAcceptanceTest.java`

**Level:** White-box service integration testing.

The suite contains 16 tests using real Spring services and
repositories with an isolated in-memory H2 database.
The Engine is mocked to isolate Portal behavior.

The Portal and Engine do not need to be running separately.

From the repository root, run:

```bash
(cd portal && ./mvnw -Dtest=StudentApplicationsAcceptanceTest test -Dmaven.test.failure.ignore=true)
```

Windows PowerShell:

```powershell
Push-Location portal
.\mvnw.cmd "-Dtest=StudentApplicationsAcceptanceTest" test "-Dmaven.test.failure.ignore=true"
Pop-Location
```

### Reports

Maven creates reports in:

- `portal/target/surefire-reports/`
- `portal/target/site/jacoco/`, when report generation completes.

Preserved evidence from the recorded execution:

- `evidence/student-applications-junit-results.txt`
- `evidence/student-applications-junit-results.xml`

The recorded run executed 16 tests:
10 passed, 4 assertion failures, 2 errors and 0 skipped.

The two errors were unexpected business-rule exceptions
during operations expected to succeed.

## 2. Student Applications Engine API tests

**Location:** `tests/acceptance/test_application_engine.py`

**Level:** Black-box HTTP API testing.

The suite contains 11 checks covering eligibility, transcript
validity, input validation, matching and ranking.

It uses Python's standard library; no pip packages are required.

Stop any existing Engine before using `--start`, because
the script starts its own Engine on port 8081.

From the repository root, run:

```bash
python3 tests/acceptance/test_application_engine.py --start
```

Windows:

```powershell
py tests/acceptance/test_application_engine.py --start
```

The script starts the supplied `engine/uniplace-engine.jar`,
runs the checks and stops the process it started.
It does not require the Portal or a seeded Portal database.

Alternatively, to test an Engine that is already running,
omit `--start`.

### Reports

The script writes:

- `evidence/application-engine-results.txt`
- `evidence/application-engine-trace.json`

The trace contains the exact request bodies, HTTP statuses
and responses.

The recorded run executed 11 tests:
5 passed and 6 failed.

Fixed dates in these requests are intentional test inputs.
The Engine evaluates the supplied dates independently of
the current date.

## 3. Complete Portal test suite and coverage

This command runs the vendor's tests and all added Portal
tests available on the checked-out branch:

```bash
(cd portal && ./mvnw test -Dmaven.test.failure.ignore=true)
```

Windows PowerShell:

```powershell
Push-Location portal
.\mvnw.cmd test "-Dmaven.test.failure.ignore=true"
Pop-Location
```

Inspect the Surefire reports for actual results.
Open `portal/target/site/jacoco/index.html` for coverage,
when generated.

For MAINT-01, assess service-layer line coverage against
the required 70%. Record the measured value; do not infer
coverage from the number of tests.

The focused Student Applications run does not establish
the results or coverage of the complete suite.

## Interpreting results

- Passed: the test's assertions were satisfied.
- Failure: an assertion did not match the observed behavior.
- Error: an unexpected exception prevented normal completion.
- Skipped: the test was not executed.

A failing test requires investigation against its requirement.
Compilation, dependency-download and startup failures must
be distinguished from product defects.

The Maven failure-ignore option allows reporting to continue.
A BUILD SUCCESS message with this option does not mean
every test passed.

The Python suite returns a nonzero exit status when checks fail.
Do not change correct requirement assertions merely to obtain
a passing run.

## Traceability

Case IDs A01–A16 and E01–E11 map to requirements in:

- `docs/student-applications-test-cases.md`

Execution summaries and defect references are recorded in:

- `docs/student-applications-results.md`
- GitHub Issues.

A passing integration check verifies only its stated assertions.
Mocked Engine results do not verify the real Engine.
Pagination record counts do not establish response-time performance.

## Other suites and pending work

| Area | Location or status |
| --- | --- |
| Offers and allowance | Areej adds commands and results when her suite is available |
| Users and security | Lana adds commands and results when her suite is available |
| Manual/system checks | M01–M06 remain pending until executed |
| Performance testing | Add actual JMeter or equivalent commands and results |
| AI Assistant evaluation | `tests/ai-eval/`; add runnable scripts and recorded results when available |

Suggested folders such as `tests/api/`, `tests/e2e/` and
`tests/performance/` may be used for additional suites.
The implemented Student Applications scripts currently use
`tests/acceptance/`.

Each contributor must update this document when adding a suite.
Preserve existing instructions and distinguish planned tests
from executed results.

## Student Applications — M01 HTTP concurrency and retry

Run from the repository root with Python 3 and the Portal and Engine running:

```bash
python3 tests/acceptance/check_submission_requests.py
```

### Setup

Create the fictional student account through Coordinator → Students:

- Username: qa.concurrent
- Name: QA Concurrency Student
- Student number: 20999001
- Email: qa.concurrency@example.test
- Program: BSc Information Systems
- GPA: 3.50; credits: 90; prior internships: 0
- Skills: Java, SQL, Spring, Excel, Safety, Writing
- Upload a valid PDF CV and a recent PDF transcript.
- Set a test password and enter it at the script's hidden prompt.

The account must have no applications. Posting 8 must be OPEN and titled
Sustainability Reporting Intern. Check its deadline and eligibility
requirements before running.

### Behavior and evidence

The script sends two simultaneous authenticated submissions and then
one sequential retry. It creates application records; run it once per
empty fixture. Do not reset a shared working database to repeat it.

Evidence is saved under evidence/m01-http-<timestamp>/.

Execution on 19 November 2026:
- Concurrent submissions: FAIL — applications 1118 and 1119 persisted.
- Sequential retry: PASS — no additional application.
- Existing defect: #31, requirements APP-03 and APP-05.
- Evidence: evidence/m01-http-20261119T150917425148Z/

Missing-CSRF HTTP 405 is inconclusive. Browser double-clicks and actual
network timeouts are not tested. Inspect summary.json: this script does
not use a nonzero exit code to signal an observed test failure.