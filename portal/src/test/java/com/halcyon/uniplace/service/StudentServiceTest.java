package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudentServiceTest extends IntegrationTest {

    @Autowired StudentService service;

    @Test
    void skillsAreTrimmedAndDeduplicated() {
        assertEquals(List.of("Java", "SQL"), StudentService.parseSkills(" Java, SQL , java,, "));
    }

    @Test
    void profileUpdateChangesNameEmailAndSkills() {
        Student s = student("3.00", List.of("Excel"));

        service.updateProfile(s.getId(), "Amal K.", "amal@students.test", "Python, SQL");

        Student saved = students.findById(s.getId()).orElseThrow();
        assertEquals("Amal K.", saved.getFullName());
        assertEquals(List.of("Python", "SQL"), saved.getSkills());
    }

    @Test
    void invalidEmailIsRefused() {
        Student s = student("3.00", List.of("Excel"));
        assertThrows(BusinessRuleException.class, () -> service.updateProfile(s.getId(), "Name", "not-an-email", ""));
    }

    @Test
    void gpaMustBeBetweenZeroAndFour() {
        Student s = student("3.00", List.of("Excel"));
        assertThrows(BusinessRuleException.class,
                () -> service.updateAcademicRecord(s.getId(), new BigDecimal("4.01"), 60, 0, null));
    }

    @Test
    void studentNumberHasEightDigits() {
        assertThrows(BusinessRuleException.class, () -> service.create("1234567", "Name", "a@b.qa", "BSc",
                new BigDecimal("3.00"), 60, 0));
    }
}
