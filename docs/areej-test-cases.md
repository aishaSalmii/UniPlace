# Offers and allowance test design

Reviewed and executed by Areeg AlGhazali. The case designs and the test code started as AI-assisted drafts; the execution results under each case are from my own runs. Cases or datasets that have not been executed yet are marked **Not run**.

Environment for all runs: Windows, OpenJDK 21.0.13 (Temurin), Maven wrapper, Python 3.12.5, vendor Portal source and vendor Engine 1.0.0 unchanged. Branch `qa/offers-allowance`. Phase 1 runs: 15 Nov 2026 (evidence commit a781aa4). Phase 2 runs: 26–27 Nov 2026 (evidence commits 8a0830d, 11a7ba1, c430447, bd0bc99, d58b27e). Manual runs used the Portal after `start.cmd --jar --reset` (demo password for all accounts `Uniplace!2026`).

Evidence files (folder `tests/evidence/offers/`):
- JUnit: `junit-OffersAcceptanceTest-2026-11-15.txt` (and `.xml`), `junit-OffersDeadlineBoundaryTest-2026-11-26.txt`, `junit-OffersAgreementTest-2026-11-27.txt`, `junit-AiAssistantAcceptanceTest-2026-11-27.txt`
- Engine: `engine-results-2026-11-15.txt`, `engine-trace-2026-11-15.json`, `engine-manual-30h-2026-11-15.json`, `engine-manual-cap-2026-11-15.json`, `allowance-boundaries-20261126-190257.json`
- Concurrency: `last-seat-race-20261126-184919.json`, `last-seat-race-20261126-185112.json`, `last-seat-race-20261126-185213.json`
- AI Assistant (fake provider, not a real model): `ai-fake-provider-capture.jsonl`, `ai-fake-provider-slow.jsonl`, `ai-fake-provider-invalid.jsonl`
- Screenshots of the manual runs are attached to the GitHub issues and comments named in each result.

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

Automation and execution: JUnit O03 (allowance one cent above the posting). The other datasets were run manually on 26 Nov 2026 as recruiter.lusail on application 113 (Noor Al-Emadi, INTERVIEW) for posting 10 Health Informatics Intern (allowance QAR 3,800.00, dates 29/12/2026 to 28/03/2027).

Execution results:

- Dataset allowance = posting + 0.01 (JUnit O03), 15 Nov 2026: refused with a business-rule error. Status: **Pass**. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt
- Dataset allowance 3800.01 (manual), 26 Nov 2026: refused, "Invalid offer: monthlyAllowance > posting.monthlyAllowance (3800.00)". Status: **Pass**.
- Dataset start 28/12/2026, one day before the posting start (manual), 26 Nov 2026: refused, "Invalid offer: startDate/endDate outside [2026-12-29, 2027-03-28]". Status: **Pass**.
- Dataset end 29/03/2027, one day after the posting end (manual), 26 Nov 2026: refused with the same message. Status: **Pass**.
- Dataset allowance exactly 3800.00 and dates exactly 29/12/2026 to 28/03/2027 (manual), 26 Nov 2026: "Offer issued", offer PENDING, application OFFERED. Status: **Pass**.

### O-03

Requirement: OFF-02. Technique: White-box time boundary. Type: Functional.

Preconditions and steps: Call responseDeadline for issued=12 Nov 2026 14:30 and default 72 hours.

Expected result: 15 Nov 2026 14:30, preserving time.

Automation and execution: JUnit O04. Manual confirmation on the running Portal, 26 Nov 2026.

Execution results:

- JUnit O04, 15 Nov 2026: actual deadline 2026-11-15T00:00 (expected 2026-11-15T14:30). Status: **Fail**. Defect: #18. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt
- Manual, 26 Nov 2026: offer to application 113 issued 26/11/2026 18:10 with 72 hours; "Answer by" shows 29/11/2026 00:00 (expected 29/11/2026 18:10). Status: **Fail**. Defect: #18.

### O-04

Requirement: OFF-02; ADM-01. Technique: Settings partition. Type: Functional.

Preconditions and steps: Set response hours to 73; issue at 14:30. Also change the setting on the running Portal and issue a new offer without restarting.

Expected result: Deadline is issue time +73 hours, with no truncation to full days. A changed setting applies to the next offer immediately.

Automation and execution: JUnit OffersDeadlineBoundaryTest D06 (96 hours) and D07 (73 hours), fixed clock. Manual check with 96 hours on 26 Nov 2026.

Execution results:

- JUnit D06 (96 hours, issued 2026-11-12T00:00), 26 Nov 2026: deadline 2026-11-16T00:00. Status: **Pass**. Evidence: junit-OffersDeadlineBoundaryTest-2026-11-26.txt
- JUnit D07 (73 hours, issued 2026-11-12T00:00), 26 Nov 2026: deadline 2026-11-15T00:00 (expected 2026-11-15T01:00). Status: **Fail**. Defect: #18. Evidence: junit-OffersDeadlineBoundaryTest-2026-11-26.txt
- Manual (Settings "Offer response time" = 96, no restart), 26 Nov 2026: offer to application 104 (Ibrahim Al-Mannai) issued 26/11/2026 18:17, "Answer by" 30/11/2026 00:00. The new setting was applied at once (date moved to 30/11), but the time of day was lost (expected 30/11/2026 18:17). Status: **Pass** for ADM-01, **Fail** for the time. Defect: #18. Setting restored to 72 afterwards.

### O-05

Requirement: OFF-02. Technique: White-box negative path. Type: Functional.

Preconditions and steps: Construct pending offer whose respondBy is one hour before clock now. Immediately call accept without invoking expiry scheduler. Phase 2 adds boundary datasets at one minute before, exactly at and one minute after the deadline, and the expiry job.

Expected result: Acceptance refused even before scheduler runs. Acceptance allowed up to and including the deadline. An overdue offer becomes EXPIRED and its application WITHDRAWN.

Automation and execution: JUnit O05; OffersDeadlineBoundaryTest D01–D05 (fixed clock 2026-11-20T14:30, Asia/Qatar). Manual expiry check on 26 Nov 2026.

Execution results:

- JUnit O05, 15 Nov 2026: late acceptance succeeded, no error thrown. Status: **Fail**. Defect: #23. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt
- JUnit D01 (accept 1 minute before the deadline), 26 Nov 2026: ACCEPTED. Status: **Pass**. Evidence: junit-OffersDeadlineBoundaryTest-2026-11-26.txt
- JUnit D02 (accept exactly at the deadline), 26 Nov 2026: ACCEPTED. Status: **Pass**.
- JUnit D03 (accept 1 minute after the deadline), 26 Nov 2026: accepted, no error thrown. Status: **Fail**. Defect: #23.
- JUnit D04 (expiry job 1 minute after the deadline), 26 Nov 2026: 1 offer expired; offer EXPIRED, application WITHDRAWN. Status: **Pass**.
- JUnit D05 (expiry job exactly at the deadline), 26 Nov 2026: offer stays PENDING. Status: **Pass**.
- Manual expiry (Settings "Offer response time" = 1 hour), 26 Nov 2026: offer to application 84 (Tamim Al-Hajri) issued 26/11/2026 18:20 showed "Answer by" 26/11/2026 00:00, which is before the issue time; the expiry job then set the offer to Expired and the application to Withdrawn. Status: **Pass** for expiry. The deadline before the issue time is another effect of #18. Setting restored to 72 afterwards.

### O-06

Requirement: OFF-03. Technique: White-box cascade. Type: Functional.

Preconditions and steps: One student has pending offered app and another SUBMITTED application. Accept offered app.

Expected result: Accepted app ACCEPTED; other active app WITHDRAWN.

Automation and execution: JUnit O06.

Execution results:

- JUnit O06, 15 Nov 2026: other application stayed SUBMITTED (expected WITHDRAWN). Status: **Fail**. Defect: #24. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt

### O-07

Requirement: OFF-03; NOT-01. Technique: White-box cascade. Type: Functional.

Preconditions and steps: One-seat posting has offered app plus another student SUBMITTED app. Accept first offer.

Expected result: Posting FILLED; remaining active app REJECTED and notification generated.

Automation and execution: JUnit O07 (posting status and the other application's state). Notification check done manually in the Outbox on 27 Nov 2026 after the last seat of posting 8 (Sustainability Reporting Intern) was taken.

Execution results:

- JUnit O07, 15 Nov 2026: posting became FILLED (correct), but the other student's application stayed SUBMITTED (expected REJECTED). Status: **Fail**. Defect: #25. Evidence: junit-OffersAcceptanceTest-2026-11-15.txt
- Notification check (Outbox, coordinator), 27 Nov 2026: posting 8 is Filled, but there is no rejection or "posting filled" notification for student.ruqaya (application 9). Status: **Fail**. Defect: #25 (comment with screenshots).

### O-08

Requirement: OFF-04. Technique: Concurrency. Type: Reliability.

Preconditions and steps: Prepare two pending offers for a single free seat. Use a barrier to submit both accept requests simultaneously; repeat with reset fixtures.

Expected result: Exactly one succeeds, seatsTaken=1, no negative seatsLeft, loser receives explanation.

Automation and execution: Black-box script `tests/acceptance/test_last_seat_race.py` (student.ibrahim, application 7, and student.mubarak, application 8, both on posting 8 with 1 seat), run 3 times after `start.cmd --jar --reset`.

Execution results:

- Run 1, 26 Nov 2026 18:49: both students got "Congratulations! You accepted the offer."; both applications Accepted; posting Filled, 0 seats left. Status: **Fail**. Defect: #38. Evidence: last-seat-race-20261126-184919.json
- Run 2, 26 Nov 2026 18:51: same result. Status: **Fail**. Defect: #38. Evidence: last-seat-race-20261126-185112.json
- Run 3, 26 Nov 2026 18:52: same result. Status: **Fail**. Defect: #38. Evidence: last-seat-race-20261126-185213.json
- Seats never below zero: held in all 3 runs (0 left, not -1), but 2 students hold 1 seat.

### O-09

Requirement: OFF-05. Technique: Black-box BVA. Type: Functional.

Preconditions and steps: Use 1 Jan through 30 Jan inclusive, monthly 3000, no late documents; compare 29 and 30 weekly hours.

Expected result: 29h total 3000; 30h total 3300.

Automation and execution: Engine API tests/acceptance/test_engine.py E12 and E13; tests/acceptance/test_allowance_boundaries.py A01–A03 (independent oracle).

Execution results:

- Dataset 29 h (Engine E12), 15 Nov 2026: total 3000.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- Dataset 30 h (Engine E13), 15 Nov 2026: total 3000.00 (expected 3300); no transport line in the statement. Status: **Fail**. Defect: #26. Evidence: engine-results-2026-11-15.txt, engine-manual-30h-2026-11-15.json
- A01 (29 h), 26 Nov 2026: 3000.00. Status: **Pass**. Evidence: allowance-boundaries-20261126-190257.json
- A02 (30 h), 26 Nov 2026: 3000.00, no transport line (expected 3300.00). Status: **Fail**. Defect: #26.
- A03 (31 h), 26 Nov 2026: 3300.00. Status: **Pass**.

### O-10

Requirement: OFF-05. Technique: Black-box cap boundary. Type: Functional.

Preconditions and steps: Same 30-day duration, monthly 4900 and weeklyHours=31.

Expected result: Monthly total capped at 5000; lines show -200 cap adjustment.

Automation and execution: Engine API test_engine.py E16; test_allowance_boundaries.py A04–A08.

Execution results:

- Engine E16, 15 Nov 2026: total 5200.00 (expected 5000); no cap adjustment line. Status: **Fail**. Defect: #27. Evidence: engine-results-2026-11-15.txt, engine-manual-cap-2026-11-15.json
- A04 (4600 + 300 = 4900, below cap), 26 Nov 2026: 4900.00. Status: **Pass**. Evidence: allowance-boundaries-20261126-190257.json
- A05 (4700 + 300 = 5000, equal to cap), 26 Nov 2026: 5000.00. Status: **Pass**.
- A06 (4900 + 300 = 5200), 26 Nov 2026: 5200.00, no cap line (expected 5000.00). Status: **Fail**. Defect: #27.
- A07 (5000 without transport), 26 Nov 2026: 5000.00. Status: **Pass**.
- A08 (5000 + 300 = 5300), 26 Nov 2026: 5300.00, no cap line (expected 5000.00). Status: **Fail**. Defect: #27.

### O-11

Requirement: OFF-05. Technique: Black-box started-week BVA. Type: Functional.

Preconditions and steps: Use monthly 3000, 29h, duration 30 days; lateDays=0,1,8,100.

Expected result: Totals 3000,2850,2700,2400; penalty never exceeds 20%.

Automation and execution: Engine API test_engine.py E12, E14, E15 and E17; test_allowance_boundaries.py A17–A23 (adds 7, 28 and 29 late days).

Execution results:

- Dataset lateDays 0 (Engine E12), 15 Nov 2026: total 3000.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- Dataset lateDays 1 (Engine E14), 15 Nov 2026: total 2850.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- Dataset lateDays 8 (Engine E15), 15 Nov 2026: total 2700.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- Dataset lateDays 100 (Engine E17), 15 Nov 2026: total 2400.00. Status: **Pass**. Evidence: engine-results-2026-11-15.txt
- A17–A23 (0, 1, 7, 8, 28, 29 and 100 late days), 26 Nov 2026: 3000.00, 2850.00, 2850.00, 2700.00, 2400.00, 2400.00, 2400.00. Status: **Pass** (7 of 7). Evidence: allowance-boundaries-20261126-190257.json

### O-12

Requirement: OFF-05. Technique: Black-box calendar boundary. Type: Functional.

Preconditions and steps: Use 1 Jan to 31 Jan inclusive and 31 Jan to 28 Feb inclusive in a non-leap year; compare independent calendar-month calculation with all statement lines.

Expected result: Whole-calendar-month semantics follow OpenAPI, not every month treated as 30 days.

Automation and execution: test_allowance_boundaries.py A09–A16 and A28 (monthly 3000, oracle implements the OpenAPI month rule).

Execution results:

- A09 (1 Jan–31 Jan), A10 (1 Jan–15 Feb), A12 (15 Jan–14 Apr), A13 (1–10 Jan), A14 (1–28 Feb 2027), A15 (1–29 Feb 2028), 26 Nov 2026: 3000.00, 4500.00, 9000.00, 1000.00, 3000.00, 3000.00. Status: **Pass** (6 of 6). Evidence: allowance-boundaries-20261126-190257.json
- A11 (31 Jan–28 Feb, 1 month), 26 Nov 2026: 3100.00, "1.03 months" (expected 3000.00). Status: **Fail**. Defect: #39.
- A16 (30 Jan–1 Mar, 1 month + 1 day), 26 Nov 2026: 3200.00 (expected 3100.00). Status: **Fail**. Defect: #39.
- A28 (31 Mar–30 Apr, 1 month), 26 Nov 2026: 3100.00 (expected 3000.00). Status: **Fail**. Defect: #39.

### O-13

Requirement: OFF-06; USR-05. Technique: Role decision table. Type: Security.

Preconditions and steps: Accept an offer. Open its allowance statement as owner, Coordinator, unrelated Student and unrelated Recruiter.

Expected result: Full lines only for authorized owner and Coordinator; unrelated users denied without disclosure.

Automation and execution: Manual role check on 27 Nov 2026, application 7 (Ibrahim Al-Kuwari, Sustainability Reporting Intern, Doha Energy Partners, Accepted). JUnit O08 covers a related rule (an unrelated student cannot accept an offer).

Execution results:

- Related check: JUnit O08 (an unrelated student cannot accept someone else's offer), 15 Nov 2026, **Pass**.
- Owner student.ibrahim, 27 Nov 2026: statement shown (Base allowance 3 months x 3200.00 = 9600.00, Total 9600.00). Status: **Pass**.
- Coordinator, 27 Nov 2026: statement shown with the same lines. Status: **Pass**.
- The posting's recruiter (recruiter.dohaenergy), 27 Nov 2026: offer shown, statement not shown (OFF-06 lists only Student and Coordinator). Status: **Pass**.
- Unrelated student (student.amal), 27 Nov 2026: page opened and the full statement shown (expected access denied). Status: **Fail**. Defect: #35 (comment with screenshots).
- Unrelated recruiter (recruiter.lusail), 27 Nov 2026: page opened with the student's name, GPA and offer; statement not shown (expected access denied, USR-05). Status: **Fail**. Defect: same root cause as #35 (comment added).

### O-14

Requirement: OFF-07. Technique: Integration. Type: Functional.

Preconditions and steps: Accepted placement; record agreement on start date then start+8 days as Coordinator and verify recalculation.

Expected result: No penalty on start; two started late weeks at +8 days; updated statement saved.

Automation and execution: JUnit OffersAgreementTest G01–G08 (fixed clock 2027-02-10, placement 2027-01-01 to 2027-03-31, Engine mocked, request captured). Manual UI check on application 7 on 27 Nov 2026.

Execution results:

- G01 (no agreement recorded), 27 Nov 2026: lateDays 0 sent. Status: **Pass**. Evidence: junit-OffersAgreementTest-2026-11-27.txt
- G02 (agreement on the start date), G03 (before the start date), 27 Nov 2026: lateDays 0. Status: **Pass**.
- G04 (+1 day), G05 (+7 days), G06 (+8 days), 27 Nov 2026: lateDays 1, 7, 8 sent. Status: **Pass**.
- G07 (date and new statement stored, "Late documents penalty" line), 27 Nov 2026: stored. Status: **Pass**.
- G08 (future date), 27 Nov 2026: refused. Status: **Pass**.
- Manual, 27 Nov 2026: before any date the statement had no penalty line; recording 27/11/2026 gave "Agreement date recorded and allowance recalculated" and "Placement agreement received on 27/11/2026" (no penalty, the placement starts 22/12/2026); a future date was refused with "Enter a date that is not in the future." Status: **Pass**.

### O-15

Requirement: OFF-05; PERF-02. Technique: Timing. Type: Performance.

Preconditions and steps: After 5 warmups send 100 /allowance calls; also measure eligibility/match/rank 200 candidates per PERF-02. Record hardware.

Expected result: Allowance has no explicit numeric SLA here; report distribution only. Eligibility/match p95<=200ms; rank p95<=500ms.

Automation and execution: Manual timing measurement; no script in this repository yet; not run.

Execution results:

- **Not run.**

### O-16

Requirement: OFF-05 (OpenAPI rounding rule). Technique: Black-box BVA on rounding. Type: Functional.

Preconditions and steps: Send amounts whose exact result ends in a half cent or a repeating fraction; compare with an independent calculation using ROUND_HALF_UP to 2 decimals.

Expected result: Totals rounded half up; the total matches the sum of the statement lines.

Automation and execution: test_allowance_boundaries.py A24–A27.

Execution results:

- A25 (1000 x 1/30 = 33.333...), 26 Nov 2026: 33.33. Status: **Pass**. Evidence: allowance-boundaries-20261126-190257.json
- A26 (3333.33 with 5% penalty = 3166.6635), 26 Nov 2026: 3166.66. Status: **Pass**.
- A24 (1000.05 x 3/30 = 100.005), 26 Nov 2026: total 100.00 while the base line shows 100.01 (expected 100.01). Status: **Fail**. Defect: #40.
- A27 (1000.25 x 3/30 = 100.025), 26 Nov 2026: total 100.02 while the base line shows 100.03 (expected 100.03). Status: **Fail**. Defect: #40.

### O-17

Requirement: ADM-01; OFF-05. Technique: White-box integration (request capture). Type: Functional.

Preconditions and steps: Change transport supplement, penalty rate, penalty cap and monthly cap in Settings; recalculate an accepted placement; capture the request the Portal sends to the Engine.

Expected result: Every changed setting is sent to the Engine immediately.

Automation and execution: JUnit OffersAgreementTest G09 and G10.

Execution results:

- G09 (transport 400, penalty rate 0.10, penalty cap 0.30), 27 Nov 2026: all three sent. Status: **Pass**. Evidence: junit-OffersAgreementTest-2026-11-27.txt
- G10 (monthly cap 4000), 27 Nov 2026: "monthlyCap is not sent to the Engine". Status: **Fail**. Defect: #47.

### O-18

Requirement: NOT-01; NOT-02; NOT-03. Technique: Outbox observation. Type: Functional.

Preconditions and steps: Issue, decline, accept and let expire offers; open the Outbox as Coordinator and the Notifications page as a student.

Expected result: One notification per event to the right recipient; the body names the posting and the company; a student sees only their own notifications.

Automation and execution: Manual, 27 Nov 2026.

Execution results:

- Offer received (student.noor, Health Informatics Intern, Lusail Health Group): recorded, names posting and company. Status: **Pass**.
- Offer declined (recruiter.pearl, Amal Al-Kuwari, Software Support Intern, Pearl Logistics): recorded. Status: **Pass**.
- Offer expired (student.tamim and recruiter.lusail, Health Informatics Intern): recorded for both. Status: **Pass**.
- Offer accepted (posting 8 after the race in O-08): "Offer accepted" recorded for both students and twice for recruiter.dohaenergy, for one seat. Status: **Fail**. Defect: #38 (comment).
- Posting filled (posting 8): no notification to the remaining applicant student.ruqaya. Status: **Fail**. Defect: #25 (comment).
- NOT-03, student.noor's Notifications page: only her own 4 notifications. Status: **Pass**.

### O-19

Requirement: NOT-04; OFF-02. Technique: Outbox observation and specification review. Type: Functional.

Preconditions and steps: Keep a pending offer inside the reminder window and watch the Outbox over at least 20 minutes. Compare NOT-04 with OFF-02 and ADM-01.

Expected result: Exactly one reminder per offer. The reminder timing is consistent with the configured response window.

Automation and execution: Manual, 27 Nov 2026; specification review.

Execution results:

- Outbox, 27 Nov 2026: "Reminder: answer your offer for Sustainability Reporting Intern" for student.ibrahim recorded at 19:13 and again at 19:23. Status: **Fail**. Defect: #48.
- Specification review: NOT-04 assumes a 48-hour window, OFF-02 and ADM-01 define 72 hours (configurable). Status: **Fail** (specification defect). Defect: #49.

### O-20

Requirement: AI-03; AI-10; AI-11. Technique: White-box prompt inspection and black-box request capture. Type: Security and functional.

Preconditions and steps: Build the feedback and interview-question prompts for a known student; check what they contain. Then point the Portal at the fake provider (`tests/acceptance/fake_ai_provider.py capture`) and record the request it actually sends.

Expected result: Only posting title, skills and letter text; no name, student number or GPA; no internal rules or data; explicit criteria, scale, item counts and format with consistent examples.

Automation and execution: JUnit AiAssistantAcceptanceTest I01–I06; fake provider, 27 Nov 2026 (fake provider, not a real model).

Execution results:

- I01, I02 (no personal data in the feedback and questions prompts), 27 Nov 2026: name, student number and GPA present. Status: **Fail**. Defect: #51. Evidence: junit-AiAssistantAcceptanceTest-2026-11-27.txt
- Fake provider, 27 Nov 2026: all 3 requests (postings 1, 13, 5) contained "Student: Noor Al-Emadi (20231004), GPA 3.10". Status: **Fail**. Defect: #51. Evidence: ai-fake-provider-capture.jsonl, -slow.jsonl, -invalid.jsonl
- I03 (no internal rules), 27 Nov 2026: prompt contains "pays QAR 3,000" and "recruiters reject applications that score below 60"; also in the captured request. Status: **Fail**. Defect: #52.
- I04, I05 (consistent examples and counts), 27 Nov 2026: example score 8.5 and "exactly three strengths" (AI-01 allows 1 to 5); also in the captured request. Status: **Fail**. Defect: #53.
- I06 (questions prompt states 5 questions and JSON format), 27 Nov 2026: neither stated, the parser requires both. Status: **Fail**. Defect: #54.

### O-21

Requirement: AI-06; AI-01. Technique: White-box parser BVA and black-box invalid answer. Type: Functional.

Preconditions and steps: Give the parser answers with 6 items, 0 items and score 150, and one valid answer. Then make the fake provider return score 150 and 6 strengths (`invalid` mode) and request feedback on the website.

Expected result: Invalid answers are treated as errors and no partial result is shown; a valid answer is accepted.

Automation and execution: JUnit I07–I10; fake provider, 27 Nov 2026 (fake provider, not a real model).

Execution results:

- I07 (6 items), I08 (empty list), I09 (score 150), 27 Nov 2026: all accepted by the parser. Status: **Fail**. Defect: #55. Evidence: junit-AiAssistantAcceptanceTest-2026-11-27.txt
- I10 (valid answer), 27 Nov 2026: accepted. Status: **Pass**.
- Fake provider `invalid`, posting 5, student.noor, 27 Nov 2026: page showed "Letter feedback: 150 / 100" and Strengths s1–s6. Status: **Fail**. Defect: #55 (comment). Evidence: ai-fake-provider-invalid.jsonl

### O-22

Requirement: AI-07. Technique: White-box and black-box label check. Type: Functional.

Preconditions and steps: Produce a simulated result and a real-provider result with model `gpt-4.1-mini` (JUnit) and `fake-model-1` (fake provider); read the label on the result.

Expected result: Simulated results say simulated; real results name the configured model.

Automation and execution: JUnit I11, I12; fake provider, 27 Nov 2026.

Execution results:

- I11 (simulated label), 27 Nov 2026: labelled simulated. Status: **Pass**. Evidence: junit-AiAssistantAcceptanceTest-2026-11-27.txt
- I12 (configured gpt-4.1-mini), 27 Nov 2026: label "Model gpt-4o-mini". Status: **Fail**. Defect: #56.
- Fake provider, 27 Nov 2026: request sent with model fake-model-1, page labelled "Model gpt-4o-mini" (3 of 3). Status: **Fail**. Defect: #56 (comment). Evidence: ai-fake-provider-capture.jsonl

### O-23

Requirement: AI-05. Technique: Black-box fault injection (slow provider). Type: Reliability.

Preconditions and steps: Run the fake provider in `slow` mode (answers after 30 s); request feedback as student.noor on posting 13 and time the page.

Expected result: After about 10 seconds the page shows "Assistant unavailable" and the student can still submit.

Automation and execution: Fake provider, 27 Nov 2026 (fake provider, not a real model).

Execution results:

- 27 Nov 2026: nothing at 10 s; the page waited for the provider (30.0 s in the provider log, about 25 s on a hand stopwatch) and then showed the result. Status: **Fail**. Defect: #57. Evidence: ai-fake-provider-slow.jsonl

### O-24

Requirement: AI-08. Technique: White-box cache check. Type: Functional.

Preconditions and steps: Request feedback twice with the same letter, then with a changed letter, for the same student and posting within 10 minutes.

Expected result: Same letter returns the cached result; a changed letter is evaluated again.

Automation and execution: JUnit I13, I14.

Execution results:

- I13 (same letter), 27 Nov 2026: provider called once, identical result. Status: **Pass**. Evidence: junit-AiAssistantAcceptanceTest-2026-11-27.txt
- I14 (changed letter), 27 Nov 2026: old result served from the cache. Status: **Fail**. Defect: #43 (Lana's issue; comment with root cause).

### O-25

Requirement: AI-04; AI-09; AI-01. Technique: Experiment with a real model (ai-eval, 20 letters x 5 runs). Type: Functional and reliability.

Preconditions and steps: Pilot 2–3 letters, then run all 20 letters of `ai-eval/letters.json` 5 times each against a real model; record provider, model, prompt version, settings and timestamps; compare scores with the reference bands, the injection letters L17–L18 and the spread between runs.

Expected result: Injection letters scored on content only (AI-04); scores of the same letter differ by at most 10 points (AI-09).

Automation and execution: Not run: no real API key was available. Requested from the team.

Execution results:

- **Not run.** Related finding from code review: the Portal sends temperature 1.3 (seen in every captured request), which makes AI-09 harder to meet.