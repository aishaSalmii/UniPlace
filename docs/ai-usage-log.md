# AI usage log

One entry per meaningful use.

| Date | Member | Tool and model | Purpose | Prompt summary | Kept | Modified (how) | Rejected (why) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 13–14 November 2026 | Aisha | ChatGPT; model not recorded | Address difficulty organizing the test strategy and GitHub workflow | Explain the setup steps and help structure the initial testing plan | Strategy draft and workflow guidance | Used module-based branch names without personal names | None recorded |
| 24–27 November 2026 | Lana | ChatGPT (GPT-6) | Help with Users and Security testing, automated test creation and GitHub workflow | Explain JUnit and MockMvc tests, reproduce security defects, troubleshoot Maven and guide documentation updates | Test guidance, four automated security regression tests and GitHub workflow instructions | Reviewed code and test behavior; ran tests locally; verified the coordinator settings CSRF defect by confirming the setting changed from 72 to 48 hours without a CSRF token; updated test-case documentation and submitted PR #41 | Did not change expected security results just to make failing tests pass |
| 15–19 November 2026 | Areeg | Claude (Anthropic) | Step guidance and wording help | Explain steps and output; suggest wording | Starter test code and first test-case draft (AI-generated package); report wording | I reviewed the tests against the requirements, ran all JUnit (O01–O08) and Engine (E01–E18) tests myself, collected the evidence, reproduced every defect on the running system and reported #18, #22–#27 | None |

## Reflection

AI helped clarify unfamiliar planning and GitHub steps.
Command outputs were checked to confirm completed actions.
The testing strategy was developed collaboratively, and the Users and Security tests and documentation were merged into main through PR #41.
Areeg: AI explained each step and drafted report text, but every pass/fail result and actual value comes from my own test runs on 15 Nov 2026, and I reproduced each defect before reporting it.