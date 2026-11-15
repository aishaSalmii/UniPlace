# Offers and allowance test design

Draft for Areej to review and execute. All cases start as Not run in her own environment. For each dataset record actual result, status, date, environment, evidence and issue link. Automation references describe prior preparation and map to starter code; they are not personal execution claims.

## Offers and allowance owned by Areej

### O-01

Requirement: OFF-01. Technique: White-box transition. Type: Functional.

Preconditions and steps: Create SUBMITTED application and attempt issue using valid form. Repeat with INTERVIEW application.

Expected result: SUBMITTED refused; INTERVIEW allowed and becomes OFFERED.

Automation and execution: OFF02 checks forbidden transition; positive pending.

### O-02

Requirement: OFF-01. Technique: BVA. Type: Functional.

Preconditions and steps: Use INTERVIEW app; offer posting allowance exactly, then one cent above it; dates exactly posting bounds then one day outside.

Expected result: Exact allowed boundaries pass; excess allowance and out-of-range dates refused.

Automation and execution: Manual pending.

### O-03

Requirement: OFF-02. Technique: White-box time boundary. Type: Functional.

Preconditions and steps: Call responseDeadline for issued=12 Nov 2026 14:30 and default 72 hours.

Expected result: 15 Nov 2026 14:30, preserving time.

Automation and execution: OFF01.

### O-04

Requirement: OFF-02; ADM-01. Technique: Settings partition. Type: Functional.

Preconditions and steps: Set response hours to 73; issue at 14:30.

Expected result: Deadline is issue time +73 hours, with no truncation to full days.

Automation and execution: Manual pending.

### O-05

Requirement: OFF-02. Technique: White-box negative path. Type: Functional.

Preconditions and steps: Construct pending offer whose respondBy is one hour before clock now. Immediately call accept without invoking expiry scheduler.

Expected result: Acceptance refused even before scheduler runs.

Automation and execution: OFF03.

### O-06

Requirement: OFF-03. Technique: White-box cascade. Type: Functional.

Preconditions and steps: One student has pending offered app and another SUBMITTED application. Accept offered app.

Expected result: Accepted app ACCEPTED; other active app WITHDRAWN.

Automation and execution: OFF04.

### O-07

Requirement: OFF-03. Technique: White-box cascade. Type: Functional.

Preconditions and steps: One-seat posting has offered app plus another student SUBMITTED app. Accept first offer.

Expected result: Posting FILLED; remaining active app REJECTED and notification generated.

Automation and execution: OFF05 checks state; notification pending.

### O-08

Requirement: OFF-04. Technique: Concurrency. Type: Reliability.

Preconditions and steps: Prepare two pending offers for a single free seat. Use a barrier to submit both accept requests simultaneously; repeat with reset fixtures.

Expected result: Exactly one succeeds, seatsTaken=1, no negative seatsLeft, loser receives explanation.

Automation and execution: Manual pending.

### O-09

Requirement: OFF-05. Technique: Black-box BVA. Type: Functional.

Preconditions and steps: Use 1 Jan through 30 Jan inclusive, monthly 3000, no late documents; compare 29 and 30 weekly hours.

Expected result: 29h total 3000; 30h total 3300.

Automation and execution: E12 and E13.

### O-10

Requirement: OFF-05. Technique: Black-box cap boundary. Type: Functional.

Preconditions and steps: Same 30-day duration, monthly 4900 and weeklyHours=31.

Expected result: Monthly total capped at 5000; lines show -200 cap adjustment.

Automation and execution: E16.

### O-11

Requirement: OFF-05. Technique: Black-box started-week BVA. Type: Functional.

Preconditions and steps: Use monthly 3000, 29h, duration 30 days; lateDays=0,1,8,100.

Expected result: Totals 3000,2850,2700,2400; penalty never exceeds 20%.

Automation and execution: E12 E14 E15 E17.

### O-12

Requirement: OFF-05. Technique: Black-box calendar boundary. Type: Functional.

Preconditions and steps: Use 1 Jan to 31 Jan inclusive and 31 Jan to 28 Feb inclusive in a non-leap year; compare independent calendar-month calculation with all statement lines.

Expected result: Whole-calendar-month semantics follow OpenAPI, not every month treated as 30 days.

Automation and execution: Manual pending.

### O-13

Requirement: OFF-06; USR-05. Technique: Role decision table. Type: Security.

Preconditions and steps: Accept an offer. Open its allowance statement as owner, Coordinator, unrelated Student and unrelated Recruiter.

Expected result: Full lines only for authorized owner and Coordinator; unrelated users denied without disclosure.

Automation and execution: HTTP02 tests general app disclosure; accepted statement matrix pending.

### O-14

Requirement: OFF-07. Technique: Integration. Type: Functional.

Preconditions and steps: Accepted placement; record agreement on start date then start+8 days as Coordinator and verify recalculation.

Expected result: No penalty on start; two started late weeks at +8 days; updated statement saved.

Automation and execution: Manual pending.

### O-15

Requirement: OFF-05; PERF-02. Technique: Timing. Type: Performance.

Preconditions and steps: After 5 warmups send 100 /allowance calls; also measure eligibility/match/rank 200 candidates per PERF-02. Record hardware.

Expected result: Allowance has no explicit numeric SLA here; report distribution only. Eligibility/match p95<=200ms; rank p95<=500ms.

Automation and execution: benchmark_engine.py provided for SLA endpoints, not executed; allowance timing pending.

