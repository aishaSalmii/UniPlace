# Student Applications Test Results

## Execution details

- Tester: Aisha
- Date: 14 November 2026
- Branch: qa/student-application-tests
- OS: macOS 26.6.2 (build 25G83)
- Java: Oracle Java 21.0.12.1 LTS

## Automated test summary

| Suite | Executed | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Engine API | 11 | 5 | 6 | 0 | 0 |
| Portal JUnit | 16 | 10 | 4 | 2 | 0 |
| Total | 27 | 15 | 10 | 2 | 0 |

The two JUnit errors were business-rule exceptions during
operations expected to succeed. They require defect analysis;
they were not compilation or startup failures.

## Reported defects

| Issue | Test | Finding |
| --- | --- | --- |
| #4 | E02 | GPA equal to the minimum incorrectly rejected |
| #5 | A05 | Withdrawn applications counted as active |
| #6 | A06 | Probation application limit returned as 5 instead of 2 |
| #7 | A07 | Reapplication after rejection refused |
| #8 | A10 | Score equal to shortlist threshold requires an override |
| #9 | A11 | Withdrawal of an accepted application not refused |
| #10 | A12 | Suspended-company shortlisting not refused |
| #11 | E03 | Transcript rejected on its 180th valid day |
| #13 | E06 | GPA above 4.00 returns HTTP 200 instead of 400 |
| #14 | E07 | Skill normalization does not follow the contract |
| #15 | E08 | Empty nice-to-have list incorrectly adds points |
| #16 | E10 | Seat cutoff includes more candidates than seats |

All 12 unsuccessful checks from the current automated runs have
been reported. Reports remain subject to review and deduplication.
Manual and non-functional checks are still pending.

## Remaining findings to document

- E03: Transcript rejected on its 180th valid day.
- E06: GPA above 4.00 accepted without a validation error.
- E07: Skill case and surrounding spaces not normalized.
- E08: Empty nice-to-have list incorrectly increases score.
- E10: More candidates marked within seats than available seats.
- A07: Reapplication after rejection refused.
- A10: Score equal to shortlist threshold requires override.
- A11: Accepted application can be withdrawn.
- A12: Suspended company can shortlist an application.

Failed checks are findings to review and report individually;
the test count is not automatically the distinct defect count.

## Evidence

- evidence/application-engine-results.txt
- evidence/application-engine-trace.json
- evidence/student-applications-junit-results.txt
- evidence/student-applications-junit-results.xml

## Pending work

- Complete remaining defect reports and check for duplicates.
- Update test-case execution statuses and issue links.
- Update the AI usage log for retained testing assistance.
- Complete pending manual and non-functional tests.
- Run the full test suite and assess service-layer coverage.
- Open the testing pull request and obtain teammate review.

## CI coverage

Source: GitHub Actions run 43735551175.

| Scope | Missed lines | Total lines | Covered lines | Line coverage |
| --- | ---: | ---: | ---: | ---: |
| Service package | 283 | 1037 | 754 | 72.71% |
| ApplicationService | 38 | 124 | 86 | 69.35% |

The service package exceeds the MAINT-01 line-coverage threshold
of 70%. This assesses only the coverage condition; the full
suite still contains the six reported unsuccessful tests.

These figures come from the combined CI suite, including vendor
tests. They are not coverage attributable solely to Aisha's tests.

Evidence: evidence/ci-run-43735551175-jacoco.xml

### M06 — Invalid motivation letter: error message and input retention

Executed manually on 16 November 2026 using student.amal and
Sustainability Reporting Intern at Doha Energy Partners.

The form disabled submission for a 99-character letter. The disabled
attribute was temporarily removed using browser Developer Tools to
test server-side handling; no project files were modified.

The server refused submission but displayed a generic error and cleared
the letter, resetting the counter to 0.

- USA-01: FAIL — rejection reason is not specific. Issue #21.
- USA-06: FAIL — typed input is lost after refusal. Issue #20.

Evidence: screenshots attached to issues #20 and #21.

### M04 — State filter retention during pagination

Executed manually on 16 November 2026 as coordinator.

Before clicking Next: State was Submitted, with 33 applications
across 2 pages.

After clicking Next: State reset to Any state, mixed states appeared,
and the total changed to 116 applications across 6 pages.

Before:
http://127.0.0.1:8080/coordinator/applications?state=SUBMITTED&companyId=&postingId=

After:
http://127.0.0.1:8080/coordinator/applications?page=1

Result: FAIL for State filter retention. Issue #29.
Evidence: before-and-after screenshots attached to issue #29.

Company and Posting id filter checks remain pending.

Additional M04 checks:
- Company filtering: Doha Energy Partners returned 22 applications.
  Clicking Next cleared the company filter and displayed 116 applications
  from multiple companies. FAIL; additional evidence attached to #29.
- Posting id 8: returned 3 Sustainability Reporting Intern applications.
  Basic filtering PASS. Pagination retention not tested because the
  results fit on one page.

M04 overall: FAIL due to State and Company filter loss during pagination.

### M02 — Application limits and Settings changes (partial execution)

Executed manually on 16 November 2026 using student.amal and coordinator.

- Amal initially had 3 active applications and 1 Withdrawn.
  Dashboard displayed 4 of 5, reproducing the count discrepancy in #5.
- Sustainability Reporting Intern submission succeeded, leaving
  4 active applications and 1 Withdrawn.
- Field Operations Intern submission was refused with a generic message.
- Coordinator changed the GOOD limit from 5 to 6 and saved.
- Without an application restart, Amal's dashboard still displayed 5 of 5.
  Reopened Settings retained 6. Dashboard limit update: FAIL, issue #30.
- Another Field Operations submission was refused. Its exact refusal
  cause was not established.
- GOOD limit restored to 5; PROBATION remained 2.

Evidence: screenshots for the Settings mismatch attached to issue #30.

M02 remains partially executed. Clean GOOD boundary checks and the
PROBATION boundary/Settings-change checks remain incomplete. 

#### M02 — Additional PROBATION checks

Executed manually on 16 November 2026 as student.omar.

- Profile: PROBATION, GPA 1.85, 60 completed credits.
- Application list: one Submitted and one Rejected.
- Dashboard displayed 1 of 5 despite the configured PROBATION limit of 2.
  This reproduces the displayed-limit problem in issue #6.
- Field Operations Intern submission returned “You are not eligible
  for this posting.” The specific eligibility reason was not displayed.
- The 182-character motivation letter remained after this refusal:
  input retention passed for this particular eligibility-refusal path.
- The submission-boundary check was blocked by eligibility. Permission
  to create a second or third active application was not established.

M02 remains partially executed. Evidence for the dashboard discrepancy
is recorded in issue #6.

### M01 — Supplemental concurrency and retry integration checks

Executed 16 November 2026 using StudentApplicationConcurrencyTest.

- M01a PASS: retry after a committed submission retained one application.
- M01b FAIL: two overlapping submissions both succeeded and persisted
  2 applications instead of 1. APP-05 violation, issue #31.
- Total: 2 tests, 1 passed, 1 failure, 0 errors, 0 skipped.

Scope: isolated H2 database with mocked Engine. Browser/session/CSRF
and real-network timeout behavior remain untested.
This run's coverage figures are not used because JaCoCo reported
class/execution-data mismatches.

Evidence:
- evidence/student-application-concurrency-results.txt
- evidence/student-application-concurrency-results.xml
- Terminal screenshots attached to issue #31.