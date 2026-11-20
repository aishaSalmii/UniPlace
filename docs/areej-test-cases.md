# Offers and allowance test design

Reviewed and executed by Areeg AlGhazali. The case designs started as an AI-assisted draft; the execution results under each case are from my own runs. Cases or datasets that have not been executed yet are marked **Not run**.

Environment for all runs: Windows, OpenJDK 21.0.13 (Temurin), Maven wrapper, Python 3.12.5, vendor Portal source and vendor Engine 1.0.0 unchanged. Branch `qa/offers-allowance`, evidence commit a781aa4.

Evidence files (folder `tests/evidence/offers/`):
- JUnit: `junit-OffersAcceptanceTest-2026-11-15.txt` (and `.xml`)
- Engine: `engine-results-2026-11-15.txt`, `engine-trace-2026-11-15.json`, `engine-manual-30h-2026-11-15.json`, `engine-manual-cap-2026-11-15.json`

## Offers and allowance owned by Areej

### O-01

Requirement: OFF-01. Technique: White-box transition. Type: Functional.

Preconditions and steps: Create SUBMITTED application and attempt issue using valid form. Repeat with INTERVIEW application.

Expected result: SUBMITTED refused; INTERVIEW allowed and becomes OFFERED.

Automation and execution: JUnit OffersAcceptanceTest O01 (INTERVIEW, positive) and O02 (SUBMITTED, negative).

Execution results:

- JUnit O01 (INTERVIEW), 15 Nov 2026: offer created as PENDING, application OFFERED. Status: **Pass**. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt
- JUnit O02 (SUBMITTED), 15 Nov 2026: no error thrown, offer created. Status: **Fail**. Defect: #22. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt

### O-02

Requirement: OFF-01. Technique: BVA. Type: Functional.

Preconditions and steps: Use INTERVIEW app; offer posting allowance exactly, then one cent above it; dates exactly posting bounds then one day outside.

Expected result: Exact allowed boundaries pass; excess allowance and out-of-range dates refused.

Automation and execution: JUnit O03 (allowance one cent above the posting). The other datasets are manual and not run yet.

Execution results:

- Dataset allowance = posting + 0.01 (JUnit O03), 15 Nov 2026: refused with a business-rule error. Status: **Pass**. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt
- Dataset allowance exactly equal to posting: **Not run**.
- Dataset dates exactly on posting bounds: **Not run**.
- Dataset dates one day outside posting bounds: **Not run**.

### O-03

Requirement: OFF-02. Technique: White-box time boundary. Type: Functional.

Preconditions and steps: Call responseDeadline for issued=12 Nov 2026 14:30 and default 72 hours.

Expected result: 15 Nov 2026 14:30, preserving time.

Automation and execution: JUnit O04.

Execution results:

- JUnit O04, 15 Nov 2026: actual deadline 2026-11-15T00:00 (expected 2026-11-15T14:30). Status: **Fail**. Defect: #18. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt

### O-04

Requirement: OFF-02; ADM-01. Technique: Settings partition. Type: Functional.

Preconditions and steps: Set response hours to 73; issue at 14:30.

Expected result: Deadline is issue time +73 hours, with no truncation to full days.

Automation and execution: Manual; not run yet.

Execution results:

- **Not run.**

### O-05

Requirement: OFF-02. Technique: White-box negative path. Type: Functional.

Preconditions and steps: Construct pending offer whose respondBy is one hour before clock now. Immediately call accept without invoking expiry scheduler.

Expected result: Acceptance refused even before scheduler runs.

Automation and execution: JUnit O05.

Execution results:

- JUnit O05, 15 Nov 2026: late acceptance succeeded, no error thrown. Status: **Fail**. Defect: #23. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt

### O-06

Requirement: OFF-03. Technique: White-box cascade. Type: Functional.

Preconditions and steps: One student has pending offered app and another SUBMITTED application. Accept offered app.

Expected result: Accepted app ACCEPTED; other active app WITHDRAWN.

Automation and execution: JUnit O06.

Execution results:

- JUnit O06, 15 Nov 2026: other application stayed SUBMITTED (expected WITHDRAWN). Status: **Fail**. Defect: #24. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt

### O-07

Requirement: OFF-03. Technique: White-box cascade. Type: Functional.

Preconditions and steps: One-seat posting has offered app plus another student SUBMITTED app. Accept first offer.

Expected result: Posting FILLED; remaining active app REJECTED and notification generated.

Automation and execution: JUnit O07 (posting status and the other application's state). The notification check is manual and not run yet.

Execution results:

- JUnit O07, 15 Nov 2026: posting became FILLED (correct), but the other student's application stayed SUBMITTED (expected REJECTED). Status: **Fail**. Defect: #25. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt
- Notification check: **Not run**.

### O-08

Requirement: OFF-04. Technique: Concurrency. Type: Reliability.

Preconditions and steps: Prepare two pending offers for a single free seat. Use a barrier to submit both accept requests simultaneously; repeat with reset fixtures.

Expected result: Exactly one succeeds, seatsTaken=1, no negative seatsLeft, loser receives explanation.

Automation and execution: Manual concurrency check; not run yet.

Execution results:

- **Not run.**

### O-09

Requirement: OFF-05. Technique: Black-box BVA. Type: Functional.

Preconditions and steps: Use 1 Jan through 30 Jan inclusive, monthly 3000, no late documents; compare 29 and 30 weekly hours.

Expected result: 29h total 3000; 30h total 3300.

Automation and execution: Engine API tests/acceptance/test_engine.py E12 and E13.

Execution results:

- Dataset 29 h (Engine E12), 15 Nov 2026: total 3000.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- Dataset 30 h (Engine E13), 15 Nov 2026: total 3000.00 (expected 3300); no transport line in the statement. Status: **Fail**. Defect: #26. Evidence: engine-results-2026-11-15.txt, engine-manual-30h-2026-11-15.json

### O-10

Requirement: OFF-05. Technique: Black-box cap boundary. Type: Functional.

Preconditions and steps: Same 30-day duration, monthly 4900 and weeklyHours=31.

Expected result: Monthly total capped at 5000; lines show -200 cap adjustment.

Automation and execution: Engine API test_engine.py E16.

Execution results:

- Engine E16, 15 Nov 2026: total 5200.00 (expected 5000); no cap adjustment line. Status: **Fail**. Defect: #27. Evidence: engine-results-2026-11-15.txt, engine-manual-cap-2026-11-15.json

### O-11

Requirement: OFF-05. Technique: Black-box started-week BVA. Type: Functional.

Preconditions and steps: Use monthly 3000, 29h, duration 30 days; lateDays=0,1,8,100.

Expected result: Totals 3000,2850,2700,2400; penalty never exceeds 20%.

Automation and execution: Engine API test_engine.py E12, E14, E15 and E17.

Execution results:

- Dataset lateDays 0 (Engine E12), 15 Nov 2026: total 3000.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- Dataset lateDays 1 (Engine E14), 15 Nov 2026: total 2850.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- Dataset lateDays 8 (Engine E15), 15 Nov 2026: total 2700.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- Dataset lateDays 100 (Engine E17), 15 Nov 2026: total 2400.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt

### O-12

Requirement: OFF-05. Technique: Black-box calendar boundary. Type: Functional.

Preconditions and steps: Use 1 Jan to 31 Jan inclusive and 31 Jan to 28 Feb inclusive in a non-leap year; compare independent calendar-month calculation with all statement lines.

Expected result: Whole-calendar-month semantics follow OpenAPI, not every month treated as 30 days.

Automation and execution: Manual; not run yet.

Execution results:

- **Not run.**

### O-13

Requirement: OFF-06; USR-05. Technique: Role decision table. Type: Security.

Preconditions and steps: Accept an offer. Open its allowance statement as owner, Coordinator, unrelated Student and unrelated Recruiter.

Expected result: Full lines only for authorized owner and Coordinator; unrelated users denied without disclosure.

Automation and execution: Manual role check; not run yet. JUnit O08 covers a related rule (an unrelated student cannot accept an offer), not the statement view.

Execution results:

- **Not run.** Related check: JUnit O08 (an unrelated student cannot accept someone else's offer), 15 Nov 2026, **Pass**. It does not cover viewing the statement.

### O-14

Requirement: OFF-07. Technique: Integration. Type: Functional.

Preconditions and steps: Accepted placement; record agreement on start date then start+8 days as Coordinator and verify recalculation.

Expected result: No penalty on start; two started late weeks at +8 days; updated statement saved.

Automation and execution: Manual; not run yet.

Execution results:

- **Not run.**

### O-15

Requirement: OFF-05; PERF-02. Technique: Timing. Type: Performance.

Preconditions and steps: After 5 warmups send 100 /allowance calls; also measure eligibility/match/rank 200 candidates per PERF-02. Record hardware.

Expected result: Allowance has no explicit numeric SLA here; report distribution only. Eligibility/match p95<=200ms; rank p95<=500ms.

Automation and execution: Manual timing measurement; no script in this repository yet; not run.

Execution results:

- **Not run.**