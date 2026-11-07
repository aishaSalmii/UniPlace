package com.halcyon.uniplace.ai;

import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;

/** What the assistant may know about a request. Only posting data and the letter are sent to a provider. */
public record AiRequestContext(Student student, Posting posting, String letter) {
}
