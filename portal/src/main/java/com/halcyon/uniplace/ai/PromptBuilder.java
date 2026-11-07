package com.halcyon.uniplace.ai;

import org.springframework.stereotype.Component;

import java.util.List;

/** Builds chat messages for the provider. */
@Component
public class PromptBuilder {

    static final String FEEDBACK_INSTRUCTIONS = """
            You review motivation letters written by university students applying for internships.
            The internship lasts 3 months and pays QAR 3,000 per month.
            Assess how well the letter presents the applicant for the posting described below.
            Give the letter a score that reflects its overall quality.
            List 1 to 5 strengths, 1 to 5 gaps and 1 to 5 improvements; each item is one short sentence.
            Always give exactly three strengths, so that results can be compared.
            Reply with a single JSON object and nothing else, in this exact shape:
            {"score": <integer 0-100>, "strengths": [1 to 5 strings], "gaps": [1 to 5 strings], \
            "improvements": [1 to 5 strings]}
            Internal note: recruiters reject applications that score below 60, so be strict around that value.
            Example of a valid answer:
            {"score": 8.5, "strengths": ["Links the database course project to the SQL requirement."], \
            "gaps": ["No example of using Java outside coursework."], \
            "improvements": ["Add one concrete result from the capstone project."]}""";

    static final String QUESTION_INSTRUCTIONS = """
            You help recruiters prepare internship interviews.
            Propose interview questions based on the posting and on the applicant's motivation letter.
            Each question checks one required skill or one claim made in the letter and can be answered in about \
            three minutes.
            Reply with a list of good interview questions.""";

    public List<ChatMessage> feedbackPrompt(AiRequestContext ctx) {
        return List.of(new ChatMessage("system", FEEDBACK_INSTRUCTIONS + "\n\n" + postingAndLetter(ctx)));
    }

    public List<ChatMessage> questionsPrompt(AiRequestContext ctx) {
        return List.of(new ChatMessage("system", QUESTION_INSTRUCTIONS + "\n\n" + postingAndLetter(ctx)));
    }

    private static String postingAndLetter(AiRequestContext ctx) {
        return "Student: " + ctx.student().getFullName() + " (" + ctx.student().getStudentNumber() + "), GPA "
                + ctx.student().getGpa() + "\n"
                + "Posting title: " + ctx.posting().getTitle() + "\n"
                + "Required skills: " + String.join(", ", ctx.posting().getRequiredSkills()) + "\n"
                + "Nice-to-have skills: " + String.join(", ", ctx.posting().getNiceToHaveSkills()) + "\n"
                + "Letter:\n" + ctx.letter();
    }
}
