# Student Applications Test Results

## Execution details

- Tester: Aisha
- Original automated suites: 14 November 2026
- Manual and supplemental automated checks: 16 November 2026
- Branch: qa/student-application-tests
- OS: macOS 26.6.2 (build 25G83)
- Java: Oracle Java 21.0.12.1 LTS

## Automated test summary

| Suite | Execution date | Executed | Passed | Failures | Errors | Skipped |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Portal acceptance JUnit | 14 Nov 2026 | 16 | 10 | 4 | 2 | 0 |
| Engine API | 14 Nov 2026 | 11 | 5 | 6 | 0 | 0 |
| Supplemental concurrency integration | 16 Nov 2026 | 2 | 1 | 1 | 0 | 0 |
| Supplemental state-transition checks | 16 Nov 2026 | 58 | 55 | 3 | 0 | 0 |
| Combined inventory of separate executions | 14–16 Nov 2026 | 87 | 71 | 14 | 2 | 0 |

These totals combine separately executed suites, not one full-suite run. Parameterized invocations are counted as checks; they are not distinct requirements or defects. Manual checks and vendor CI tests are not added to these totals. The 14 failures and 2 errors are unsuccessful checks, not 16 distinct bugs. No fixes or successful retests are claimed.

The two JUnit errors were business-rule exceptions during
operations expected to succeed. They require defect analysis;
they were not compilation or startup failures.

## Reported defects

| Issue | Test | Finding |
| --- | --- | --- |
| #4 | E02 | GPA equal to the minimum incorrectly rejected |
| #5 | A05 | Withdrawn applications counted as active |
| #6 | A06; M02 | Probation limit returned as 5 instead of 2; Omar dashboard showed 1 of 5 with configured limit 2 |
| #7 | A07 | Reapplication after rejection refused |
| #8 | A10 | Score equal to shortlist threshold requires an override |
| #9 | A11; M05 | Accepted withdrawal not refused; supplemental checks confirmed ACCEPTED/REJECTED persisted as WITHDRAWN and repeated WITHDRAWN call was not refused |
| #10 | A12 | Suspended-company shortlisting not refused |
| #11 | E03 | Transcript rejected on its 180th valid day |
| #13 | E06 | GPA above 4.00 returns HTTP 200 instead of 400 |
| #14 | E07 | Skill normalization does not follow the contract |
| #15 | E08 | Empty nice-to-have list incorrectly adds points |
| #16 | E10 | Seat cutoff includes more candidates than seats |
| #20 | M06 | Refused invalid-letter submission clears typed input after client-side restriction bypass |
| #21 | M06 | Invalid-letter refusal gives a generic message without the rule/value |
| #29 | M04 | Next drops State and Company application filters |
| #30 | M02 | Saved GOOD limit of 6 persists in Settings but student dashboard retains 5; submission-refusal cause unconfirmed |
| #31 | M01b | Synchronized overlapping service submissions persist 2 applications instead of 1 |

The inventory contains 17 reported issue IDs associated with Aisha's checks, subject to team review and deduplication. Issue numbers are not a count of all repository issues. The three M05 failure invocations strengthen issue #9 rather than becoming three new defect reports. This document records observed findings; it does not assert current GitHub issue state or that defects have been fixed.

## Evidence

- evidence/application-engine-results.txt
- evidence/application-engine-trace.json
- evidence/student-applications-junit-results.txt
- evidence/student-applications-junit-results.xml
- evidence/student-application-concurrency-results.txt
- evidence/student-application-concurrency-results.xml
- evidence/student-application-state-results.txt
- evidence/student-application-state-results.xml

## Remaining scope and review

- M01: browser/session/CSRF and real-network timeout/retry behavior remain untested; two supplemental integration checks executed.
- M02: clean GOOD and PROBATION submission-boundary enforcement and broader Settings checks remain incomplete; Omar's attempt was blocked by eligibility.
- M03: local 100-request timing workload passed (p95 16.572834 ms, 0 errors); reference-hardware, filters, ownership and browser/concurrency scope remain unverified. See M03 evidence below.
- M04: State and Company pagination retention failed; posting id 8 basic filtering passed, but its pagination retention remains untested.
- M05: browser/API, full authorization and offer-service paths remain untested; 58 supplemental checks executed.
- M06: invalid-length refusal message and input retention failed after temporary client-side bypass; these results do not describe every refusal path.
- Draft PR #17 exists. Teammate review, shared-case consolidation and final report/presentation preparation remain outstanding.
- AI assistance is recorded in docs/ai-usage-log.md; team review of these records remains necessary.
- A historical full Portal CI run and its coverage are recorded below. No fresh full-suite execution including the later M01/M05 classes is claimed here.

## Historical full Portal CI run and coverage

Source: [GitHub Actions run 43735551175](https://github.com/aishaSalmii/UniPlace/actions/runs/43735551175).

That run executed 163 Portal tests: 157 passed, 4 failures, 2 errors, 0 skipped. The unsuccessful cases were A05, A06, A07, A10, A11 and A12. It preceded the supplemental concurrency and state-transition classes and is not the current full-suite total.

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

Company results and the Posting id check are recorded below; only posting-filter pagination retention remains untested.

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

### M05 — Supplemental state-transition integration checks

Executed 16 November 2026 using StudentApplicationStateTransitionsTest.
58 checks: 55 passed, 3 failed, 0 errors, 0 skipped.

Passed:
- 49 state-machine contract checks.
- 4 permitted-withdrawal checks.
- 2 interview-cancellation checks.

Failed:
- ACCEPTED and REJECTED applications could be withdrawn; persisted
  states changed to WITHDRAWN.
- Repeated withdrawal from WITHDRAWN did not raise the required refusal.

Additional evidence recorded in issue #9.

Scope: isolated H2 fixtures with mocked Engine. M05 remains partially
executed; browser/API, full authorization and offer-service paths
remain untested.

Evidence:
- evidence/student-application-state-results.txt
- evidence/student-application-state-results.xml

## M03 — Local application-list performance (partial requirement evidence)

Executed 16 November 2026 at approximately 23:27 Qatar time (20:27 UTC).
Before generation: 117 applications. Built-in coordinator Tools added 1,000
applications, 100 students, 20 postings and 1,000 notifications. After generation:
1,117 applications across 56 pages. Existing records retained; no reset performed.

Environment: Apple M1, 8 physical/8 logical cores, 8 GiB RAM; macOS 26.6.2;
Oracle Java 21.0.12.1. Recorded repository HEAD: cd48b4050ffd636d0eccc4dcc0a9ee380ae57206.
The tracked working tree was clean at measurement; the newly added runner was
not yet committed. This identifies source context, not proof of the running JAR's build hash.

Method: one authenticated coordinator; 8 warm-up requests excluded, then 100
sequential GETs, 25 each to zero-based pages 0, 1, 27 and 55. Timing covers request
start through full HTML response-body read over loopback. Login, HTML parsing,
static assets and browser rendering excluded. Responses checked for HTTP 200,
expected total and page number, 20 rows (17 on last page), seven cells per row,
and no repeated application ID within a page. This does not prove every record's
content or completeness across all 56 pages. Password/session cookies not saved.

| Metric | Value |
| --- | ---: |
| Measured requests | 100 |
| Invalid/error responses | 0 |
| Mean HTTP response time | 11.943280 ms |
| Median | 10.650813 ms |
| p95 (nearest rank, 95th sorted observation) | 16.572834 ms |
| Maximum | 44.216042 ms |
| Comparison threshold | 2,000 ms |

| Page (zero-based) | Samples | p95 (ms) |
| --- | ---: | ---: |
| 0 | 25 | 16.572834 |
| 1 | 25 | 16.498708 |
| 27 | 25 | 14.274250 |
| 55 | 25 | 16.852542 |

Verdict: PASS for this local, unfiltered sequential HTTP workload. M03/PERF-01
remain PARTIALLY EXECUTED: the host was not constrained to the 4-core/8-GB reference;
filtered lists, student/recruiter ownership, full browser rendering and concurrent
users were not assessed. No full PERF-01 or PERF-03 compliance claim is made.
These 100 timing samples are separate from the 87 functional automated-check inventory.
No new defect is supported by these timings.

Evidence directory: evidence/m03-20261116T202716440861Z/
- requests.csv: raw observations; independently recalculated and matched summary.
- summary.json: aggregate and per-page values.
- environment-and-plan.json: hardware, source HEAD and measurement settings.
Runner: tests/performance/measure_application_list.py.
The 117 baseline includes prior manual test records; it is not an untouched seed-only database.
