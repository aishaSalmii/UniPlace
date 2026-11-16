# AI usage log

One entry per meaningful use.

| Date | Member | Tool and model | Purpose | Prompt summary | Kept | Modified (how) | Rejected (why) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 13–14 November 2026 | Aisha | ChatGPT; model not recorded | Address difficulty organizing the test strategy and GitHub workflow | Explain the setup steps and help structure the initial testing plan | Strategy draft and workflow guidance | Used module-based branch names without personal names | None recorded |
| 15–16 November 2026 | Areeg | Claude (Anthropic) | Guidance while running the offers and allowance tests; help wording defect reports and results files | Explain guide steps and test output; suggest wording for my findings | Report structure and wording suggestions | I ran the JUnit (O01–O08) and Engine (E01–E18) tests and the manual allowance requests myself, collected the evidence, checked each result against OFF-01 to OFF-05 and reproduced each defect before reporting (#18, #22–#27); switched to Java 21; fixed duplicate #19 | None |

## Reflection

AI helped clarify unfamiliar planning and GitHub steps.
Command outputs were checked to confirm completed actions.
The proposed strategy is awaiting team review in PR #2.
Areeg: AI explained each step and drafted report text, but every pass/fail result and actual value comes from my own test runs on 15 Nov 2026, and I reproduced each defect before reporting it.