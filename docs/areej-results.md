# Offers and allowance: execution results (Areeg AlGhazali)

Module: Offers and allowance (OFF-01 to OFF-07). Task issue: #12. Branch: `qa/offers-allowance`.
Test design with per-case results: [areej-test-cases.md](areej-test-cases.md). Evidence folder: `tests/evidence/offers/` (commit a781aa4).

## Environment

- Windows, OpenJDK 21.0.13 (Temurin), Maven wrapper, Spring Boot 3.3.5
- Vendor Portal source and vendor Engine `engine/uniplace-engine.jar` (health reports version 1.0.0), business code unchanged
- Python 3.12.5 for the Engine API script
- GitHub Actions "Tests" run #27 on commit a781aa4 ended red because the new acceptance tests fail on the defects below (reports in the `portal-reports` artifact)

## Run 1: Portal white-box integration tests

Command (from `portal`): `.\mvnw.cmd "-Dtest=OffersAcceptanceTest" test "-Dmaven.test.failure.ignore=true"`
Date: 15 Nov 2026, 19:21 (UTC+3). Result: **8 run, 3 passed, 5 failed, 0 errors**.
Evidence: `tests/evidence/offers/junit-OffersAcceptanceTest-2026-11-15.txt` and `.xml`

The Engine is replaced by a mock in this class, so these tests check Portal rules only, not the allowance arithmetic.

| Test | Requirement | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| O01 | OFF-01 | INTERVIEW application can receive an offer | Offer PENDING, application OFFERED | Pass | |
| O02 | OFF-01 | SUBMITTED application refused | No error, offer created | Fail | #22 |
| O03 | OFF-01 | Posting allowance + 0.01 refused | Refused | Pass | |
| O04 | OFF-02 | Deadline 2026-11-15T14:30 | 2026-11-15T00:00 | Fail | #18 |
| O05 | OFF-02 | Acceptance after respondBy refused | Accepted, no error | Fail | #23 |
| O06 | OFF-03 | Other application WITHDRAWN | SUBMITTED | Fail | #24 |
| O07 | OFF-03 | Posting FILLED and other application REJECTED | FILLED, but other application SUBMITTED | Fail | #25 |
| O08 | USR-05 | Unrelated student cannot accept | Refused | Pass | |

## Run 2: Engine black-box API tests

Command (from repository root, Engine already running): `py tests\acceptance\test_engine.py`
Date: 15 Nov 2026 (UTC+3). Result for the whole file: **18 run, 9 passed, 9 failed**.
Evidence: `tests/evidence/offers/engine-results-2026-11-15.txt`, `engine-trace-2026-11-15.json`

My allowance tests (dates 2026-01-01 to 2026-01-30 = 1 month, monthly 3000, transport 300, cap 5000, penalty 5% per started week capped at 20%):

| Test | Input change | Expected | Actual | Status | Issue |
| --- | --- | --- | --- | --- | --- |
| E12 | 29 h | 3000.00 | 3000.00 | Pass | |
| E13 | 30 h | 3300.00 | 3000.00 (no transport line) | Fail | #26 |
| E14 | 29 h, 1 late day | 2850.00 | 2850.00 | Pass | |
| E15 | 29 h, 8 late days | 2700.00 | 2700.00 | Pass | |
| E16 | 4900, 31 h | 5000.00 | 5200.00 (no cap adjustment line) | Fail | #27 |
| E17 | 29 h, 100 late days | 2400.00 | 2400.00 | Pass | |

Manual confirmation of the statement lines (PowerShell `Invoke-RestMethod`): `engine-manual-30h-2026-11-15.json`, `engine-manual-cap-2026-11-15.json`.

The other 7 failures in this file are outside the offers module and belong to other members: E02 (#4), E03 (#11), E06 (#13), E07 (#14), E08 (#15) and E10 (#16) were already reported by Aisha; E18 (back-to-back interview slot rejected) was passed to the team for triage.

## Defects I reported

| Issue | Title | Requirement | Category | Severity / priority (proposed) |
| --- | --- | --- | --- | --- |
| #18 | Offer response deadline loses the issue time | OFF-02 | functional | Major / P1 |
| #22 | Offer can be issued to a SUBMITTED application without an interview | OFF-01 | state | Major / P1 |
| #23 | Offer can be accepted after its response deadline has passed | OFF-02 | state | Major / P1 |
| #24 | Accepting an offer does not withdraw the student's other active applications | OFF-03 | state | Major / P1 |
| #25 | Filling the last seat does not reject the posting's remaining active applications | OFF-03 | state | Major / P1 |
| #26 | Engine omits the transport supplement at exactly 30 weekly hours | OFF-05 | functional | Major / P1 |
| #27 | Engine does not apply the 5,000 monthly allowance cap | OFF-05 | functional | Major / P1 |

#19 was an accidental duplicate of #18 and is closed as a duplicate.

## Not run yet

- O-02: exact allowance, exact posting dates, dates one day outside
- O-04: 73-hour response setting
- O-07: notification check
- O-08: last-seat concurrency (OFF-04)
- O-12: calendar-month durations
- O-13: allowance statement access by role (OFF-06)
- O-14: agreement recalculation (OFF-07)
- O-15: timing

## AI assistance

The starter test classes, the first draft of the test design, and the wording of the defect reports and this summary were prepared with AI assistance. I ran every test myself, compared each result with the requirement, and reproduced each defect before reporting it. Recorded in `docs/ai-usage-log.md`.