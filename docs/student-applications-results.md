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
| #31 | M01b; M01 HTTP | Synchronized service submissions and concurrent HTTP submissions persist 2 applications instead of 1 |
| #36 | Eligibility investigation, 22 November | Portal mapper uses Settings default minimum GPA instead of posting minimum; eligible GPA 1.80 student refused for a GPA 1.50 posting |

The inventory contains 18 reported issue IDs associated with Aisha's checks, subject to team review and deduplication. Issue numbers are not a count of all repository issues. The three M05 failure invocations strengthen issue #9 rather than becoming three new defect reports. This document records observed findings; it does not assert current GitHub issue state or that defects have been fixed.

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

- M01: integration and authenticated concurrent HTTP checks reproduced #31; sequential retry passed. Browser double-clicks and actual network timeouts remain untested; missing-CSRF HTTP 405 is inconclusive.
- M02: GOOD 4→5 passed on 19 November. PROBATION 1→2 passed and 2→3 failed (#6) on 22 November after the temporary workaround for #36. Default GPA restored to 2.00. GOOD sixth-attempt refusal cause and submission enforcement after increasing GOOD to 6 remain unresolved.
- M03: local 100-request timing workload passed (p95 16.572834 ms, 0 errors); reference-hardware, filters, ownership and browser/concurrency scope remain unverified. See M03 evidence below.
- M04: State and Company pagination retention failed; posting id 8 basic filtering passed, but its pagination retention remains untested.
- M05: browser/API, full authorization and offer-service paths remain untested; 58 supplemental checks executed.
- M06: invalid-length refusal message and input retention failed after temporary client-side bypass; these results do not describe every refusal path.
- PR #17 is ready for review; two reviews were pending at the last observed check. Teammate review, shared-case consolidation and final report/presentation preparation remain outstanding.
- AI assistance is recorded in docs/ai-usage-log.md; team review of these records remains necessary.
- Latest verified full Portal CI at commit 278e123: 223 tests, 213 passed, 8 failures, 2 errors, 0 skipped. See the 22 November CI verification section. This run includes the supplemental M01/M05 classes. Historical totals are retained separately and overlap this run.

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

## M01 additional HTTP execution — 19 November 2026

Command: `python3 tests/acceptance/check_submission_requests.py`

Using the fictional qa.concurrent account with no existing applications,
two simultaneous authenticated POST requests to /postings/8/apply
created two SUBMITTED applications: 1118 and 1119.

- Concurrent submission: FAIL — expected one application; two persisted.
- Sequential retry: PASS — no additional application was created.
- Both records matched the expected student and posting.
- Existing defect: #31, violating APP-03 and APP-05.
- Evidence: evidence/m01-http-20261119T150917425148Z/
- The earlier run on 16 November stopped at the missing-CSRF precheck
  with HTTP 405, before concurrent submission.
- HTTP 405 does not establish successful CSRF protection.
- Browser double-clicks and actual network timeouts remain untested.

These are supplemental HTTP checks, separate from the previously
reported 87 automated checks.

## M02 additional manual execution — 19 November 2026

Fixture: qa.limits, student 20999002, GOOD standing, GPA 3.50,
90 credits, no prior internships, valid CV and recent transcript.
Initially no applications. Settings: GOOD=5, PROBATION=2.

- Created five SUBMITTED applications to distinct postings.
- The 4→5 boundary passed: Generated Internship 16 was accepted.
- Attempting Generated Internship 26 as a sixth application was
  refused; five records remained. The generic message did not
  establish the specific refusal cause, and the letter was cleared.
- Changed GOOD from 5 to 6 without restarting. Reopened Settings
  confirmed 6 persisted, but the student dashboard showed 5 of 5.
- Retrying the sixth submission was refused again; cause unconfirmed.
- Existing issue #30: saved limit not reflected on the dashboard.
- Additional observations support #20 (input loss) and #21
  (generic refusal).
- Restored GOOD=5 and PROBATION=2 after testing.

Historical status on 19 November: PROBATION boundaries were pending.
They were subsequently completed on 22 November, as recorded below.
GOOD configured-limit submission enforcement remains unresolved.
Screenshots are attached to issue #30.

## PROBATION fixture and eligibility investigation — 22 November 2026

Created qa.probation (20999003): GPA 1.80, 90 credits,
PROBATION, zero prior internships, valid CV and recent transcript.

The dashboard displayed 0 of 5, supporting existing #6.

The first Field Operations submission was refused as ineligible,
although the posting minimum was GPA 1.50 / 40 credits.
The 182-character motivation letter was retained.

Controlled Engine probes with otherwise identical inputs:
- minGpa=1.50: eligible=true, reasons=[]
- minGpa=2.00: eligible=false, GPA below minimum.

Source inspection showed toEligibilityRequest reads DEFAULT_MIN_GPA
from Settings instead of the posting minimum. Reported as #36
(APP-04, POS-01; integration).

The probes used representative document metadata, not a capture
of the Portal's outgoing request.

The initial PROBATION application-limit attempt was blocked by eligibility.
Boundary testing subsequently completed using the temporary workaround;
see the dated boundary results below. Default GPA was restored to 2.00.
No production-code fix was made.

### PROBATION application boundary test — 22 November 2026

Student: QA Probation Limits; GPA 1.80; 90 credits; PROBATION.
Configured limits: Good standing 5; Probation 2.

| Check | Expected | Actual | Result |
|---|---|---|---|
| 1 → 2 active applications | Second application accepted | Supply Chain Intern submitted at 18:03 | PASS |
| 2 → 3 active applications | Third application blocked | Customer Experience Intern submitted at 18:15; three Submitted records confirmed | FAIL — #6 |

Default minimum GPA was temporarily lowered to 1.50 to bypass
the separate eligibility defect #36. It was restored to 2.00
after testing. Application limits remained 5 and 2.

Evidence: issue #6, reproduction comment and screenshots dated
22 November 2026 (18:12, 18:15, 18:16 and 18:17).

### CI and coverage verification — 22 November 2026

Commit: 278e123.

Portal suite: 223 tests — 213 passed, 8 failures,
2 errors, 0 skipped.

Service-package line coverage: 756/1037 = 72.90%.
ApplicationService line coverage: 86/124 = 69.35%.

The service-package coverage target of at least 70% is met.
The all-tests-pass condition is not met; the unsuccessful
acceptance checks correspond to documented defects.

Evidence: GitHub Actions run, portal-reports artifact,
and JaCoCo service-package screenshot.

Actions run: https://github.com/aishaSalmii/UniPlace/actions/runs/45038264676
