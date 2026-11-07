package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class StudentService {

    static final BigDecimal MAX_GPA = new BigDecimal("4.00");
    static final int MAX_CREDITS = 140;
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern NUMBER = Pattern.compile("\\d{8}");

    private final StudentRepository students;

    public StudentService(StudentRepository students) {
        this.students = students;
    }

    @Transactional(readOnly = true)
    public Student get(Long id) {
        return students.findById(id).orElseThrow(() -> new NotFoundException("Student"));
    }

    @Transactional(readOnly = true)
    public List<Student> all() {
        return students.findAllByOrderByFullNameAsc();
    }

    @Transactional
    public Student create(String number, String fullName, String email, String program, BigDecimal gpa,
                          int credits, int priorInternships) {
        if (number == null || !NUMBER.matcher(number).matches()) {
            throw new BusinessRuleException("The student number must have exactly 8 digits.");
        }
        if (students.existsByStudentNumber(number)) {
            throw new BusinessRuleException("Student number " + number + " already exists.");
        }
        checkAcademic(gpa, credits, priorInternships);
        checkName(fullName);
        checkEmail(email);
        Student s = new Student(number, fullName.trim(), email.trim(), program.trim(),
                gpa.setScale(2, java.math.RoundingMode.HALF_UP), credits, priorInternships);
        return students.save(s);
    }

    /** STU-02: what a student may change about themselves. */
    @Transactional
    public Student updateProfile(Long studentId, String fullName, String email, String skillsCsv) {
        Student s = get(studentId);
        checkName(fullName);
        checkEmail(email);
        s.setFullName(fullName.trim());
        s.setEmail(email.trim());
        s.setSkills(parseSkills(skillsCsv));
        return s;
    }

    /** STU-02: coordinator-only fields. */
    @Transactional
    public Student updateAcademicRecord(Long studentId, BigDecimal gpa, int credits, int priorInternships,
                                        String program) {
        Student s = get(studentId);
        checkAcademic(gpa, credits, priorInternships);
        s.setGpa(gpa.setScale(2, java.math.RoundingMode.HALF_UP));
        s.setCreditsCompleted(credits);
        s.setPriorInternships(priorInternships);
        if (program != null && !program.isBlank()) {
            s.setProgram(program.trim());
        }
        return s;
    }

    public static List<String> parseSkills(String csv) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        if (csv == null) {
            return out;
        }
        for (String raw : csv.split(",")) {
            String skill = raw.trim();
            if (skill.isEmpty()) {
                continue;
            }
            if (skill.length() > 60) {
                throw new BusinessRuleException("A skill can have at most 60 characters.");
            }
            if (seen.add(skill.toLowerCase(Locale.ROOT))) {
                out.add(skill);
            }
        }
        if (out.size() > 30) {
            throw new BusinessRuleException("At most 30 skills can be listed.");
        }
        return out;
    }

    private static void checkAcademic(BigDecimal gpa, int credits, int prior) {
        if (gpa == null || gpa.signum() < 0 || gpa.compareTo(MAX_GPA) > 0) {
            throw new BusinessRuleException("The GPA must be between 0.00 and 4.00.");
        }
        if (credits < 0 || credits > MAX_CREDITS) {
            throw new BusinessRuleException("Credits completed must be between 0 and " + MAX_CREDITS + ".");
        }
        if (prior < 0 || prior > 20) {
            throw new BusinessRuleException("Prior internships must be between 0 and 20.");
        }
    }

    private static void checkName(String name) {
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new BusinessRuleException("The full name is required (at most 120 characters).");
        }
    }

    private static void checkEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches() || email.length() > 120) {
            throw new BusinessRuleException("Enter a valid e-mail address.");
        }
    }
}
