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