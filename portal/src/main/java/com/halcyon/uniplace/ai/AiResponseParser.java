package com.halcyon.uniplace.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Turns the provider's reply into a result. */
@Component
public class AiResponseParser {

    static final int MIN_ITEMS = 1;
    static final int MAX_ITEMS = 5;

    private final ObjectMapper mapper;

    public AiResponseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public AiFeedback parseFeedback(String content, String source) {
        JsonNode root = readObject(content);
        JsonNode score = root.get("score");
        if (score == null || !score.isNumber()) {
            throw new AiUnavailableException("invalid score");
        }
        return new AiFeedback(score.asInt(), list(root, "strengths", MIN_ITEMS, MAX_ITEMS),
                list(root, "gaps", MIN_ITEMS, MAX_ITEMS), list(root, "improvements", MIN_ITEMS, MAX_ITEMS), source);
    }

    public AiQuestions parseQuestions(String content, String source) {
        JsonNode root = readObject(content);
        return new AiQuestions(list(root, "questions", 5, 5), source);
    }

    private JsonNode readObject(String content) {
        if (content == null) {
            throw new AiUnavailableException("empty answer");
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int first = text.indexOf('{');
            int last = text.lastIndexOf('}');
            text = first >= 0 && last > first ? text.substring(first, last + 1) : text;
        }
        try {
            JsonNode node = mapper.readTree(text);
            if (node == null || !node.isObject()) {
                throw new AiUnavailableException("answer is not a JSON object");
            }
            return node;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new AiUnavailableException("answer is not JSON");
        }
    }

    private static List<String> list(JsonNode root, String field, int min, int max) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) {
            throw new AiUnavailableException("invalid " + field);
        }
        List<String> out = new ArrayList<>();
        for (JsonNode item : node) {
            out.add(item.asText().trim());
        }
        return out;
    }
}
