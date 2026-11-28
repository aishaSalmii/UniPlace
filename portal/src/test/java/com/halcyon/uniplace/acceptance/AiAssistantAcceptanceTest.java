package com.halcyon.uniplace.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.uniplace.ai.*;
import com.halcyon.uniplace.domain.*;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI-01 to AI-11, checked without calling a real model: the prompts the Portal builds,
 * the validation of provider answers, the result label and the cache.
 * The provider client (AiClient) is mocked; prompts are read from PromptBuilder.
 * AI-assisted (Claude); reviewed and executed by Areeg AlGhazali.
 */
public class AiAssistantAcceptanceTest extends IntegrationTest {
    static final String LETTER = "I built a REST API in Java with Spring Boot and wrote SQL migrations for a "
            + "course project. I used Git with pull requests and would like to learn how a production team works.";
    static final String VALID = "{\"score\": 72, \"strengths\": [\"Concrete Java project.\"], "
            + "\"gaps\": [\"No Docker experience.\"], \"improvements\": [\"Add one measurable result.\"]}";

    @Autowired PromptBuilder prompts;
    @Autowired AiResponseParser parser;
    @Autowired AiAssistantService assistant;
    @MockBean AiClient client;

    Student student;
    Posting posting;
    AiRequestContext ctx;

    @BeforeEach
    void fixture() {
        Company c = company("QA AI", CompanyStatus.APPROVED);
        posting = openPosting(c, "Backend Developer Intern", List.of("Java", "SQL", "Spring"), 2);
        student = student("3.25", List.of("Java"));
        ctx = new AiRequestContext(student, posting, LETTER);
    }

    private static String text(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages) {
            sb.append(m.content()).append('\n');
        }
        return sb.toString();
    }

    private void assertNoPersonalData(String prompt) {
        assertAll(
                () -> assertFalse(prompt.contains(student.getFullName()), "prompt contains the student's name"),
                () -> assertFalse(prompt.contains(student.getStudentNumber()), "prompt contains the student number"),
                () -> assertFalse(prompt.contains("GPA"), "prompt contains the GPA"));
    }

    // ---- AI-03: no personal data in the request ----

    @Test
    void I01_feedbackPromptContainsNoPersonalData() {
        assertNoPersonalData(text(prompts.feedbackPrompt(ctx)));
    }

    @Test
    void I02_questionsPromptContainsNoPersonalData() {
        assertNoPersonalData(text(prompts.questionsPrompt(ctx)));
    }

    // ---- AI-10: no internal business rules or thresholds ----

    @Test
    void I03_feedbackPromptContainsNoInternalRules() {
        String prompt = text(prompts.feedbackPrompt(ctx));
        assertAll(
                () -> assertFalse(prompt.toLowerCase().contains("reject"), "prompt mentions the rejection rule"),
                () -> assertFalse(prompt.contains("60"), "prompt contains the shortlist threshold 60"),
                () -> assertFalse(prompt.contains("QAR"), "prompt contains allowance data"));
    }

    // ---- AI-11: criteria, scale, item counts and format, with consistent examples ----

    @Test
    void I04_feedbackExampleIsConsistentWithTheFormat() throws Exception {
        String prompt = text(prompts.feedbackPrompt(ctx));
        String after = prompt.substring(prompt.indexOf("Example of a valid answer:"));
        String example = after.substring(after.indexOf('{'), after.indexOf('}') + 1);
        JsonNode score = new ObjectMapper().readTree(example).get("score");
        assertTrue(score.isInt() && score.asInt() >= 0 && score.asInt() <= 100,
                "example score " + score + " is not an integer from 0 to 100");
    }

    @Test
    void I05_feedbackItemCountsAreNotContradictory() {
        String prompt = text(prompts.feedbackPrompt(ctx));
        assertFalse(prompt.contains("exactly three strengths"),
                "prompt asks for 1 to 5 strengths and for exactly three strengths");
    }

    @Test
    void I06_questionsPromptStatesNumberAndFormat() {
        String prompt = text(prompts.questionsPrompt(ctx));
        assertAll(
                () -> assertTrue(prompt.toLowerCase().matches("(?s).*\\b(5|five)\\s+(\\w+\\s+)?questions.*"),
                        "questions prompt does not ask for 5 questions"),
                () -> assertTrue(prompt.contains("JSON"), "questions prompt does not state the answer format"));
    }

    // ---- AI-06: invalid answers are treated as errors ----

    @Test
    void I07_parserRefusesMoreThanFiveItems() {
        String six = "{\"score\": 70, \"strengths\": [\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"], "
                + "\"gaps\": [\"g\"], \"improvements\": [\"i\"]}";
        assertThrows(AiUnavailableException.class, () -> parser.parseFeedback(six, "test"));
    }

    @Test
    void I08_parserRefusesAnEmptyList() {
        String empty = "{\"score\": 70, \"strengths\": [], \"gaps\": [\"g\"], \"improvements\": [\"i\"]}";
        assertThrows(AiUnavailableException.class, () -> parser.parseFeedback(empty, "test"));
    }

    @Test
    void I09_parserRefusesScoreAbove100() {
        String high = "{\"score\": 150, \"strengths\": [\"s\"], \"gaps\": [\"g\"], \"improvements\": [\"i\"]}";
        assertThrows(AiUnavailableException.class, () -> parser.parseFeedback(high, "test"));
    }

    @Test
    void I10_parserAcceptsAValidAnswer() {
        AiFeedback f = parser.parseFeedback(VALID, "test");
        assertEquals(72, f.score());
        assertEquals(1, f.strengths().size());
    }

    // ---- AI-07: the result names its source and the real model ----

    @Test
    void I11_simulatedResultIsLabelledSimulated() {
        when(client.configured()).thenReturn(false);
        AiFeedback f = assistant.feedback(ctx);
        assertTrue(f.source().toLowerCase().contains("simulated"), "source: " + f.source());
    }

    @Test
    void I12_realResultNamesTheConfiguredModel() {
        when(client.configured()).thenReturn(true);
        when(client.model()).thenReturn("gpt-4.1-mini");
        when(client.complete(any())).thenReturn(VALID);
        AiFeedback f = assistant.feedback(ctx);
        assertTrue(f.source().contains("gpt-4.1-mini"), "source names another model: " + f.source());
    }

    // ---- AI-08: cache per student, posting and letter content ----

    @Test
    void I13_sameLetterIsServedFromTheCache() {
        when(client.configured()).thenReturn(true);
        when(client.model()).thenReturn("gpt-4o-mini");
        when(client.complete(any())).thenReturn(VALID);
        AiFeedback first = assistant.feedback(ctx);
        AiFeedback second = assistant.feedback(ctx);
        assertEquals(first, second);
        verify(client, times(1)).complete(any());
    }

    @Test
    void I14_changedLetterIsNotServedFromTheCache() {
        when(client.configured()).thenReturn(true);
        when(client.model()).thenReturn("gpt-4o-mini");
        when(client.complete(any())).thenReturn(VALID);
        assistant.feedback(ctx);
        assistant.feedback(new AiRequestContext(student, posting, LETTER + " I also used Docker Compose."));
        verify(client, times(2)).complete(any());
    }
}