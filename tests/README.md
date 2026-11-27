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

## Supplemental Portal concurrency and state-transition tests

Run from the repository root. These tests use isolated H2 fixtures
and a mocked Engine; separate Portal and Engine processes are not required.

```bash
(cd portal && ./mvnw "-Dtest=StudentApplicationConcurrencyTest,StudentApplicationStateTransitionsTest" test -Dmaven.test.failure.ignore=true)
```

Recorded results:
- Concurrency: 2 checks, 1 passed, 1 failure (#31).
- State transitions: 58 checks, 55 passed, 3 failures (#9).

Reports: portal/target/surefire-reports/

Preserved evidence:
- evidence/student-application-concurrency-results.txt
- evidence/student-application-concurrency-results.xml
- evidence/student-application-state-results.txt
- evidence/student-application-state-results.xml

Do not use the historical concurrency run's coverage figures:
JaCoCo reported class/execution-data mismatches.
## Student Applications — M03 local performance check

Location: tests/performance/measure_application_list.py

Prerequisites:
- Python 3; no third-party packages.
- Portal running at http://127.0.0.1:8080.
- Coordinator login.
- Seed data plus at least 1,000 additional applications.
- Confirm the exact application total before running.
- Avoid database changes during measurement.

Run from the repository root, replacing 1127 with the current total:

```bash
python3 tests/performance/measure_application_list.py --expected-total 1127
```

Enter the Coordinator password at the hidden prompt.

The script does not generate or reset data. It excludes 8 warm-up
requests, then measures 100 sequential authenticated GETs across
four application-list pages.

Evidence is written to evidence/m03-<timestamp>/:
- environment-and-plan.json
- requests.csv
- summary.json

The local workload passes when all measured responses are valid
and p95 is at most 2,000 ms. Inspect summary.json; the script does
not explicitly return a nonzero exit status for a measured FAIL.

Historical execution on 16 November 2026:
1,117 applications; 100 requests; zero invalid/error responses;
p95 16.572834 ms.

Scope: local sequential HTTP timing only. Login, static assets,
HTML parsing and browser rendering are excluded from timing.
Reference-hardware compliance, filters, ownership and concurrent
users are not established.

## Other suites and pending work

| Area | Location or status |
| --- | --- |
| Offers and allowance | Areej adds commands and results when her suite is available |
| Users and security | Lana adds commands and results when her suite is available |
| Manual/system checks | M01–M06 have recorded executions with remaining scope limitations; see docs/student-applications-results.md. Posting-filter pagination failed on 25 November (#29). |
| Performance testing | M03 Python HTTP runner documented above; historical local results recorded. Broader performance scope remains unverified. |
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

## Offers, allowance, notifications and AI Assistant tests — Areeg

Requirements: OFF-01 to OFF-07, ADM-01 (offer and allowance settings), NOT-01 to NOT-04, AI-01 to AI-11.
Case IDs O-01 to O-25 map to requirements in `docs/areej-test-cases.md`; the execution summary is in
`docs/areej-results.md`; evidence is in `tests/evidence/offers/`. Task issue: #12.

On Windows, set JAVA_HOME for the session if it does not point to JDK 17 or 21, for example:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
```

### Portal integration tests (JUnit, no running Portal or Engine needed)

All four classes are in `portal/src/test/java/com/halcyon/uniplace/acceptance/` and use real Spring services
with an in-memory H2 database. The Engine (and for the AI class, the AI provider client) is mocked, and the
deadline and agreement classes use a fixed clock.

| Class | Tests | Requirements | Recorded result |
| --- | --- | --- | --- |
| `OffersAcceptanceTest` | O01–O08 | OFF-01, OFF-02, OFF-03, USR-05 | 15 Nov 2026: 3 pass, 5 fail (#18, #22–#25) |
| `OffersDeadlineBoundaryTest` | D01–D07 | OFF-02, ADM-01 | 26 Nov 2026: 5 pass, 2 fail (#18, #23) |
| `OffersAgreementTest` | G01–G10 | OFF-07, ADM-01 | 27 Nov 2026: 9 pass, 1 fail (#47) |
| `AiAssistantAcceptanceTest` | I01–I14 | AI-03, AI-06, AI-07, AI-08, AI-10, AI-11 | 27 Nov 2026: 3 pass, 11 fail (#43, #51–#56) |

Windows PowerShell, from the repository root:

```powershell
Push-Location portal
.\mvnw.cmd "-Dtest=OffersAcceptanceTest,OffersDeadlineBoundaryTest,OffersAgreementTest,AiAssistantAcceptanceTest" test "-Dmaven.test.failure.ignore=true"
Pop-Location
```

macOS/Linux:

```bash
(cd portal && ./mvnw -Dtest=OffersAcceptanceTest,OffersDeadlineBoundaryTest,OffersAgreementTest,AiAssistantAcceptanceTest test -Dmaven.test.failure.ignore=true)
```

To run one class, put only its name after `-Dtest=`. Results are in `portal/target/surefire-reports/`.
Preserved evidence: `junit-OffersAcceptanceTest-2026-11-15.txt` (and `.xml`),
`junit-OffersDeadlineBoundaryTest-2026-11-26.txt`, `junit-OffersAgreementTest-2026-11-27.txt`,
`junit-AiAssistantAcceptanceTest-2026-11-27.txt`.

### Engine API scripts (black-box, Engine must be running)

Start the system first with `start.cmd --jar` (Windows) or `./start.sh` (macOS/Linux). The scripts use only
the Python standard library.

| Script | Checks | Requirement | Recorded result |
| --- | --- | --- | --- |
| `tests/acceptance/test_engine.py` | E12–E17 (allowance part of E01–E18) | OFF-05 | 15 Nov 2026: 4 pass, 2 fail (#26, #27) |
| `tests/acceptance/test_allowance_boundaries.py` | A01–A28, independent calculation | OFF-05 (OpenAPI month and rounding rules) | 26 Nov 2026: 20 pass, 8 fail (#26, #27, #39, #40) |

```powershell
py tests\acceptance\test_engine.py
py tests\acceptance\test_allowance_boundaries.py
```

(macOS/Linux: `python3` instead of `py`.) `test_engine.py` writes `evidence/engine-results.txt` and
`evidence/engine-trace.json` and overwrites them on every run; dated copies are kept in
`tests/evidence/offers/`. `test_allowance_boundaries.py` writes
`tests/evidence/offers/allowance-boundaries-<timestamp>.json`.

### Last-seat race (OFF-04, Portal must be running with fresh demo data)

```powershell
.\start.cmd --jar --reset
# in a second window, after "Demonstration data loaded":
py tests\acceptance\test_last_seat_race.py
```

Two students (application 7 and application 8, posting 8 with one seat) accept at the same moment.
Reset before every run, because the first run uses up the seat. Each run writes
`tests/evidence/offers/last-seat-race-<timestamp>.json`. Recorded: 3 runs on 26 Nov 2026, all fail (#38).

### AI Assistant with a fake provider (AI-03, AI-05, AI-06, AI-07)

`tests/acceptance/fake_ai_provider.py` is a local stand-in for an OpenAI-compatible provider on
`http://127.0.0.1:9999/v1`. It records every request and answers in one mode:
`capture` (valid answer), `slow` (answers after 30 s) or `invalid` (score 150, 6 strengths).

1. Start the Portal (`.\start.cmd --jar --reset`), then in a second window: `py tests\acceptance\fake_ai_provider.py capture`
2. As coordinator, Settings: AI provider "Other OpenAI-compatible endpoint", base URL `http://127.0.0.1:9999/v1`,
   API key `test-key`, AI model name `fake-model-1`, Save.
3. As a student, open an open posting, paste a letter from `ai-eval/letters.json` and click
   "Get feedback on my letter". Use a different posting for each mode (results are cached for 10 minutes).
4. Stop with Ctrl+C and repeat with `slow` and `invalid`.
5. Restore Settings: AI provider OpenAI, empty base URL and model, tick Remove for the API key.

Requests are appended to `tests/evidence/offers/ai-fake-provider-<mode>.jsonl`. Recorded on 27 Nov 2026:
all 3 modes fail (#51, #55, #56, #57). These are fake-provider results, not real-model results.

### Manual checks and not run

Manual checks on the running Portal (offer boundaries, settings, expiry, statement access, agreement date,
Outbox notifications and reminders) are described step by step in `docs/areej-test-cases.md`
(O-02 to O-05, O-07, O-13, O-14, O-18, O-19). Not run: O-15 (timing) and O-25 (real-model AI evaluation,
AI-04 and AI-09), because no real API key was available.



