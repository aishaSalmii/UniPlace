# Offers and allowance: execution results (Areeg AlGhazali)

Module: Offers and allowance (OFF-01 to OFF-07), with the related ADM-01 settings, notifications (NOT-01 to NOT-04) and AI Assistant (AI-01 to AI-11) checks. Task issue: #12. Branch: `qa/offers-allowance`.
Test design and per-case results: [docs/areej-test-cases.md](areej-test-cases.md) (O-01 to O-25). Evidence folder: `tests/evidence/offers/`.

## Environment

- Windows, OpenJDK 21.0.13 (Temurin), Maven wrapper, Spring Boot 3.3.5
- Vendor Portal source and vendor Engine `engine/uniplace-engine.jar` (health: version 1.0.0), business code unchanged
- Python 3.12.5 for the Engine API, allowance, race and fake-provider scripts
- Manual and script runs used the Portal at http://127.0.0.1:8080 and the Engine at http://127.0.0.1:8081 after `start.cmd --jar --reset`
- GitHub Actions (Java 17, Ubuntu) reproduces the Portal test failures; the job is red because the acceptance tests fail on the defects below

## Summary

| Suite | Date | Kind | Run | Pass | Fail |
| --- | --- | --- | --- | --- | --- |
| 1. OffersAcceptanceTest (JUnit) | 15 Nov 2026 | White-box integration | 8 | 3 | 5 |
| 2. test_engine.py, offers part E12–E17 | 15 Nov 2026 | Black-box API | 6 | 4 | 2 |
| 3. OffersDeadlineBoundaryTest (JUnit) | 26 Nov 2026 | White-box, fixed clock | 7 | 5 | 2 |
| 4. test_last_seat_race.py | 26 Nov 2026 | Black-box concurrency | 3 | 0 | 3 |
| 5. test_allowance_boundaries.py | 26 Nov 2026 | Black-box API, independent oracle | 28 | 20 | 8 |
| 6. OffersAgreementTest (JUnit) | 27 Nov 2026 | White-box, request capture | 10 | 9 | 1 |
| 7. AiAssistantAcceptanceTest (JUnit) | 27 Nov 2026 | White-box, mocked provider | 14 | 3 | 11 |
| 8. Fake AI provider (capture, slow, invalid) | 27 Nov 2026 | Black-box, fake provider | 3 | 0 | 3 |
| 9. Manual checks on the running Portal | 26–27 Nov 2026 | Black-box UI and Outbox | see below | | |

## Run 1: Portal white-box integration tests

Command (from `portal`): `.\mvnw.cmd "-Dtest=OffersAcceptanceTest" test "-Dmaven.test.failure.ignore=true"`
Date: 15 Nov 2026, 19:21 (UTC+3). Result: **8 run, 3 passed, 5 failed, 0 errors**.
Evidence: `junit-OffersAcceptanceTest-2026-11-15.txt` and `.xml`

The Engine is mocked in this class, so these tests check Portal rules only, not allowance arithmetic.

| Test | Requirement | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| O01 | OFF-01 | INTERVIEW application can receive an offer | Offer PENDING, application OFFERED | Pass | |
| O02 | OFF-01 | SUBMITTED application refused | No error, offer created | Fail | #22 |
| O03 | OFF-01 | Posting allowance + 0.01 refused | Refused | Pass | |
| O04 | OFF-02 | Deadline 2026-11-15T14:30 | 2026-11-15T00:00 | Fail | #18 |
| O05 | OFF-02 | Acceptance after respondBy refused | Accepted, no error | Fail | #23 |
| O06 | OFF-03 | Other application WITHDRAWN | SUBMITTED | Fail | #24 |
| O07 | OFF-03 | Posting FILLED and other application REJECTED | FILLED, but other application SUBMITTED | Fail | #25 |
| O08 | USR-05 | Unrelated student cannot accept | Refused (ForbiddenException) | Pass | |

## Run 2: Engine black-box API tests

Command (from repository root, Engine already running): `py tests\acceptance\test_engine.py`
Date: 15 Nov 2026 (UTC+3). Result for the whole file: **18 run, 9 passed, 9 failed**.
Evidence: `engine-results-2026-11-15.txt`, `engine-trace-2026-11-15.json`

Allowance tests owned by Areeg (same dates 2026-01-01 to 2026-01-30 = 1 month, monthly 3000, transport 300, cap 5000, penalty 5% per started week capped at 20%):

| Test | Input change | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| E12 | 29 h | 3000.00 | 3000.00 | Pass | |
| E13 | 30 h | 3300.00 | 3000.00 (no transport line) | Fail | #26 |
| E14 | 29 h, 1 late day | 2850.00 | 2850.00 | Pass | |
| E15 | 29 h, 8 late days | 2700.00 | 2700.00 | Pass | |
| E16 | 4900, 31 h | 5000.00 | 5200.00 (no cap adjustment line) | Fail | #27 |
| E17 | 29 h, 100 late days | 2400.00 | 2400.00 | Pass | |

Manual confirmation of the line items (PowerShell `Invoke-RestMethod`): `engine-manual-30h-2026-11-15.json`, `engine-manual-cap-2026-11-15.json`.

The other 7 failures in this file are outside the offers module and were passed to the owning member: E02 (#4), E03 (#11), E06 (#13), E07 (#14), E08 (#15), E10 (#16) reported by Aisha; E18 (back-to-back interview slot rejected) reported to the team for triage.

## Run 3: Offer deadline boundaries (fixed clock)

Command (from `portal`): `.\mvnw.cmd "-Dtest=OffersDeadlineBoundaryTest" test "-Dmaven.test.failure.ignore=true"`
Date: 26 Nov 2026. Result: **7 run, 5 passed, 2 failed, 0 errors**. Evidence: `junit-OffersDeadlineBoundaryTest-2026-11-26.txt`

| Test | Requirement | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| D01 | OFF-02 | Accept 1 minute before the deadline | ACCEPTED | Pass | |
| D02 | OFF-02 | Accept exactly at the deadline | ACCEPTED | Pass | |
| D03 | OFF-02 | Accept 1 minute after the deadline refused | Accepted | Fail | #23 |
| D04 | OFF-02 | Overdue offer EXPIRED, application WITHDRAWN | As expected | Pass | |
| D05 | OFF-02 | Offer exactly at the deadline not expired | Stays PENDING | Pass | |
| D06 | ADM-01 | 96 hours: 2026-11-16T00:00 | 2026-11-16T00:00 | Pass | |
| D07 | ADM-01 | 73 hours: 2026-11-15T01:00 | 2026-11-15T00:00 | Fail | #18 |

## Run 4: Last-seat race (OFF-04)

Command (from repository root, Portal freshly reset): `py tests\acceptance\test_last_seat_race.py`
Two students (student.ibrahim, application 7; student.mubarak, application 8) accept offers for the single seat of posting 8 at the same moment (thread barrier). Repeated 3 times, each after `start.cmd --jar --reset`.

| Run | Time | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| 1 | 26 Nov 2026 18:49 | One accepted, the other refused with an explanation | Both accepted; posting Filled, 0 left | Fail | #38 |
| 2 | 26 Nov 2026 18:51 | Same | Both accepted | Fail | #38 |
| 3 | 26 Nov 2026 18:52 | Same | Both accepted | Fail | #38 |

Evidence: `last-seat-race-20261126-184919.json`, `-185112.json`, `-185213.json`. Seats never went below zero, but one seat was given to two students.

## Run 5: Allowance boundaries against an independent calculation (OFF-05)

Command (from repository root, Engine running): `py tests\acceptance\test_allowance_boundaries.py`
Date: 26 Nov 2026 19:02. Result: **28 run, 20 passed, 8 failed**. Evidence: `allowance-boundaries-20261126-190257.json`
The expected values come from a separate calculation in the script (OpenAPI calendar-month rule, exact fractions, ROUND_HALF_UP), not from the Engine.

| Group | Cases | Pass | Failed cases | Issue |
| --- | --- | --- | --- | --- |
| Transport threshold (29/30/31 h) | A01–A03 | 2 | A02: 3000.00, expected 3300.00 | #26 |
| Monthly cap | A04–A08 | 3 | A06: 5200.00, A08: 5300.00, expected 5000.00 | #27 |
| Calendar months | A09–A16, A28 | 6 | A11: 3100.00 (exp. 3000.00), A16: 3200.00 (exp. 3100.00), A28: 3100.00 (exp. 3000.00) | #39 |
| Late-documents penalty | A17–A23 | 7 | none | |
| Rounding | A24–A27 | 2 | A24: 100.00 (exp. 100.01), A27: 100.02 (exp. 100.03) | #40 |

## Run 6: Agreement recalculation and allowance settings (OFF-07, ADM-01)

Command (from `portal`): `.\mvnw.cmd "-Dtest=OffersAgreementTest" test "-Dmaven.test.failure.ignore=true"`
Date: 27 Nov 2026. Result: **10 run, 9 passed, 1 failed, 0 errors**. Evidence: `junit-OffersAgreementTest-2026-11-27.txt`

| Test | Requirement | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| G01–G03 | OFF-07 | No date, on start, before start: lateDays 0 | 0 | Pass | |
| G04–G06 | OFF-07 | +1, +7, +8 days: lateDays 1, 7, 8 | 1, 7, 8 | Pass | |
| G07 | OFF-07 | Date and new statement stored | Stored, penalty line present | Pass | |
| G08 | OFF-07 | Future date refused | Refused | Pass | |
| G09 | ADM-01 | Transport, penalty rate and cap settings sent | Sent | Pass | |
| G10 | ADM-01 | Monthly cap setting sent | Not sent | Fail | #47 |

## Run 7: AI Assistant prompts, parser, label and cache (mocked provider)

Command (from `portal`): `.\mvnw.cmd "-Dtest=AiAssistantAcceptanceTest" test "-Dmaven.test.failure.ignore=true"`
Date: 27 Nov 2026. Result: **14 run, 3 passed, 11 failed, 0 errors**. Evidence: `junit-AiAssistantAcceptanceTest-2026-11-27.txt`
No real model is called: the provider client is mocked.

| Test | Requirement | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| I01, I02 | AI-03 | No name, student number or GPA in prompts | Present | Fail | #51 |
| I03 | AI-10 | No internal rules or data | "pays QAR 3,000", "reject ... below 60" | Fail | #52 |
| I04, I05 | AI-11 | Examples and counts consistent with the format | Example score 8.5; "exactly three strengths" | Fail | #53 |
| I06 | AI-11, AI-02 | Questions prompt states 5 questions and JSON | Not stated | Fail | #54 |
| I07–I09 | AI-06 | 6 items, 0 items, score 150 refused | Accepted | Fail | #55 |
| I10 | AI-06 | Valid answer accepted | Accepted | Pass | |
| I11 | AI-07 | Simulated result labelled simulated | Labelled simulated | Pass | |
| I12 | AI-07 | Label names gpt-4.1-mini | "Model gpt-4o-mini" | Fail | #56 |
| I13 | AI-08 | Same letter served from the cache | Served from the cache | Pass | |
| I14 | AI-08 | Changed letter evaluated again | Old result served | Fail | #43 |

## Run 8: AI Assistant on the running Portal with a fake provider

Command (from repository root): `py tests\acceptance\fake_ai_provider.py capture` (then `slow`, then `invalid`)
Settings: AI provider "Other OpenAI-compatible endpoint", base URL `http://127.0.0.1:9999/v1`, API key `test-key`, model `fake-model-1`; restored afterwards.
Date: 27 Nov 2026, student.noor. **These are fake-provider results, not real-model results.**

| Mode | Posting | Requirement | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- | --- |
| capture | 1 | AI-03 | No personal data sent | "Student: Noor Al-Emadi (20231004), GPA 3.10" sent | Fail | #51 |
| capture | 1 | AI-07 | Label names fake-model-1 | "Model gpt-4o-mini" | Fail | #56 |
| slow | 13 | AI-05 | "Assistant unavailable" after 10 s | Page waited 30.0 s, then showed the result | Fail | #57 |
| invalid | 5 | AI-06 | Invalid answer refused | "150 / 100" and 6 strengths shown | Fail | #55 |

The captured requests also contain the text reported in #52 and #53, and use temperature 1.3. Evidence: `ai-fake-provider-capture.jsonl`, `ai-fake-provider-slow.jsonl`, `ai-fake-provider-invalid.jsonl`; screenshots in the issue comments.

## Run 9: Manual checks on the running Portal

| Case | Date | Check | Result | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| O-02 | 26 Nov 2026 | Offer allowance 3800.01, start one day early, end one day late (posting 10) | All refused with a message | Pass | |
| O-02 | 26 Nov 2026 | Offer at exactly 3800.00 and the posting's dates | Offer issued | Pass | |
| O-03 | 26 Nov 2026 | 72-hour deadline, issued 26/11 18:10 | Answer by 29/11 00:00 | Fail | #18 |
| O-04 | 26 Nov 2026 | Setting 96 hours applies without restart | Applied (30/11), time lost | Pass / Fail | #18 |
| O-05 | 26 Nov 2026 | Unanswered offer expires (setting 1 hour) | Expired and Withdrawn; deadline shown before the issue time | Pass | #18 |
| O-07 | 27 Nov 2026 | Remaining applicant notified when posting fills | No notification for student.ruqaya | Fail | #25 |
| O-13 | 27 Nov 2026 | Statement for owner, Coordinator, own recruiter | Owner and Coordinator see it; own recruiter does not | Pass | |
| O-13 | 27 Nov 2026 | Unrelated student and unrelated recruiter | Page opens; student sees the full statement | Fail | #35 |
| O-14 | 27 Nov 2026 | Record agreement date today; future date | Recalculated; future date refused | Pass | |
| O-18 | 27 Nov 2026 | Offer received, declined, expired notifications; NOT-03 | Correct recipients, posting and company named | Pass | |
| O-18 | 27 Nov 2026 | Offer accepted notifications after the race | Two acceptances recorded for one seat | Fail | #38 |
| O-19 | 27 Nov 2026 | One reminder per pending offer | Recorded at 19:13 and again at 19:23 | Fail | #48 |

## Defects reported by Areeg

| Issue | Title | Requirement | Category | Severity / priority (GitHub labels) |
| --- | --- | --- | --- | --- |
| #18 | Offer response deadline loses the issue time | OFF-02 | functional | Major / P1 |
| #22 | Offer can be issued to a SUBMITTED application without an interview | OFF-01 | state | Major / P1 |
| #23 | Offer can be accepted after its response deadline has passed | OFF-02 | state | Major / P1 |
| #24 | Accepting an offer does not withdraw the student's other active applications | OFF-03 | state | Major / P1 |
| #25 | Filling the last seat does not reject the posting's remaining active applications | OFF-03 | state | Major / P1 |
| #26 | Engine omits the transport supplement at exactly 30 weekly hours | OFF-05 | functional | Major / P1 |
| #27 | Engine does not apply the 5,000 monthly allowance cap | OFF-05 | functional | Major / P1 |
| #38 | Two students can accept the last seat at the same time (seat overbooked) | OFF-04 | concurrency | Major / P1 |
| #39 | Engine counts an extra day when the start day does not exist in the next month | OFF-05 | functional | Major / P2 |
| #40 | Allowance total is rounded half-to-even instead of halves up, and contradicts its own line | OFF-05 | functional | Major / P3 |
| #47 | Monthly allowance cap from Settings is never sent to the Engine | ADM-01, OFF-05 | configuration | Major / P2 |
| #48 | Offer reminder is recorded again every 10 minutes instead of once | NOT-04 | functional | Major / P2 |
| #49 | Specification contradiction: NOT-04 assumes a 48-hour response window, OFF-02 and ADM-01 define 72 hours (configurable) | NOT-04, OFF-02 | requirements | Major / P3 |
| #51 | AI prompts send the student's name, student number and GPA to the provider | AI-03 | ai | Critical / P1 |
| #52 | Letter-feedback prompt contains internal business rules and allowance data | AI-10 | ai | Major / P2 |
| #53 | Letter-feedback prompt contradicts its own format: example score 8.5 and "exactly three strengths" | AI-11 | ai | Major / P2 |
| #54 | Interview-questions prompt does not ask for 5 questions or a JSON answer, but the parser requires both | AI-11, AI-02 | ai | Major / P2 |
| #55 | AI answer validation accepts too many or too few items and scores outside 0–100 | AI-06 | ai | Major / P2 |
| #56 | AI feedback always names "gpt-4o-mini" instead of the model configured in Settings | AI-07 | ai | Minor / P3 |
| #57 | AI Assistant has no 10-second timeout: the page waits for a slow AI provider (AI-05) | AI-05 | ai | Major / P2 |

#19 was an accidental duplicate of #18 and is closed as duplicate. Where my proposed severity differed from the team's label, the issue text gives my reasoning: #38 proposed Critical; #40 and #49 proposed Minor.

Evidence added to issues reported by others: #35 (unrelated student and unrelated recruiter can open an accepted application), #43 (changed letter served from the AI cache, with root cause).

## Not run

- O-15: allowance and PERF-02 timing (no script yet).
- O-25: real-model AI evaluation (pilot, then 20 letters x 5 runs), covering AI-04 (instructions inside the letter) and AI-09 (score spread of at most 10 points). No real API key was available; requested from the team. All AI results above come from code-level tests or the fake provider, not from a real model.

## AI assistance

The test classes, scripts, test design draft and the wording of these reports were prepared with AI assistance (Claude). Areeg reviewed them against the requirements, ran every test and manual check herself, and reproduced each defect before reporting it. Recorded in `docs/ai-usage-log.md`.