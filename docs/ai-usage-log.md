# AI usage log

One entry per meaningful use.

| Date | Member | Tool and model | Purpose | Prompt summary | Kept | Modified (how) | Rejected (why) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 13–14 November 2026 | Aisha | ChatGPT; model not recorded | Address difficulty organizing the test strategy and GitHub workflow | Explain the setup steps and help structure the initial testing plan | Strategy draft and workflow guidance | Used module-based branch names without personal names | None recorded |
| 24–27 November 2026 | Lana | ChatGPT (GPT-6) | Users and Security testing, automated tests and GitHub workflow | Explain JUnit and MockMvc tests, reproduce security defects, troubleshoot Maven and update documentation | Test guidance, security regression test drafts and GitHub instructions | Reviewed code against requirements, ran tests locally, checked execution evidence and updated test-case documentation | Did not change expected security results to make failing tests pass |
| 15–19 November 2026 | Areeg | Claude (Anthropic) | Step guidance and wording help | Explain steps and output; suggest wording | AI-assisted starter test code, initial test-case draft and report wording | Reviewed tests against requirements, executed JUnit O01–O08 and Engine E01–E18, collected evidence and reported #18 and #22–#27 | Did not treat generated code or explanations as execution evidence |
| 26–27 November 2026 | Areeg | Claude (Anthropic); model reported by Areeg as Claude Opus 5.5 | Deadline, concurrency, allowance, agreement, notification and AI Assistant testing | Draft tests and scripts from requirements and OpenAPI; interpret results and draft reports | AI-assisted drafts of OffersDeadlineBoundaryTest, OffersAgreementTest, AiAssistantAcceptanceTest, test_last_seat_race.py, test_allowance_boundaries.py and fake_ai_provider.py; report wording | Reviewed expected values, executed tests and manual checks, collected evidence for #38–#40, #47–#49 and #51–#57; corrected an ambiguous prompt assertion, incorrect race-message extraction and chunked-request handling | Did not weaken correct assertions, change vendor business code or claim AI-04/AI-09 results without real-model execution |
| 13–14 November 2026 | Aisha | ChatGPT; model not recorded | Design and draft acceptance/Engine checks | Help implement requirement-based boundary, state, ownership and Engine contract checks | StudentApplicationsAcceptanceTest.java; test_application_engine.py; case/results drafts and command guidance | Results populated from Aisha's actual 16 JUnit and 11 Engine executions; distinguished assertion failures from unexpected exceptions, mocks from real Engine calls, and partial coverage from complete requirements | A candidate Engine rounding finding was not retained when a corrected fixture returned 18; empty optional-skill scoring was separated from rounding |
| 14–16 November 2026 | Aisha | ChatGPT; model not recorded | Draft defect reports and traceability | Explain failures and prepare reproducible reports with evidence and requirement links | Issue drafts for #4–#11, #13–#16, #20, #21, #29–#31; report inventory | Aisha performed submissions and supplied issue numbers/screenshots; repeated findings grouped under existing issues, including M05 evidence under #9 | Failure count was not treated as distinct bug count; Engine internal causes were not asserted from black-box results |
| 16 November 2026 | Aisha | ChatGPT; model not recorded | Guide M04/M06 manual checks | Test filter retention, error specificity and retained input | Step guidance and case/results text | M06 disabled-button attribute temporarily removed in DevTools and explicitly documented; M04 tested State/Company pagination plus a one-page Posting id fixture | Initial repeated-click advice for a disabled button did not reveal server behavior; corrected after source inspection. Posting-filter pagination was not claimed tested |
| 16 November 2026 | Aisha | ChatGPT; model not recorded | Guide partial M02 execution | Inspect counts and test a saved GOOD-limit change; inspect PROBATION behavior | Recorded count discrepancies, dashboard/Settings mismatch and issue #30 draft | Aisha confirmed Settings=6 and dashboard limit=5, restored GOOD=5 and PROBATION=2; recorded Omar eligibility refusal separately from limit enforcement | Generic Field Operations refusals were not attributed to a proven cause; no claim that second/third PROBATION submissions were permitted |
| 16 November 2026 | Aisha | ChatGPT; model not recorded | Interpret CI results and calculate line coverage | Distinguish known failing tests, code coverage and correctness | Historical run 43735551175 summary; service line coverage 72.71% and ApplicationService 69.35% | Used line counts rather than displayed instruction coverage; attributed figures to combined vendor and added tests | Later concurrency-run coverage figures excluded because JaCoCo reported class/execution-data mismatches; coverage alone not treated as MAINT-01 compliance |
| 16 November 2026 | Aisha | ChatGPT; model not recorded | Generate supplemental APP-05 integration checks | Add committed-retry and synchronized-overlap checks without changing production code | StudentApplicationConcurrencyTest.java | Aisha ran locally: 2 checks, 1 pass, 1 failure; two persisted records reported in #31 and reports committed at 5b7c910 | AI environment could not execute Maven because its download host was unreachable; no AI execution claim. Browser/session/CSRF and real-network timeout coverage not claimed |
| 16 November 2026 | Aisha | ChatGPT; model not recorded | Generate supplemental APP-06/APP-08/INT-04 checks | Check 49 requirement-derived state pairs, 7 withdrawal states and 2 cancellation cases | StudentApplicationStateTransitionsTest.java | Aisha ran locally: 58 checks, 55 pass, 3 failures; persisted-state assertions strengthened #9; reports committed at 9cf30b9 | Passing state-machine checks not treated as proof that every service/channel enforces them; browser/API, full authorization and offer-service coverage remain incomplete |
| 16 November 2026 | Aisha | ChatGPT; model not recorded | Reconcile retained documentation | Update M05 traceability, totals, issue inventory and this log using supplied current files | Updated three Markdown documents | Preserved testing limits and historical CI provenance; combined 87 checks are separately executed suites, not one full-suite run; teammate review remains pending | Obsolete Not run/pending-report statements removed where execution or issue creation was evidenced; no new tests, production fixes or teammate contributions invented |

| 16 November 2026 | Aisha | ChatGPT; model not recorded | Prepare and verify partial M03 performance measurement | Generate a coordinator-authenticated timing runner and interpret 100-request evidence | tests/performance/measure_application_list.py; M03 result and traceability text | Aisha generated 1,000 records once and ran locally; ChatGPT recalculated CSV mean/median/p95/max and checked 100 HTTP 200 valid rows against summary; p95 16.572834 ms | Did not count 100 timing samples as additional functional tests; did not claim reference-hardware, rendering, ownership, concurrent-user or full PERF-01 compliance; screenshot-only results replaced by CSV verification |

| 19 November 2026 | Aisha | ChatGPT; model not recorded | Prepare and revise M01 HTTP concurrency/retry script and update documentation | Generate simultaneous authenticated submissions and a sequential retry; interpret the HTTP 405 precheck and execution evidence | Revised script, reproduction comment and documentation updates | Ran the script locally; verified applications 1118 and 1119 in the UI; linked evidence to existing issue #31 | Rejected treating HTTP 405 as proof of CSRF protection or claiming actual timeout/browser double-click testing |

| 19–20 November 2026 | Aisha | ChatGPT; model not recorded | Guide M02 boundary and Settings checks and document results | Plan an eligible test account, test the fifth/sixth submission and compare saved limits with the dashboard | Manual steps and documentation wording | Executed checks myself, verified five SUBMITTED records, confirmed saved GOOD=6 versus dashboard 5 of 5, and restored GOOD=5/PROBATION=2 | Did not claim the generic refusal proved its cause or that PROBATION boundaries were completed |

| 2026-11-22 | Aisha | ChatGPT | Support acceptance testing and evidence documentation | Helped investigate an eligibility mismatch, plan controlled boundary checks, and summarise observed results | Modified and verified through manual execution, screenshots and API checks |

| 2026-11-25 | Aisha | ChatGPT | Guide additional manual checks and update documentation | Helped plan Settings and pagination checks and summarise observed evidence | Modified wording against screenshots; kept submission-refusal cause unconfirmed |
## Reflection

## Reflection

AI helped clarify unfamiliar planning and GitHub steps.
Command outputs were checked to confirm completed actions.

The testing strategy was developed collaboratively. Strategy
PR #2 was merged after team review. Users and Security work
was merged through PRs #37 and #41. Student Applications
PR #17 was merged on 27 November 2026.

AI assistance included retained code and documentation, not
only explanations. Aisha supplied local execution output and
performed the manual UI checks and GitHub actions. Aisha's
model/version was not recorded at the time and is not
reconstructed here. Local reports, screenshots and recorded
commits support the results. Generated tests were not assumed
to pass before execution.

The most useful verification was separating testing layers:
a passing state-machine check did not guarantee enforcement
by the withdrawal service, and a passing integration check
with a mocked Engine did not establish UI or live Engine
behaviour. Misleading suggestions were corrected, including
repeated clicks on a disabled button and interpreting
instruction coverage as line coverage.

The combined Portal execution on 27 November 2026 ran
236 tests: 217 passed, 17 failures, 2 errors and 0 skipped.
Service-layer line coverage was 74.16% (769 of 1,037 lines).
These results include vendor and team tests. They do not
establish that all requirements were satisfied. Evidence is
preserved in evidence/combined-2026-11-27/. This execution
does not include the phase-two tests submitted in PR #58.

Areeg used Claude for explanations, test and script drafts,
and report wording. She reviewed the retained work against
the requirements and recorded results from her own executions
on 15 and 26–27 November 2026. Comparing the Engine with an
independent calculation and testing the Portal with a local
fake provider exposed failures beyond successful inputs.

Some AI-assisted drafts required correction after execution:
a GPA value matched an overly broad question-count assertion,
the race script extracted the wrong message, and the fake
provider initially could not read chunked requests. These
problems were corrected without weakening requirement-based
expected results.

Areeg's AI Assistant evidence is identified as mocked-client,
simulated-provider or local fake-provider testing. It does
not establish real-model behaviour. Real-model evaluation
remained pending because credentials were unavailable.

Lana used AI assistance for Users and Security test design,
automated regression checks, GitHub workflow and documentation.
Execution claims must agree with the corresponding reports,
issue evidence and repository history. The Settings-CSRF
test's failed HTTP assertion does not, by itself, verify its
later unchanged-setting assertion; any mutation claim needs
separate evidence.

Remaining performance, real-model AI and system-test gaps
are explicitly retained. AI-generated explanations were not
treated as execution evidence. Each member remains responsible
for verifying their own recorded contribution.
