# Combined Portal execution — 28 November 2026

Tested commit: see commit.txt.
Includes merged PRs #50 and #58.

Command:
cd portal
./mvnw clean test -Dmaven.test.failure.ignore=true

Results:
- Executed: 267
- Passed: 234
- Failures: 31
- Errors: 2
- Skipped: 0

Service-layer line coverage:
801 covered / 1,037 total lines = 77.24%.

The 70% coverage target was exceeded, but the suite did not
fully pass. Coverage includes vendor and team tests.
Separate Python Engine checks are not included.

Evidence: Surefire XML/text reports and the jacoco/ folder.