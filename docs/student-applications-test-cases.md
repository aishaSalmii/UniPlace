# **Student application test cases**

Owner: Aisha. WB = white-box; BB = black-box. Results below reflect Aisha's execution on 14 November 2026, as shown in the shared terminal output and linked reports. Manual cases M04 and M06 were executed on 16 November 2026 and failed the checks documented below. M02 was partially executed on 16 November 2026: the dashboard limit update failed (#30); Omar also displayed a PROBATION limit of 5 instead of 2 (#6). Clean GOOD and PROBATION boundary checks remain incomplete; Omar’s submission attempt was blocked by eligibility. Supplemental M01 integration checks were executed on 16 November 2026: retry after commit passed and overlapping submissions failed (#31). M01 browser/session/CSRF and real-network timeout checks remain untested. Supplemental M05 state checks ran on 16 November 2026: 55 passed and 3 failed (#9). M05 system/channel scope remains incomplete. M03 remains Not run. Posting-filter retention across pages remains untested. Passing integration tests verify their stated assertions with a mocked Engine; they do not establish real-provider or complete UI behavior.

## **Execution details**

- Original automated suites: 14 November 2026
- Supplemental M01/M05 automated checks: 16 November 2026

- Manual M02/M04/M06 execution date: 16 November 2026

- OS: macOS 26.6.2 (build 25G83)

- Java: Oracle Java 21.0.12.1 LTS

- Branch: qa/student-application-tests

- Uploaded JUnit evidence commit: 52a0172

| Suite | Execution date | Executed | Passed | Failures | Errors | Skipped |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Portal acceptance JUnit | 14 Nov 2026 | 16 | 10 | 4 | 2 | 0 |
| Engine API | 14 Nov 2026 | 11 | 5 | 6 | 0 | 0 |
| Supplemental concurrency integration | 16 Nov 2026 | 2 | 1 | 1 | 0 | 0 |
| Supplemental state-transition checks | 16 Nov 2026 | 58 | 55 | 3 | 0 | 0 |
| Combined inventory of separate executions | 14–16 Nov 2026 | 87 | 71 | 14 | 2 | 0 |

These totals combine separately executed suites, not one full-suite run. Parameterized invocations are counted as checks; they are not distinct requirements or defects. Manual checks and vendor CI tests are not added to these totals. The 14 failures and 2 errors are unsuccessful checks, not 16 distinct bugs. No fixes or successful retests are claimed.

Do not use the concurrency run's coverage figures: JaCoCo reported class/execution-data mismatches.

JUnit Error records an unexpected exception rather than an assertion failure. A07 and A10 raised business-rule exceptions for operations expected to succeed; they were not compiler or startup errors. A failed check is not automatically a separate defect.

### **Evidence**

- [Concurrency text report](../evidence/student-application-concurrency-results.txt)
- [Concurrency XML report](../evidence/student-application-concurrency-results.xml)
- [State-transition text report](../evidence/student-application-state-results.txt)
- [State-transition XML report](../evidence/student-application-state-results.xml)

- [JUnit text report](../evidence/student-applications-junit-results.txt)

- [JUnit XML report](../evidence/student-applications-junit-results.xml)

- [Engine text report](../evidence/application-engine-results.txt)

- [Engine request and response trace](../evidence/application-engine-trace.json)

- [Uploaded JUnit evidence at commit 52a0172](https://github.com/aishaSalmii/UniPlace/blob/52a0172/evidence/student-applications-junit-results.txt)

Reported findings are linked below, including manual findings #20, #21, #29 and #30, and supplemental integration finding #31. Keep existing screenshots and full raw evidence.

## **Test case matrix**

| Test case ID | Requirement | Module | Level | Technique and type | Preconditions | Steps | Test data | Expected result | Actual result | Status | Automated test | Issue |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A01 | APP-01 | Student applications | Integration | WB/BVA; Functional | Default approved company, eligible GOOD student, open posting; Engine mocked eligible. | Submit 99 ASCII x characters. | x ×99 | BusinessRuleException;0 saved applications. | 99-character letter refused; zero applications saved. | Passed | StudentApplicationsAcceptanceTest.A01_letter99IsRefused | — |
| A02 | APP-01; APP-04 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit100 characters. | x ×100; mocked score70 | SUBMITTED with match score70. | 100-character letter accepted as SUBMITTED with stored mocked match score 70. | Passed | StudentApplicationsAcceptanceTest.A02_letter100IsAccepted | — |
| A03 | APP-01 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit2000 characters. | x ×2000 | SUBMITTED. | 2,000-character letter accepted as SUBMITTED. | Passed | StudentApplicationsAcceptanceTest.A03_letter2000IsAccepted | — |
| A04 | APP-01 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit2001 characters. | x ×2001 | Refused;0 saved applications. | 2,001-character letter refused; zero applications saved. | Passed | StudentApplicationsAcceptanceTest.A04_letter2001IsRefused | — |
| A05 | APP-02 | Student applications | Integration | WB/predicate; Functional | One WITHDRAWN app; no other apps. | Count active apps. | WITHDRAWN | Count0. | Active count was 1 for one WITHDRAWN application; expected 0. | Failed | StudentApplicationsAcceptanceTest.A05_withdrawnDoesNotCountAsActive | [#5](https://github.com/aishaSalmii/UniPlace/issues/5) |
| A06 | APP-02 | Student applications | Integration | WB/decision branch; Functional | PROBATION student, default Settings. | Read active limit. | GPA1.80 | Limit2. | PROBATION limit was 5; expected 2. | Failed | StudentApplicationsAcceptanceTest.A06_probationLimitIsTwo | [#6](https://github.com/aishaSalmii/UniPlace/issues/6) |
| A07 | APP-03 | Student applications | Integration | WB/state partition; Functional | Only REJECTED app to same posting; still open; eligible, below active limit. | Submit new120-character letter. | REJECTED then retry | New SUBMITTED app;2 history records. | BusinessRuleException: You already have an active application for this posting. New submission did not succeed. | Error | StudentApplicationsAcceptanceTest.A07_reapplyAfterRejectedIsAllowed |[#7](https://github.com/aishaSalmii/UniPlace/issues/7)|
| A08 | APP-03 | Student applications | Integration | WB/state partition; Functional | Only WITHDRAWN app, open posting and eligible student. | Submit new120-character letter. | WITHDRAWN then retry | New SUBMITTED app. | Reapplication after WITHDRAWN accepted as SUBMITTED. | Passed | StudentApplicationsAcceptanceTest.A08_reapplyAfterWithdrawnIsAllowed | — |
| A09 | APP-03 | Student applications | Integration | WB/duplicate partition; Functional | Eligible isolated student and open posting. | Submit once then submit again. | Same student/posting | Second refused;1 record. Does not prove concurrent APP-05 idempotency. | Second active application refused; one application record remained. | Passed | StudentApplicationsAcceptanceTest.A09_secondActiveApplicationIsRefused | — |
| A10 | APP-07 | Student applications | Integration | WB/BVA; Functional | SUBMITTED app;approved company;own Recruiter. | Shortlist without justification. | Score60;threshold60 | SHORTLISTED without override requirement. | BusinessRuleException: The match score 60 is below the shortlist threshold 60. Give a justification to shortlist it anyway. | Error | StudentApplicationsAcceptanceTest.A10_scoreExactlyThresholdNeedsNoOverride | [#8](https://github.com/aishaSalmii/UniPlace/issues/8) |
| A11 | APP-06; APP-08 | Student applications | Integration | WB/state transition; Functional | ACCEPTED app owned by Student. | Call withdraw. | ACCEPTED | Refused;ACCEPTED retained. | Withdrawal call completed without the required BusinessRuleException. | Failed | StudentApplicationsAcceptanceTest.A11_acceptedCannotBeWithdrawn | [#9](https://github.com/aishaSalmii/UniPlace/issues/9) |
| A12 | COM-02; APP-06 | Student applications | Integration | WB/branch; Functional | SUBMITTED app;company updated to SUSPENDED. | Own Recruiter attempts shortlist. | Score80 | Refused;state unchanged. | Shortlisting call completed without the required BusinessRuleException while company was SUSPENDED. | Failed | StudentApplicationsAcceptanceTest.A12_suspendedCompanyPreventsShortlisting | [#10](https://github.com/aishaSalmii/UniPlace/issues/10) |
| A13 | REL-01; APP-04 | Student applications | Integration | WB/fault injection; Reliability | Eligible-looking student;Engine mock throws unavailable. | Submit valid letter. | EngineUnavailableException | Specific exception;no application persisted. | EngineUnavailableException raised; no application persisted. | Passed | StudentApplicationsAcceptanceTest.A13_engineOutageRefusesSubmission | — |
| A14 | USR-05; SEC-05; APP-08 | Student applications | Integration | WB/ownership decision; Security | App belongs to different Student. | Other student attempts withdraw. | Distinct student IDs | ForbiddenException;no unauthorized mutation. | Unrelated student withdrawal raised ForbiddenException. The test does not separately assert persisted state afterwards. | Passed | StudentApplicationsAcceptanceTest.A14_otherStudentCannotWithdraw | — |
| A15 | APP-10 | Student applications | Integration | WB/volume boundary; Functional | 21 historical rejected apps belonging to same student. | Request pages0 and1. | 21 records | 20 records then1; no loss. | Page 0 contained 20 records and page 1 contained 1. This does not establish response-time performance. | Passed | StudentApplicationsAcceptanceTest.A15_paginationReturns20ThenOne | — |
| A16 | APP-04 | Student applications | Integration | WB/negative partition; Functional | Engine mock refuses with2 reasons. | Submit otherwise valid letter. | GPA reason and missingCV reason | Both reasons retained in exception;0 app records. UI display needs separate test. | Both mocked eligibility reasons retained in exception; no application saved. UI display not tested. | Passed | StudentApplicationsAcceptanceTest.A16_everyEligibilityReasonIsRetained | — |
| M01 | APP-05 | Student applications | Integration (supplemental); planned system scope incomplete | WB/concurrency and retry; Reliability | Isolated H2 fixtures: eligible student, approved company, open posting and valid documents; Engine mocked. | M01a submit, discard successful response, then retry. M01b synchronize two service calls at mocked eligibility after duplicate checks and before insertion. | Same student, posting and 120-character letter | Exactly 1 persisted application; sensible repeat response. | M01a PASS: committed retry retained one SUBMITTED record and raised BusinessRuleException. M01b FAIL: both calls submitted; 2 records persisted instead of 1. | PARTIALLY EXECUTED — concurrency FAIL; browser/session/CSRF and real-network timeout behavior untested | StudentApplicationConcurrencyTest.M01a_retryAfterCommitKeepsOneApplication; M01b_overlappingSubmissionsKeepOneApplication; executed 16 November 2026 | #31; evidence/student-application-concurrency-results.txt and .xml; evidence commit 5b7c910 |
| M02 | APP-02; ADM-01 | Student applications | System | BB/BVA and decision table; Functional | student.amal in GOOD standing; coordinator Settings initially GOOD=5, PROBATION=2. Full design requires clean eligible fixtures for both standings. | Design: test GOOD 4→5→6 and PROBATION 1→2→3, then change Settings and repeat. Executed subset: inspect states, submit Sustainability Reporting, attempt Field Operations, change GOOD 5→6, inspect dashboard and reopen Settings, restore GOOD=5. | Initially 3 active plus 1 Withdrawn; after successful submission 4 active plus 1 Withdrawn; saved GOOD=6, PROBATION=2. | Fifth/second active application allowed; sixth/third refused under default limits; configured limits take effect immediately. | Dashboard initially counted 4 despite 3 active (#5). Sustainability Reporting succeeded. Field Operations refused before and after Settings change; exact refusal cause unconfirmed. GOOD=6 persisted in Settings, but dashboard showed 5 of 5 (#30). Omar: PROBATION, one Submitted and one Rejected; dashboard showed 1 of 5 (#6). Field Operations refused as ineligible; 182-character letter retained. No second/third application permission established. Restored GOOD=5; PROBATION remained 2. | PARTIALLY EXECUTED — dashboard limit update FAIL; clean GOOD and PROBATION boundary/Settings checks incomplete | Manual, 16 November 2026 | #5 count discrepancy; #6 probation display; #30 dashboard limit mismatch; screenshots attached to #30 |
| M03 | APP-10; PERF-01 | Student applications | System | BB/volume and timing; Performance | Seed plus1000 extra apps on recorded reference hardware; authenticated. | Sample at least100 application-list requests and calculate nearest-rank p95. | Page/filter combinations | p95<=2000ms;pages20;correct ownership. Not measured by A15. | Not recorded | Not run | Pending/manual | Pending |
| M04 | APP-10; USA-07 | Student applications | System | BB/filter partition; Usability | Coordinator account; State and Company filtered results span 2 pages. | Apply each filter separately; click Next where available. | State=Submitted; Company=Doha Energy Partners; Posting id=8 | Correct filtered results and criteria retained across pages. | State: 33 results; Company: 22 results. Next clears either filter and displays 116 unfiltered results. Posting id 8 returns 3 matching results; no second page. | FAIL; posting-filter pagination retention untested | Manual, 16 November 2026 | #29; screenshots attached to issue |
| M05 | APP-06; APP-08; INT-04 | Student applications | Unit/state-machine and service integration (supplemental); system scope incomplete | WB/state-pair matrix and service-path checks; Functional | Isolated H2 fixtures, owning student/recruiter, approved company; mocked Engine slot validation. | Check 49 state pairs; attempt withdrawal from each of 7 states; cancel an interview and retry cancellation; attempt cancellation after OFFERED. | All 7 application states; valid future slot | APP-06 state pairs enforced; allowed withdrawals succeed; forbidden withdrawals refused without mutation; cancellation returns INTERVIEW to SHORTLISTED; repeated/post-offer cancellation refused. | 58 checks: 55 passed, 3 failed. All 49 state-machine pairs, 4 permitted withdrawals and 2 cancellation checks passed. ACCEPTED and REJECTED changed to WITHDRAWN; repeated WITHDRAWN withdrawal lacked refusal. | PARTIALLY EXECUTED — withdrawal enforcement FAIL; browser/API, complete authorization and offer-service paths untested | StudentApplicationStateTransitionsTest; executed 16 November 2026 | #9; evidence/student-application-state-results.txt and .xml; commit 9cf30b9 |
| M06 | USA-01; USA-06; APP-01 | Student applications | System | BB/negative validation; Usability | student.amal; open Sustainability Reporting Intern posting, id 8; valid session. | Enter 99 characters; temporarily remove disabled from Submit using DevTools; submit and inspect message and input. | 99 characters | Specific rule/value explained; typed content retained. | Submission refused with generic error; letter cleared and counter reset to 0. Client-side restriction temporarily bypassed; no project files modified. | FAIL for USA-01 and USA-06 | Manual, 16 November 2026 | #21 generic message; #20 input loss; screenshots attached to issues |

## **Supplemental real Engine checks**

These are black-box HTTP API checks in tests/acceptance/test_application_engine.py. Each request is stateless and uses explicit fixture dates. Full payloads and responses are saved in evidence/application-engine-trace.json. Results below are from Aisha's execution, not merely the earlier preparation run.

| Test | Requirement or contract | Expected result | Actual result | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| E01 | APP-04; /eligibility | Valid baseline eligible with no reasons | Baseline eligible; reasons empty | Passed | — |
| E02 | APP-04; /eligibility GPA at least minGpa | GPA 2 with minGpa 2 eligible | Ineligible; reason says GPA 2 is below minimum 2 | Failed | [#4](https://github.com/aishaSalmii/UniPlace/issues/4) |
| E03 | STU-04; APP-04 | Transcript valid on day 180, issueDate = asOf minus 179 days | Eligible result false | Failed | [#11](https://github.com/aishaSalmii/UniPlace/issues/11) |
| E04 | STU-04; APP-04 | Transcript invalid on day 181 | Eligible result false | Passed | — |
| E05 | APP-04; /eligibility | GPA, credits and two missing document failures yield at least 4 reasons | Ineligible with at least 4 reasons | Passed | — |
| E06 | REL-02; EligibilityRequest domain | GPA 4.01 returns HTTP 400 naming gpa | Returned HTTP 200; field assertion not reached | Failed | [#13](https://github.com/aishaSalmii/UniPlace/issues/13) |
| E07 | /match skill normalization; APP-04 | Student skill ' java ' matches required 'JAVA'; score 70 with unmatched nonempty nice list | Score 0 | Failed | [#14](https://github.com/aishaSalmii/UniPlace/issues/14) |
| E08 | /match empty optional contribution; APP-04 | All required skills matched and no nice skills gives score 70 | Score 100 | Failed | [#15](https://github.com/aishaSalmii/UniPlace/issues/15) |
| E09 | /match rounding | Required contribution 17.5 rounds up to 18; nonempty unmatched nice list contributes 0 | Score 18 | Passed | — |
| E10 | APP-09; /rank | Ranking [1,2], aboveCut [1] with one seat | Ranking assertion passed; aboveCut was [1,2] | Failed | [#16](https://github.com/aishaSalmii/UniPlace/issues/16) |
| E11 | REL-02; /rank | Zero seats returns HTTP 400 naming seats | HTTP 400 with field seats | Passed | — |

## **Requirement to test traceability matrix**

| Requirement | Test cases | Issues found | Execution and remaining scope |
| --- | --- | --- | --- |
| APP-01 | A01–A04, M06 | #20, #21 (USA-06, USA-01) | A01–A04 passed. M06 refused submission but displayed a generic error and cleared input after temporary DevTools bypass; FAIL for USA-01/USA-06. |
| APP-02 | A05, A06, M02 | #5, #6, #30 | A05/A06 failed. M02 partially executed: manual withdrawn-count discrepancy reproduced; saved GOOD=6 but dashboard retained limit 5. Full GOOD/PROBATION boundary and enforcement checks incomplete; generic Field Operations refusal cause unconfirmed. |
| ADM-01 | M02 | #30 | PARTIALLY EXECUTED: GOOD limit saved as 6 and persisted on reopening Settings; student dashboard retained 5 without restart. Display update FAIL. Exact submission-refusal cause unconfirmed; remaining Settings/enforcement checks incomplete. Baseline restored to GOOD=5, PROBATION=2. |
| APP-03 | A07–A09 | #7 | A07 error; A08/A09 passed; no claim of concurrent idempotency |
| APP-04 | A02, A13, A16, E01–E09 | #4, #13, #14, #15 | Mixed executed results; complete Portal display/integration paths not yet verified |
| APP-05 | M01 | #31 | Supplemental integration: retry after commit PASS; synchronized overlapping submissions FAIL (2 records, expected 1). Isolated H2, mocked Engine. Browser/session/CSRF and real-network timeout checks remain untested. |
| APP-06 | A11, M05 | #9 | A11 failed. M05: all 49 state-machine pairs passed; service withdrawal bypassed restrictions for ACCEPTED, REJECTED and WITHDRAWN. Two cancellation checks passed. Full channel/authorization/offer-service matrix remains incomplete. |
| INT-04 | M05 | — | M05 cancellation returned INTERVIEW to SHORTLISTED and marked the interview cancelled; repeat cancellation and cancellation after OFFERED refused. Rescheduling, UI/API and wider authorization checks are not established by these two checks. |
| COM-02 | A12 | #10 | Suspended-company shortlist check failed; other channels not fully verified |
| APP-07 | A10 | #8 | Equality test error; other threshold/override/audit paths need review |
| APP-08 | A11, A14, M05 | #9 | M05 confirmed ACCEPTED and REJECTED persisted as WITHDRAWN; repeated WITHDRAWN call was not refused. Four permitted withdrawals passed. A14 unauthorized-student refusal passed; wider channel matrix incomplete. |
| APP-09 | E10 | #16 | Ranking order passed for fixture but seat cutoff failed; broader ranking datasets pending |
| APP-10 | A15, M03, M04 | #29 | A15 record-count pagination passed. M04 FAIL: State and Company filters lost on Next. Posting id 8 basic filtering passed; posting-filter pagination retention untested. Ownership matrix and M03 performance checks remain incomplete. |
| STU-04 | E03, E04 | #11 | Day 180 failed; day 181 passed; upload/type/size domains not fully checked here |
| REL-01 | A13 | — | Mocked Engine-outage integration check passed; live outage path still pending |
| REL-02 | E06, E11 | #13 | GPA domain check failed; zero-seat validation passed; wider invalid input matrix pending |
| USR-05 / SEC-05 | A14 | — | Unauthorized withdrawal refused; complete record/channel matrix belongs in shared security testing |
| PERF-01 | M03 | Pending | Not run; pagination count is not performance evidence |
| USA-01 / USA-06 | M06 | #21, #20 | FAIL: generic refusal message and loss of typed letter after temporary client-side bypass. |
| USA-07 | M04 | #29 | FAIL: State and Company filters cleared on Next. Posting id 8 basic filtering passed; posting-filter pagination retention untested. |

Requirement coverage here describes design and observed execution, not JaCoCo code coverage. A passed partial check does not establish that an entire requirement is satisfied. M01 and M02 are partially executed; Supplemental M05 state checks ran on 16 November 2026: 55 passed and 3 failed (#9). M05 system/channel scope remains incomplete. M03 remains Not run. Retain all stated testing limitations. Consolidate these reviewed cases with teammate cases in docs/test-cases.md before final submission.

