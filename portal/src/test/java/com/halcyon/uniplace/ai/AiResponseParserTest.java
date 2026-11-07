package com.halcyon.uniplace.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiResponseParserTest {

    private final AiResponseParser parser = new AiResponseParser(new ObjectMapper());

    @Test
    void validFeedbackIsParsed() {
        AiFeedback f = parser.parseFeedback("{\"score\":72,\"strengths\":[\"Clear goal\"],\"gaps\":[\"No SQL example\"],"
                + "\"improvements\":[\"Add a project\",\"Shorten the opening\"]}", "Model test");

        assertEquals(72, f.score());
        assertEquals(List.of("Add a project", "Shorten the opening"), f.improvements());
        assertEquals("Model test", f.source());
    }

    @Test
    void jsonInsideAMarkdownFenceIsAccepted() {
        AiFeedback f = parser.parseFeedback("```json\n{\"score\":50,\"strengths\":[\"a\"],\"gaps\":[\"b\"],"
                + "\"improvements\":[\"c\"]}\n```", "Model test");
        assertEquals(50, f.score());
    }

    @Test
    void textThatIsNotJsonIsAnError() {
        assertThrows(AiUnavailableException.class, () -> parser.parseFeedback("Great letter!", "Model test"));
    }

    @Test
    void fiveQuestionsAreParsed() {
        AiQuestions q = parser.parseQuestions("{\"questions\":[\"q1\",\"q2\",\"q3\",\"q4\",\"q5\"]}", "Model test");
        assertEquals(5, q.questions().size());
    }
}
