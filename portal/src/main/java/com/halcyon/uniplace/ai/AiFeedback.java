package com.halcyon.uniplace.ai;

import java.util.List;

/** Structured result of a letter review. {@code source} names the simulated provider or the real model. */
public record AiFeedback(int score, List<String> strengths, List<String> gaps, List<String> improvements,
                         String source) {
}
