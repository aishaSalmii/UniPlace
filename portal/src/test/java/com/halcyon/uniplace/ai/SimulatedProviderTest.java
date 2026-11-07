package com.halcyon.uniplace.ai;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedProviderTest {

    private final SimulatedProvider provider = new SimulatedProvider();

    private static AiRequestContext ctx(String letter) {
        Posting p = new Posting(new Company("C", "S", "e@c.qa"), "Data Intern", "D");
        p.setRequiredSkills(List.of("SQL", "Python"));
        Student s = new Student("20990001", "Test", "t@s.qa", "BSc", new BigDecimal("3.00"), 60, 0);
        return new AiRequestContext(s, p, letter);
    }

    @Test
    void sameLetterGivesTheSameResult() {
        String letter = "I used SQL in two projects and I want to learn more. I am available from January.";
        assertEquals(provider.feedback(ctx(letter)), provider.feedback(ctx(letter)));
    }

    @Test
    void resultIsWithinTheExpectedStructure() {
        AiFeedback f = provider.feedback(ctx("Short letter."));
        assertTrue(f.score() >= 0 && f.score() <= 100);
        assertTrue(f.strengths().size() >= 1 && f.strengths().size() <= 5);
        assertTrue(f.gaps().size() >= 1 && f.gaps().size() <= 5);
        assertTrue(f.improvements().size() >= 1 && f.improvements().size() <= 5);
        assertEquals(SimulatedProvider.SOURCE, f.source());
    }

    @Test
    void questionsAreFive() {
        assertEquals(5, provider.questions(ctx("Any letter")).questions().size());
    }
}
