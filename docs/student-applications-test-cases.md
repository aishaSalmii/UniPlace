# Student application test cases
Owner: Aisha. WB = white-box; BB = black-box. Results below reflect Aisha's execution on 14 November 2026, as shown in the shared terminal output and linked reports. Manual cases remain Not run. Passing integration tests verify their stated assertions with a mocked Engine; they do not establish real-provider or complete UI behavior.

## Execution details

- Date: 14 November 2026
- OS: macOS 26.6.2 (build 25G83)
- Java: Oracle Java 21.0.12.1 LTS
- Branch: qa/student-application-tests
- Uploaded JUnit evidence commit: 52a0172

| Suite | Executed | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Portal JUnit | 16 | 10 | 4 | 2 | 0 |
| Engine API | 11 | 5 | 6 | 0 | 0 |
| Total automated checks | 27 | 15 | 10 | 2 | 0 |

JUnit Error records an unexpected exception rather than an assertion failure. A07 and A10 raised business-rule exceptions for operations expected to succeed; they were not compiler or startup errors. A failed check is not automatically a separate defect.

### Evidence

- [JUnit text report](../evidence/student-applications-junit-results.txt)
- [JUnit XML report](../evidence/student-applications-junit-results.xml)
- [Engine text report](../evidence/application-engine-results.txt)
- [Engine request and response trace](../evidence/application-engine-trace.json)
- [Uploaded JUnit evidence at commit 52a0172](https://github.com/aishaSalmii/UniPlace/blob/52a0172/evidence/student-applications-junit-results.txt)

Issue links below record the reported findings: #4 GPA equality, #5 active count, #6 probation limit. Other observed findings still need their own reviewed reports. Keep existing screenshots and full raw evidence.

## Test case matrix
| Test case ID | Requirement | Module | Level | Technique and type | Preconditions | Steps | Test data | Expected result | Actual result | Status | Automated test | Issue |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A01 | APP-01 | Student applications | Integration | WB/BVA; Functional | Default approved company, eligible GOOD student, open posting; Engine mocked eligible. | Submit 99 ASCII x characters. | x ×99 | BusinessRuleException;0 saved applications. | 99-character letter refused; zero applications saved. | Passed | StudentApplicationsAcceptanceTest.A01_letter99IsRefused | — |
| A02 | APP-01; APP-04 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit100 characters. | x ×100; mocked score70 | SUBMITTED with match score70. | 100-character letter accepted as SUBMITTED with stored mocked match score 70. | Passed | StudentApplicationsAcceptanceTest.A02_letter100IsAccepted | — |
| A03 | APP-01 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit2000 characters. | x ×2000 | SUBMITTED. | 2,000-character letter accepted as SUBMITTED. | Passed | StudentApplicationsAcceptanceTest.A03_letter2000IsAccepted | — |
| A04 | APP-01 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit2001 characters. | x ×2001 | Refused;0 saved applications. | 2,001-character letter refused; zero applications saved. | Passed | StudentApplicationsAcceptanceTest.A04_letter2001IsRefused | — |
| A05 | APP-02 | Student applications | Integration | WB/predicate; Functional | One WITHDRAWN app; no other apps. | Count active apps. | WITHDRAWN | Count0. | Active count was 1 for one WITHDRAWN application; expected 0. | Failed | StudentApplicationsAcceptanceTest.A05_withdrawnDoesNotCountAsActive | [#5](https://github.com/aishaSalmii/UniPlace/issues/5) |
| A06 | APP-02 | Student applications | Integration | WB/decision branch; Functional | PROBATION student, default Settings. | Read active limit. | GPA1.80 | Limit2. | PROBATION limit was 5; expected 2. | Failed | StudentApplicationsAcceptanceTest.A06_probationLimitIsTwo | [#6](https://github.com/aishaSalmii/UniPlace/issues/6) |
| A07 | APP-03 | Student applications | Integration | WB/state partition; Functional | Only REJECTED app to same posting; still open; eligible, below active limit. | Submit new120-character letter. | REJECTED then retry | New SUBMITTED app;2 history records. | BusinessRuleException: You already have an active application for this posting. New submission did not succeed. | Error | StudentApplicationsAcceptanceTest.A07_reapplyAfterRejectedIsAllowed | Not reported yet |
| A08 | APP-03 | Student applications | Integration | WB/state partition; Functional | Only WITHDRAWN app, open posting and eligible student. | Submit new120-character letter. | WITHDRAWN then retry | New SUBMITTED app. | Reapplication after WITHDRAWN accepted as SUBMITTED. | Passed | StudentApplicationsAcceptanceTest.A08_reapplyAfterWithdrawnIsAllowed | — |
| A09 | APP-03 | Student applications | Integration | WB/duplicate partition; Functional | Eligible isolated student and open posting. | Submit once then submit again. | Same student/posting | Second refused;1 record. Does not prove concurrent APP-05 idempotency. | Second active application refused; one application record remained. | Passed | StudentApplicationsAcceptanceTest.A09_secondActiveApplicationIsRefused | — |
| A10 | APP-07 | Student applications | Integration | WB/BVA; Functional | SUBMITTED app;approved company;own Recruiter. | Shortlist without justification. | Score60;threshold60 | SHORTLISTED without override requirement. | BusinessRuleException: The match score 60 is below the shortlist threshold 60. Give a justification to shortlist it anyway. | Error | StudentApplicationsAcceptanceTest.A10_scoreExactlyThresholdNeedsNoOverride | Not reported yet |
| A11 | APP-06; APP-08 | Student applications | Integration | WB/state transition; Functional | ACCEPTED app owned by Student. | Call withdraw. | ACCEPTED | Refused;ACCEPTED retained. | Withdrawal call completed without the required BusinessRuleException. | Failed | StudentApplicationsAcceptanceTest.A11_acceptedCannotBeWithdrawn | Not reported yet |
| A12 | COM-02; APP-06 | Student applications | Integration | WB/branch; Functional | SUBMITTED app;company updated to SUSPENDED. | Own Recruiter attempts shortlist. | Score80 | Refused;state unchanged. | Shortlisting call completed without the required BusinessRuleException while company was SUSPENDED. | Failed | StudentApplicationsAcceptanceTest.A12_suspendedCompanyPreventsShortlisting | Not reported yet |
| A13 | REL-01; APP-04 | Student applications | Integration | WB/fault injection; Reliability | Eligible-looking student;Engine mock throws unavailable. | Submit valid letter. | EngineUnavailableException | Specific exception;no application persisted. | EngineUnavailableException raised; no application persisted. | Passed | StudentApplicationsAcceptanceTest.A13_engineOutageRefusesSubmission | — |
| A14 | USR-05; SEC-05; APP-08 | Student applications | Integration | WB/ownership decision; Security | App belongs to different Student. | Other student attempts withdraw. | Distinct student IDs | ForbiddenException;no unauthorized mutation. | Unrelated student withdrawal raised ForbiddenException. The test does not separately assert persisted state afterwards. | Passed | StudentApplicationsAcceptanceTest.A14_otherStudentCannotWithdraw | — |
| A15 | APP-10 | Student applications | Integration | WB/volume boundary; Functional | 21 historical rejected apps belonging to same student. | Request pages0 and1. | 21 records | 20 records then1; no loss. | Page 0 contained 20 records and page 1 contained 1. This does not establish response-time performance. | Passed | StudentApplicationsAcceptanceTest.A15_paginationReturns20ThenOne | — |
| A16 | APP-04 | Student applications | Integration | WB/negative partition; Functional | Engine mock refuses with2 reasons. | Submit otherwise valid letter. | GPA reason and missingCV reason | Both reasons retained in exception;0 app records. UI display needs separate test. | Both mocked eligibility reasons retained in exception; no application saved. UI display not tested. | Passed | StudentApplicationsAcceptanceTest.A16_everyEligibilityReasonIsRetained | — |
| M01 | APP-05 | Student applications | System | BB/concurrency and retry; Reliability | Eligible student, open posting, no duplicate; real sessions/CSRF. | Synchronize2 submissions of one logical form and retry after simulated timeout. | Same student/posting/letter | Exactly1 saved app; sensible repeat response. | Not recorded | Not run | Pending/manual | Pending |
| M02 | APP-02; ADM-01 | Student applications | System | BB/BVA and decision table; Functional | Fresh open eligible postings;set default limits GOOD5 PROBATION2. | Test GOOD4→5→6 and PROBATION1→2→3;then change Settings and repeat. | Boundary counts per standing | Fifth/second allowed; sixth/third refused;Settings immediate. | Not recorded | Not run | Pending/manual | Pending |
| M03 | APP-10; PERF-01 | Student applications | System | BB/volume and timing; Performance | Seed plus1000 extra apps on recorded reference hardware; authenticated. | Sample at least100 application-list requests and calculate nearest-rank p95. | Page/filter combinations | p95<=2000ms;pages20;correct ownership. Not measured by A15. | Not recorded | Not run | Pending/manual | Pending |
| M04 | APP-10; USA-07 | Student applications | System | BB/filter partition; Usability | Multiple states/postings/companies and more than1 page. | Apply role-appropriate filters and navigate pages. | State,posting,company | Correct filtered results and criteria retained. | Not recorded | Not run | Pending/manual | Pending |
| M05 | APP-06; INT-04 | Student applications | Integration and system | BB/state transitions and WB paths; Functional | Isolated fixtures for each source state;correct role/company. | Attempt each allowed transition and forbidden transition, including direct requests;cancel interview. | All7 application states | Only allowed transitions;cancel INTERVIEW→SHORTLISTED;ACCEPTED final. | Not recorded | Not run | Pending/manual | Pending |
| M06 | USA-01; USA-06; APP-01 | Student applications | System | BB/negative validation; Usability | Open application form,valid session. | Type invalid-length letter and submit. | 99 characters | Specific rule/value explained;typed content retained. | Not recorded | Not run | Pending/manual | Pending |

## Supplemental real Engine checks

These are black-box HTTP API checks in tests/acceptance/test_application_engine.py. Each request is stateless and uses explicit fixture dates. Full payloads and responses are saved in evidence/application-engine-trace.json. Results below are from Aisha's execution, not merely the earlier preparation run.

| Test | Requirement or contract | Expected result | Actual result | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| E01 | APP-04; /eligibility | Valid baseline eligible with no reasons | Baseline eligible; reasons empty | Passed | — |
| E02 | APP-04; /eligibility GPA at least minGpa | GPA 2 with minGpa 2 eligible | Ineligible; reason says GPA 2 is below minimum 2 | Failed | [#4](https://github.com/aishaSalmii/UniPlace/issues/4) |
| E03 | STU-04; APP-04 | Transcript valid on day 180, issueDate = asOf minus 179 days | Eligible result false | Failed | Not reported yet |
| E04 | STU-04; APP-04 | Transcript invalid on day 181 | Eligible result false | Passed | — |
| E05 | APP-04; /eligibility | GPA, credits and two missing document failures yield at least 4 reasons | Ineligible with at least 4 reasons | Passed | — |
| E06 | REL-02; EligibilityRequest domain | GPA 4.01 returns HTTP 400 naming gpa | Returned HTTP 200; field assertion not reached | Failed | Not reported yet |
| E07 | /match skill normalization; APP-04 | Student skill ' java ' matches required 'JAVA'; score 70 with unmatched nonempty nice list | Score 0 | Failed | Not reported yet |
| E08 | /match empty optional contribution; APP-04 | All required skills matched and no nice skills gives score 70 | Score 100 | Failed | Not reported yet |
| E09 | /match rounding | Required contribution 17.5 rounds up to 18; nonempty unmatched nice list contributes 0 | Score 18 | Passed | — |
| E10 | APP-09; /rank | Ranking [1,2], aboveCut [1] with one seat | Ranking assertion passed; aboveCut was [1,2] | Failed | Not reported yet |
| E11 | REL-02; /rank | Zero seats returns HTTP 400 naming seats | HTTP 400 with field seats | Passed | — |

## Requirement to test traceability matrix

| Requirement | Test cases | Issues found | Execution and remaining scope |
| --- | --- | --- | --- |
| APP-01 | A01–A04, M06 | — | A01–A04 passed; UI refusal/input retention M06 not run |
| APP-02 | A05, A06, M02 | #5, #6 | A05/A06 failed; limit enforcement and immediate Settings changes M02 not run |
| APP-03 | A07–A09 | A07 report pending | A07 error; A08/A09 passed; no claim of concurrent idempotency |
| APP-04 | A02, A13, A16, E01–E09 | #4; other Engine findings pending | Mixed executed results; complete Portal display/integration paths not yet verified |
| APP-05 | M01 | Pending | Concurrent/retried form submission not run |
| APP-06 | A11, M05 | A11 report pending | A11 failed; complete state/channel matrix M05 not run |
| COM-02 | A12 | Report pending | Suspended-company shortlist check failed; other channels not fully verified |
| APP-07 | A10 | Report pending | Equality test error; other threshold/override/audit paths need review |
| APP-08 | A11, A14 | A11 report pending | Accepted withdrawal failed; unauthorized-student refusal passed |
| APP-09 | E10 | Report pending | Ranking order passed for fixture but seat cutoff failed; broader ranking datasets pending |
| APP-10 | A15, M03, M04 | Pending | Record-count pagination passed; filters/ownership/performance matrix not complete |
| STU-04 | E03, E04 | E03 report pending | Day 180 failed; day 181 passed; upload/type/size domains not fully checked here |
| REL-01 | A13 | — | Mocked Engine-outage integration check passed; live outage path still pending |
| REL-02 | E06, E11 | E06 report pending | GPA domain check failed; zero-seat validation passed; wider invalid input matrix pending |
| USR-05 / SEC-05 | A14 | — | Unauthorized withdrawal refused; complete record/channel matrix belongs in shared security testing |
| PERF-01 | M03 | Pending | Not run; pagination count is not performance evidence |
| USA-01 / USA-06 | M06 | Pending | Not run |
| USA-07 | M04 | Pending | Not run |

Requirement coverage here describes design and observed execution, not JaCoCo code coverage. A passed partial check does not establish that an entire requirement is satisfied. Preserve manual cases as Not run until executed. Consolidate these reviewed cases with teammate cases in docs/test-cases.md before final submission.

