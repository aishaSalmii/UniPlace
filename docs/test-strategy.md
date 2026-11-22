# Test strategy

## Risks and scope

We will assess UniPlace against the supplied Requirements and
Engine API contract.

Our highest priorities are unauthorized access to student data,
incorrect application states or limits, invalid offer acceptance,
overbooking, and incorrect allowance calculations.

We will test the Portal through its screens and source code,
and test the Engine through its API. Supporting checks will
cover Settings, notifications and the AI Assistant.

We will preserve the vendor's original code while collecting
evidence. Every reported defect must be reproducible and linked
to a requirement.

## Primary modules (three) and why

| Module | Owner | Reason |
| --- | --- | --- |
| Student applications | Aisha | Application limits, eligibility and state changes affect whether students can obtain placements. |
| Offers and allowance | Areej | Offer deadlines, seat availability and calculations affect placement decisions and payments. |
| Users and security | Lana | Authentication and access control protect accounts and student information. |

We will design at least 10 test cases per module, covering
functional and non-functional requirements through black-box
and white-box techniques.

## Test levels, techniques and tools, and why

### Levels

- Unit tests will check individual Portal rules.
- Integration tests will check services, persistence and
  communication with the Engine.
- System tests will check complete workflows through the Portal.
- Black-box API tests will compare Engine responses with its
  supplied contract.

### Techniques

- Equivalence partitioning will cover valid and invalid inputs.
- Boundary value analysis will check letter lengths, application
  limits, deadlines, file sizes and allowance thresholds.
- Decision tables will cover combinations of eligibility rules.
- State-transition testing will check allowed and forbidden
  application and offer transitions.
- White-box branch testing will examine decisions in the Portal.
- Security testing will check roles, ownership, sessions and CSRF.
- Performance testing will assess the specified response-time
  and concurrent-user requirements.

### Tools

- JUnit and Spring Boot Test for Portal unit and integration tests.
- JaCoCo for measuring service-layer line coverage.
- Python standard-library HTTP/unittest scripts for Aisha's executed Engine API assertions and HTTP submission checks; curl for controlled eligibility probes.
- Postman remains a planned option for API exploration; its execution is not claimed in Aisha's evidence.
- Python standard-library HTTP timing runner for the executed local application-list workload.
- JMeter remains planned for broader concurrent-user performance testing; completion must be supported by team evidence.
- Browser developer tools for inspecting requests and responses.
- GitHub Issues for defects and GitHub Actions for automated tests.

We will also test the AI Assistant through repeated evaluations,
checking response structure, consistency and resistance to
instructions inside letters. Simulated-provider results will
be distinguished from real-model results.

## Team organisation and Git workflow

Each member will analyse requirements, design and execute tests,
contribute automation, and report defects for her assigned module.

We will use these branches:

- qa/student-application-tests
- qa/offers-allowance
- qa/users-security

Each member will commit through her own account. Changes will
be submitted through pull requests and reviewed by another
member before merging into main.

Each defect issue will include its requirement, description,
reproduction steps, expected and actual results, severity,
priority, category and evidence. It will identify and be assigned
to the member who discovered it.

We will record retained AI-assisted work in docs/ai-usage-log.md,
including how we checked, changed or rejected the output.

API keys, credentials and portal/data will not be committed.

## Changes along the way (date, what changed, why)

| Date | What changed | Why |
| --- | --- | --- |
| 13 November 2026 | Created the initial strategy and assigned three primary modules. | Establish responsibilities and prioritize testing risks. |

| 14 November 2026 | Executed Student Applications Portal integration and Engine API suites. | Establish reproducible results against application rules and the Engine contract. |
| 16 November 2026 | Added synchronized concurrency and state-transition checks; executed pagination, input-retention and local HTTP timing checks. | Investigate race conditions and service paths beyond the original suite; record hardware and scope limitations. |
| 19 November 2026 | Added authenticated concurrent HTTP submission checks and a clean GOOD fixture for limit tests. | Confirm duplicate persistence through HTTP and reduce ambiguity from existing application history. |
| 22 November 2026 | Investigated eligibility mismatch (#36); temporarily lowered default GPA for isolated PROBATION boundary testing, then restored 2.00. | Separate eligibility failure from limit enforcement; 1→2 passed and 2→3 failed (#6). |
| 22 November 2026 | Verified CI at commit 278e123: 223 Portal tests, 213 passed, 8 failures, 2 errors; service line coverage 72.90%. | Reconcile current suite totals and coverage with the uploaded reports; keep historical executions separate. |

## Current execution limitations

Student Applications retains partial scope: actual browser double-clicks and
network timeouts, missing-CSRF interpretation, GOOD configured-limit submission
enforcement, posting-filter retention across multiple pages, wider system and
authorization paths, and reference-hardware/concurrent-user performance remain
unverified. Local sequential HTTP timing does not establish full performance
compliance. AI Assistant repeated evaluations and other modules require their
own executed team evidence. Planned tools and checks are not completed results.

The 22 November service-package line coverage target is met, but the
all-tests-pass condition is not met. Coverage includes vendor tests and must
not be attributed solely to Aisha's acceptance tests. PR #17 had two pending
reviews at the last check; shared case consolidation and final reporting remain
outstanding. No production-code fixes are claimed.
