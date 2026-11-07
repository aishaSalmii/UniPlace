package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStudent(Student student);

    long countByStudentAndStateIn(Student student, Collection<ApplicationState> states);

    long countByStudentAndStateNotIn(Student student, Collection<ApplicationState> states);

    boolean existsByStudentAndPostingAndStateIn(Student student, Posting posting, Collection<ApplicationState> states);

    boolean existsByStudentAndPostingAndStateNot(Student student, Posting posting, ApplicationState state);

    List<Application> findByStudentAndStateIn(Student student, Collection<ApplicationState> states);

    List<Application> findByPostingAndStateIn(Posting posting, Collection<ApplicationState> states);

    List<Application> findByPosting(Posting posting);

    long countByStateIn(Collection<ApplicationState> states);

    @EntityGraph(attributePaths = {"student", "posting", "posting.company"})
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findDetailed(@Param("id") Long id);

    @EntityGraph(attributePaths = {"posting", "posting.company"})
    Page<Application> findByStudent(Student student, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "posting", "posting.company"})
    @Query(value = "select a from Application a where a.posting.company = :company "
            + "and (:state is null or a.state = :state) and (:postingId is null or a.posting.id = :postingId)",
            countQuery = "select count(a) from Application a where a.posting.company = :company "
            + "and (:state is null or a.state = :state) and (:postingId is null or a.posting.id = :postingId)")
    Page<Application> findForCompany(@Param("company") Company company, @Param("state") ApplicationState state,
                                     @Param("postingId") Long postingId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "posting", "posting.company"})
    @Query(value = "select a from Application a where (:state is null or a.state = :state) "
            + "and (:postingId is null or a.posting.id = :postingId) "
            + "and (:companyId is null or a.posting.company.id = :companyId)",
            countQuery = "select count(a) from Application a where (:state is null or a.state = :state) "
            + "and (:postingId is null or a.posting.id = :postingId) "
            + "and (:companyId is null or a.posting.company.id = :companyId)")
    Page<Application> search(@Param("state") ApplicationState state, @Param("postingId") Long postingId,
                             @Param("companyId") Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"student"})
    List<Application> findByPostingAndStateInOrderBySubmittedAtAsc(Posting posting, Collection<ApplicationState> states);

    @Query("select a.posting.company.name, avg(a.matchScore), count(a) from Application a "
            + "group by a.posting.company.name order by a.posting.company.name")
    List<Object[]> averageScoreByCompany();

    @Query("select s.program, count(distinct s.id), count(distinct case when a.state = "
            + "com.halcyon.uniplace.domain.ApplicationState.ACCEPTED then s.id end) "
            + "from Student s left join Application a on a.student = s group by s.program order by s.program")
    List<Object[]> placementByProgram();
}
