package com.halcyon.uniplace.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic stand-in used when no API key is configured. */
@Component
public class SimulatedProvider {

    public static final String SOURCE = "Simulated assistant (no AI provider configured)";

    public AiFeedback feedback(AiRequestContext ctx) {
        String letter = ctx.letter();
        String lower = letter.toLowerCase(Locale.ROOT);
        List<String> mentioned = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : ctx.posting().getRequiredSkills()) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) {
                mentioned.add(skill);
            } else {
                missing.add(skill);
            }
        }
        int sentences = Math.max(1, letter.split("[.!?]+").length);
        int score = 20 + Math.min(30, letter.length() / 40) + Math.min(30, 10 * mentioned.size())
                + Math.min(20, 2 * sentences);
        score = Math.min(100, score);

        List<String> strengths = new ArrayList<>();
        for (String s : mentioned) {
            strengths.add("Mentions " + s + ", which the posting requires");
            if (strengths.size() == 4) {
                break;
            }
        }
        if (letter.length() >= 600) {
            strengths.add("Gives enough detail for the reader to understand the applicant");
        }
        if (strengths.isEmpty()) {
            strengths.add("States interest in the posting");
        }
        List<String> gaps = new ArrayList<>();
        for (String s : missing) {
            gaps.add("Does not mention " + s);
            if (gaps.size() == 4) {
                break;
            }
        }
        if (letter.length() < 600) {
            gaps.add("The letter is short; concrete examples are missing");
        }
        if (gaps.isEmpty()) {
            gaps.add("Could connect past work more directly to the posting");
        }
        List<String> improvements = new ArrayList<>();
        improvements.add("Open with the specific role and why it fits your studies");
        if (!missing.isEmpty()) {
            improvements.add("Show evidence of " + missing.get(0) + " from a course or project");
        }
        improvements.add("Close with your availability and the start date");
        return new AiFeedback(score, trim(strengths), trim(gaps), trim(improvements), SOURCE);
    }

    public AiQuestions questions(AiRequestContext ctx) {
        List<String> skills = ctx.posting().getRequiredSkills();
        String first = skills.isEmpty() ? "the main tools of this role" : skills.get(0);
        String second = skills.size() > 1 ? skills.get(1) : first;
        return new AiQuestions(List.of(
                "What attracted you to the " + ctx.posting().getTitle() + " internship?",
                "Describe a project where you used " + first + ". What was your role?",
                "How would you get up to speed with " + second + " in your first two weeks?",
                "Tell us about a time you worked with a team under a deadline.",
                "What would you like to have learned by the end of this internship?"), SOURCE);
    }

    private static List<String> trim(List<String> items) {
        return items.size() > 5 ? items.subList(0, 5) : items;
    }
}
