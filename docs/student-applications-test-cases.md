# Student application test cases

Owner: Aisha. Draft for review and actual execution. WB=white-box;BB=black-box. New Java starters are unexecuted; all personal statuses start Not run. Engine reference execution is separate from your own evidence.

## Test case matrix

| Test case ID | Requirement | Module | Level | Technique and type | Preconditions | Steps | Test data | Expected result | Actual result | Status | Automated test | Issue |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A01 | APP-01 | Student applications | Integration | WB/BVA; Functional | Default approved company, eligible GOOD student, open posting; Engine mocked eligible. | Submit 99 ASCII x characters. | x ×99 | BusinessRuleException;0 saved applications. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A01_letter99IsRefused | Pending |
| A02 | APP-01; APP-04 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit100 characters. | x ×100; mocked score70 | SUBMITTED with match score70. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A02_letter100IsAccepted | Pending |
| A03 | APP-01 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit2000 characters. | x ×2000 | SUBMITTED. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A03_letter2000IsAccepted | Pending |
| A04 | APP-01 | Student applications | Integration | WB/BVA; Functional | Same isolated baseline. | Submit2001 characters. | x ×2001 | Refused;0 saved applications. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A04_letter2001IsRefused | Pending |
| A05 | APP-02 | Student applications | Integration | WB/predicate; Functional | One WITHDRAWN app; no other apps. | Count active apps. | WITHDRAWN | Count0. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A05_withdrawnDoesNotCountAsActive | Pending |
| A06 | APP-02 | Student applications | Integration | WB/decision branch; Functional | PROBATION student, default Settings. | Read active limit. | GPA1.80 | Limit2. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A06_probationLimitIsTwo | Pending |
| A07 | APP-03 | Student applications | Integration | WB/state partition; Functional | Only REJECTED app to same posting; still open; eligible, below active limit. | Submit new120-character letter. | REJECTED then retry | New SUBMITTED app;2 history records. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A07_reapplyAfterRejectedIsAllowed | Pending |
| A08 | APP-03 | Student applications | Integration | WB/state partition; Functional | Only WITHDRAWN app, open posting and eligible student. | Submit new120-character letter. | WITHDRAWN then retry | New SUBMITTED app. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A08_reapplyAfterWithdrawnIsAllowed | Pending |
| A09 | APP-03 | Student applications | Integration | WB/duplicate partition; Functional | Eligible isolated student and open posting. | Submit once then submit again. | Same student/posting | Second refused;1 record. Does not prove concurrent APP-05 idempotency. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A09_secondActiveApplicationIsRefused | Pending |
| A10 | APP-07 | Student applications | Integration | WB/BVA; Functional | SUBMITTED app;approved company;own Recruiter. | Shortlist without justification. | Score60;threshold60 | SHORTLISTED without override requirement. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A10_scoreExactlyThresholdNeedsNoOverride | Pending |
| A11 | APP-06; APP-08 | Student applications | Integration | WB/state transition; Functional | ACCEPTED app owned by Student. | Call withdraw. | ACCEPTED | Refused;ACCEPTED retained. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A11_acceptedCannotBeWithdrawn | Pending |
| A12 | COM-02; APP-06 | Student applications | Integration | WB/branch; Functional | SUBMITTED app;company updated to SUSPENDED. | Own Recruiter attempts shortlist. | Score80 | Refused;state unchanged. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A12_suspendedCompanyPreventsShortlisting | Pending |
| A13 | REL-01; APP-04 | Student applications | Integration | WB/fault injection; Reliability | Eligible-looking student;Engine mock throws unavailable. | Submit valid letter. | EngineUnavailableException | Specific exception;no application persisted. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A13_engineOutageRefusesSubmission | Pending |
| A14 | USR-05; SEC-05; APP-08 | Student applications | Integration | WB/ownership decision; Security | App belongs to different Student. | Other student attempts withdraw. | Distinct student IDs | ForbiddenException;no unauthorized mutation. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A14_otherStudentCannotWithdraw | Pending |
| A15 | APP-10 | Student applications | Integration | WB/volume boundary; Functional | 21 historical rejected apps belonging to same student. | Request pages0 and1. | 21 records | 20 records then1; no loss. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A15_paginationReturns20ThenOne | Pending |
| A16 | APP-04 | Student applications | Integration | WB/negative partition; Functional | Engine mock refuses with2 reasons. | Submit otherwise valid letter. | GPA reason and missingCV reason | Both reasons retained in exception;0 app records. UI display needs separate test. | Not recorded | Not run | StudentApplicationsAcceptanceTest.A16_everyEligibilityReasonIsRetained | Pending |
| M01 | APP-05 | Student applications | System | BB/concurrency and retry; Reliability | Eligible student, open posting, no duplicate; real sessions/CSRF. | Synchronize2 submissions of one logical form and retry after simulated timeout. | Same student/posting/letter | Exactly1 saved app; sensible repeat response. | Not recorded | Not run | Pending/manual | Pending |
| M02 | APP-02; ADM-01 | Student applications | System | BB/BVA and decision table; Functional | Fresh open eligible postings;set default limits GOOD5 PROBATION2. | Test GOOD4→5→6 and PROBATION1→2→3;then change Settings and repeat. | Boundary counts per standing | Fifth/second allowed; sixth/third refused;Settings immediate. | Not recorded | Not run | Pending/manual | Pending |
| M03 | APP-10; PERF-01 | Student applications | System | BB/volume and timing; Performance | Seed plus1000 extra apps on recorded reference hardware; authenticated. | Sample at least100 application-list requests and calculate nearest-rank p95. | Page/filter combinations | p95<=2000ms;pages20;correct ownership. Not measured by A15. | Not recorded | Not run | Pending/manual | Pending |
| M04 | APP-10; USA-07 | Student applications | System | BB/filter partition; Usability | Multiple states/postings/companies and more than1 page. | Apply role-appropriate filters and navigate pages. | State,posting,company | Correct filtered results and criteria retained. | Not recorded | Not run | Pending/manual | Pending |
| M05 | APP-06; INT-04 | Student applications | Integration and system | BB/state transitions and WB paths; Functional | Isolated fixtures for each source state;correct role/company. | Attempt each allowed transition and forbidden transition, including direct requests;cancel interview. | All7 application states | Only allowed transitions;cancel INTERVIEW→SHORTLISTED;ACCEPTED final. | Not recorded | Not run | Pending/manual | Pending |
| M06 | USA-01; USA-06; APP-01 | Student applications | System | BB/negative validation; Usability | Open application form,valid session. | Type invalid-length letter and submit. | 99 characters | Specific rule/value explained;typed content retained. | Not recorded | Not run | Pending/manual | Pending |

## Supplemental real Engine checks

Run tests/acceptance/test_application_engine.py; E01–E06 cover eligibility/document validity and validation;E07–E09 cover matching;E10–E11 cover ranking/cutoff and seat validation. These use black-box contract oracles. Add your actual results and issue links.

## Requirement to test traceability matrix

| Requirement | Test cases | Automated tests | Issues found | Covered? |
| --- | --- | --- | --- | --- |
| APP-01 | A01–A04 | See case rows and API suite | Pending | Designed; execution pending |
| APP-02 | A05,A06,M02 | See case rows and API suite | Pending | Designed; execution pending |
| APP-03 | A07–A09 | See case rows and API suite | Pending | Designed; execution pending |
| APP-04 | A02,A13,A16,E01–E09 | See case rows and API suite | Pending | Designed; execution pending |
| APP-05 | M01 | See case rows and API suite | Pending | Designed; execution pending |
| APP-06 | A11,A12,M05 | See case rows and API suite | Pending | Designed; execution pending |
| APP-07 | A10 | See case rows and API suite | Pending | Designed; execution pending |
| APP-08 | A11,A14 | See case rows and API suite | Pending | Designed; execution pending |
| APP-09 | E10,E11 | See case rows and API suite | Pending | Designed; execution pending |
| APP-10 | A15,M03,M04 | See case rows and API suite | Pending | Designed; execution pending |
| REL-01 | A13 | See case rows and API suite | Pending | Designed; execution pending |
| USR-05 / SEC-05 | A14 | See case rows and API suite | Pending | Designed; execution pending |
| PERF-01 | M03 | See case rows and API suite | Pending | Designed; execution pending |
| USA-01 / USA-06 | M06 | See case rows and API suite | Pending | Designed; execution pending |
| USA-07 | M04 | See case rows and API suite | Pending | Designed; execution pending |

Coverage here means requirements design/execution traceability, not JaCoCo code coverage. Do not mark a requirement fully verified from one partial check. Keep pending manual paths visible. Consolidate with teammates into docs/test-cases.md before final submission.
