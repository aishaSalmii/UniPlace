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
| #4 | E02 | Engine rejects GPA equal to the minimum |
| #5 | A05 | Withdrawn applications count as active |
| #6 | A06 | Probation limit is 5 instead of 2 |

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