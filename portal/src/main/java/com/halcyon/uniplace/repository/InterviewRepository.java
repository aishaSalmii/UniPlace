package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.Interview;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByRecruiterAndCancelledFalse(User recruiter);

    @Query("select i from Interview i where i.application.student = :student and i.cancelled = false")
    List<Interview> findActiveForStudent(@Param("student") Student student);

    Optional<Interview> findFirstByApplicationAndCancelledFalseOrderByStartDesc(Application application);

    List<Interview> findByApplicationOrderByStartAsc(Application application);
}
