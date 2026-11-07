package com.halcyon.uniplace.engine;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.Interview;
import com.halcyon.uniplace.domain.Offer;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.StudentDocument;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceRequest;
import com.halcyon.uniplace.engine.EngineDtos.DocumentInfo;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityRequest;
import com.halcyon.uniplace.engine.EngineDtos.MatchRequest;
import com.halcyon.uniplace.engine.EngineDtos.RankCandidate;
import com.halcyon.uniplace.engine.EngineDtos.Slot;
import com.halcyon.uniplace.service.SettingsService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Builds Engine requests from domain objects. */
@Component
public class EngineRequestMapper {

    private final SettingsService settings;
    private final Clock clock;

    public EngineRequestMapper(SettingsService settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    public EligibilityRequest toEligibilityRequest(Student student, Posting posting, List<StudentDocument> documents,
                                                   LocalDate asOf) {
        EligibilityRequest r = new EligibilityRequest();
        r.gpa = student.getGpa();
        r.creditsCompleted = student.getCreditsCompleted();
        r.standing = student.getStanding().name();
        r.minGpa = settings.getDecimal(SettingsService.DEFAULT_MIN_GPA);
        r.minCredits = posting.getMinCredits();
        r.asOf = asOf;
        for (StudentDocument d : documents) {
            DocumentInfo info = new DocumentInfo();
            info.type = d.getType().name();
            info.sizeBytes = d.getSizeBytes();
            info.mimeType = d.getMimeType();
            info.issueDate = d.getIssueDate();
            r.documents.add(info);
        }
        return r;
    }

    public MatchRequest toMatchRequest(List<String> studentSkills, Posting posting) {
        MatchRequest r = new MatchRequest();
        r.studentSkills = new ArrayList<>(studentSkills);
        r.requiredSkills = new ArrayList<>(posting.getRequiredSkills());
        r.niceToHaveSkills = new ArrayList<>(posting.getNiceToHaveSkills());
        return r;
    }

    public RankCandidate toCandidate(Application a) {
        RankCandidate c = new RankCandidate();
        c.applicationId = a.getId();
        c.score = a.getMatchScore();
        c.gpa = a.getStudent().getGpa();
        c.submittedAt = withOffset(a.getSubmittedAt());
        return c;
    }

    public Slot toSlot(LocalDateTime start, LocalDateTime end) {
        return new Slot(start, end);
    }

    public List<Slot> toSlots(List<Interview> interviews) {
        List<Slot> slots = new ArrayList<>();
        for (Interview i : interviews) {
            slots.add(toSlot(i.getStart(), i.getEnd()));
        }
        return slots;
    }

    public AllowanceRequest toAllowanceRequest(Offer offer, int lateDays) {
        AllowanceRequest r = new AllowanceRequest();
        r.monthlyAllowance = offer.getMonthlyAllowance();
        r.startDate = offer.getStartDate();
        r.endDate = offer.getEndDate();
        r.weeklyHours = offer.getWeeklyHours();
        r.lateDays = lateDays;
        r.transportSupplement = settings.getDecimal(SettingsService.TRANSPORT_SUPPLEMENT);
        r.penaltyRate = settings.getDecimal(SettingsService.PENALTY_RATE);
        r.penaltyCap = settings.getDecimal(SettingsService.PENALTY_CAP);
        return r;
    }

    private OffsetDateTime withOffset(LocalDateTime local) {
        return local.atZone(clock.getZone()).toOffsetDateTime();
    }
}
