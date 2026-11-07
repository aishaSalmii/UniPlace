package com.halcyon.uniplace.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Letter feedback for students and interview questions for recruiters. */
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private final AiClient client;
    private final SimulatedProvider simulated;
    private final PromptBuilder prompts;
    private final AiResponseParser parser;
    private final FeedbackCache cache;

    public AiAssistantService(AiClient client, SimulatedProvider simulated, PromptBuilder prompts,
                              AiResponseParser parser, FeedbackCache cache) {
        this.client = client;
        this.simulated = simulated;
        this.prompts = prompts;
        this.parser = parser;
        this.cache = cache;
    }

    /** @throws AiUnavailableException when the provider fails or answers with an invalid structure */
    public AiFeedback feedback(AiRequestContext ctx) {
        Long studentId = ctx.student().getId();
        Long postingId = ctx.posting().getId();
        return cache.get(studentId, postingId, ctx.letter()).orElseGet(() -> {
            AiFeedback result = client.configured()
                    ? parser.parseFeedback(client.complete(prompts.feedbackPrompt(ctx)), "Model " + AiClient.DEFAULT_MODEL)
                    : simulated.feedback(ctx);
            cache.put(studentId, postingId, ctx.letter(), result);
            log.info("Letter feedback produced for posting {} by {}", postingId, result.source());
            return result;
        });
    }

    public AiQuestions interviewQuestions(AiRequestContext ctx) {
        if (!client.configured()) {
            return simulated.questions(ctx);
        }
        return parser.parseQuestions(client.complete(prompts.questionsPrompt(ctx)), "Model " + client.model());
    }
}
