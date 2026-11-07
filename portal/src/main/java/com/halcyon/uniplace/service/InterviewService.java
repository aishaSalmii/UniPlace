package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Interview;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.SlotRequest;
import com.halcyon.uniplace.engine.EngineDtos.SlotResult;
import com.halcyon.uniplace.engine.EngineRequestMapper;
import com.halcyon.uniplace.repository.InterviewRepository;
import com.halcyon.uniplace.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** INT-01 to INT-05. */
@Service
public class InterviewService {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InterviewRepository interviews;
    private final ApplicationService applications;
    private final EngineClient engine;
    private final EngineRequestMapper mapper;
    private final NotificationService notifications;
    private final UserRepository users;
    private final AccessGuard guard;

    public InterviewService(InterviewRepository interviews, ApplicationService applications, EngineClient engine,
                            EngineRequestMapper mapper, NotificationService notifications, UserRepository users,
                            AccessGuard guard) {
        this.interviews = interviews;
        this.applications = applications;
        this.engine = engine;
        this.mapper = mapper;
        this.notifications = notifications;
        this.users = users;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<Interview> forApplication(Application a) {
        return interviews.findByApplicationOrderByStartAsc(a);
    }

    @Transactional
    public Interview schedule(User recruiter, Long applicationId, LocalDate date, LocalTime from, LocalTime to,
                              String location) {
        Application a = applications.get(applicationId);
        guard.assertRecruiterOwns(recruiter, a);
        if (a.getState() != ApplicationState.SHORTLISTED) {
            throw new BusinessRuleException("Only a shortlisted application can be invited to an interview.");
        }
        if (date == null || from == null || to == null) {
            throw new BusinessRuleException("Enter the date, start time and end time.");
        }
        if (location == null || location.isBlank() || location.length() > 200) {
            throw new BusinessRuleException("Enter the location or meeting link (at most 200 characters).");
        }
        applications.assertNotFrozen(a);
        LocalDateTime start = date.atTime(from);
        LocalDateTime end = date.atTime(to);
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("The end time must be after the start time.");
        }

        SlotRequest request = new SlotRequest();
        request.candidate = mapper.toSlot(start, end);
        request.recruiterSlots = mapper.toSlots(interviews.findByRecruiterAndCancelledFalse(recruiter));
        request.studentSlots = mapper.toSlots(interviews.findActiveForStudent(a.getStudent()));
        SlotResult result = engine.validateSlot(request);
        if (!result.valid()) {
            throw new BusinessRuleException("This slot cannot be used: " + String.join("; ", result.conflicts()) + ".");
        }

        Interview interview = interviews.save(new Interview(a, recruiter, start, end, location.trim()));
        applications.transition(a, ApplicationState.INTERVIEW);
        a.setRescheduleRequested(false);
        applications.notifyStudent(a, "Interview scheduled: " + a.getPosting().getTitle(),
                "Your interview for " + a.getPosting().getTitle() + " at " + a.getPosting().getCompany().getName()
                        + " is on " + start.format(DISPLAY) + " until " + end.toLocalTime() + ", at " + location.trim() + ".");
        return interview;
    }

    @Transactional
    public void cancel(User recruiter, Long interviewId) {
        Interview i = interviews.findById(interviewId).orElseThrow(() -> new NotFoundException("Interview"));
        Application a = applications.get(i.getApplication().getId());
        guard.assertRecruiterOwns(recruiter, a);
        if (i.isCancelled()) {
            throw new BusinessRuleException("This interview is already cancelled.");
        }
        if (a.getState() != ApplicationState.INTERVIEW) {
            throw new BusinessRuleException("Only an interview that has not led to an offer can be cancelled.");
        }
        i.setCancelled(true);
        applications.transition(a, ApplicationState.SHORTLISTED);
        applications.notifyStudent(a, "Interview cancelled: " + a.getPosting().getTitle(),
                "Your interview on " + i.getStart().format(DISPLAY) + " for " + a.getPosting().getTitle()
                        + " was cancelled.");
    }

    /** INT-04: one reschedule request per application. */
    @Transactional
    public void requestReschedule(User student, Long applicationId, String note) {
        Application a = applications.get(applicationId);
        guard.assertStudentOwns(student, a);
        if (a.getState() != ApplicationState.INTERVIEW) {
            throw new BusinessRuleException("There is no scheduled interview to reschedule.");
        }
        if (a.isRescheduleRequested() || interviews.findByApplicationOrderByStartAsc(a).stream().anyMatch(Interview::isCancelled)) {
            throw new BusinessRuleException("A reschedule was already requested for this application.");
        }
        a.setRescheduleRequested(true);
        String reason = note == null || note.isBlank() ? "No reason given." : note.trim();
        for (User r : users.findByCompanyAndRole(a.getPosting().getCompany(), Role.RECRUITER)) {
            notifications.notify(r, "Reschedule requested: " + a.getPosting().getTitle(),
                    a.getStudent().getFullName() + " asked to reschedule the interview for "
                            + a.getPosting().getTitle() + " at " + a.getPosting().getCompany().getName() + ". " + reason);
        }
    }
}
