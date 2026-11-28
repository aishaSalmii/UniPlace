# Combined Portal execution — 27 November 2026

Tested commit: see commit.txt.

Command:
cd portal
./mvnw test -Dmaven.test.failure.ignore=true

Results:
- Executed: 236
- Passed: 217
- Failures: 17
- Errors: 2
- Skipped: 0

Service-layer line coverage:
769 covered / 1,037 total lines = 74.16%.

The 70% coverage target was exceeded, but the suite did not
fully pass. The two errors were unexpected business-rule
exceptions in A07 and A10.

These results cover the Portal suite, including vendor and
team tests. Separate Python Engine checks are not included.

Evidence: Surefire XML/text reports and the jacoco/ folder.