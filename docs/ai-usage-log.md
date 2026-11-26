# AI usage log

One entry per meaningful use.

| Date | Member | Tool and model | Purpose | Prompt summary | Kept | Modified (how) | Rejected (why) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 13–14 November 2026 | Aisha | ChatGPT; model not recorded | Address difficulty organizing the test strategy and GitHub workflow | Explain the setup steps and help structure the initial testing plan | Strategy draft and workflow guidance | Used module-based branch names without personal names | None recorded |
| 15–19 November 2026 | Areeg | Claude (Anthropic) | Step guidance and wording help | Explain steps and output; suggest wording | Starter test code and first test-case draft (AI-generated package); report wording | I reviewed the tests against the requirements, ran all JUnit (O01–O08) and Engine (E01–E18) tests myself, collected the evidence, reproduced every defect on the running system and reported #18, #22–#27 | None |

## Reflection

AI helped clarify unfamiliar planning and GitHub steps.
Command outputs were checked to confirm completed actions.
The proposed strategy is awaiting team review in PR #2.
Areeg: AI explained each step and drafted report text, but every pass/fail result and actual value comes from my own test runs on 15 Nov 2026, and I reproduced each defect before reporting it.