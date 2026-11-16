package com.halcyon.uniplace.acceptance;

import com.halcyon.uniplace.domain.*;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityResult;
import com.halcyon.uniplace.engine.EngineDtos.MatchResult;
import com.halcyon.uniplace.engine.EngineUnavailableException;
import com.halcyon.uniplace.service.*;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** APP-05 / M01 supplemental integration checks. AI-assisted.
 * Isolated H2; Engine mocked. Does not test live sessions, CSRF, browser or network timeouts.
 * Record actual execution before reporting; unexpected exceptions/timeouts are inconclusive. */
public class StudentApplicationConcurrencyTest extends IntegrationTest {
    @Autowired ApplicationService service;
    @MockBean EngineClient engine;
    Company company;
    Posting posting;
    Student student;
    User owner;
    User recruiter;

    @BeforeEach
    void fixture() {
        company = company("QA Applications", CompanyStatus.APPROVED);
        posting = openPosting(company, "QA Internship", List.of("Java"), 2);
        student = student("3.20", List.of("Java"));
        owner = studentUser(student);
        recruiter = recruiter(company);
        validDocuments(student);
        when(engine.checkEligibility(any())).thenReturn(EligibilityResult.ok());
        when(engine.match(any())).thenReturn(new MatchResult(70, List.of("Java"), List.of(), List.of()));
    }

    @Test void M01a_retryAfterCommitKeepsOneApplication() {
        service.submit(owner, posting.getId(), "x".repeat(120));
        // Discard the first successful response, then repeat the same operation.
        assertThrows(BusinessRuleException.class,
                () -> service.submit(owner, posting.getId(), "x".repeat(120)));
        assertEquals(1, applications.count());
        assertEquals(ApplicationState.SUBMITTED, applications.findAll().get(0).getState());
    }

    @Test void M01b_overlappingSubmissionsKeepOneApplication() throws Exception {
        // Both requests pass duplicate checks before either inserts.
        CountDownLatch gate = new CountDownLatch(2);
        when(engine.checkEligibility(any())).thenAnswer(invocation -> {
            gate.countDown();
            if (!gate.await(10, TimeUnit.SECONDS))
                throw new IllegalStateException("Concurrency gate timeout: inconclusive test");
            return EligibilityResult.ok();
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<String> submit = () -> {
                try {
                    User principal = users.findById(owner.getId()).orElseThrow();
                    service.submit(principal, posting.getId(), "x".repeat(120));
                    return "submitted";
                } catch (BusinessRuleException refusal) {
                    return "business-rule refusal";
                }
            };
            Future<String> first = pool.submit(submit);
            Future<String> second = pool.submit(submit);
            String a = first.get(25, TimeUnit.SECONDS);
            String b = second.get(25, TimeUnit.SECONDS);
            long count = applications.count();
            System.out.println("M01 outcomes: " + a + ", " + b
                    + "; persisted applications=" + count);
            assertEquals(1, count,
                    "APP-05: overlapping submissions must persist exactly one application");
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "Workers did not stop");
        }
    }
}
