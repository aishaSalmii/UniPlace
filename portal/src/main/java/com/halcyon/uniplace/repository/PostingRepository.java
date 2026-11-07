package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.PostingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    @EntityGraph(attributePaths = "company")
    List<Posting> findByCompanyOrderByDeadlineAsc(Company company);

    List<Posting> findByCompany(Company company);

    @EntityGraph(attributePaths = "company")
    @Query("select p from Posting p where p.company.status = :companyStatus and p.status in :statuses "
            + "and (p.status = com.halcyon.uniplace.domain.PostingStatus.OPEN or p.deadline >= :closedSince) "
            + "order by p.deadline asc, p.id asc")
    List<Posting> findVisible(@Param("companyStatus") CompanyStatus companyStatus,
                              @Param("statuses") Collection<PostingStatus> statuses,
                              @Param("closedSince") LocalDate closedSince);

    @EntityGraph(attributePaths = "company")
    @Query("select p from Posting p where p.company.id = :companyId and p.company.status = :companyStatus "
            + "and p.status in :statuses "
            + "and (p.status = com.halcyon.uniplace.domain.PostingStatus.OPEN or p.deadline >= :closedSince) "
            + "order by p.id asc")
    List<Posting> findVisibleForCompany(@Param("companyId") Long companyId,
                                        @Param("companyStatus") CompanyStatus companyStatus,
                                        @Param("statuses") Collection<PostingStatus> statuses,
                                        @Param("closedSince") LocalDate closedSince);

    List<Posting> findByStatusAndDeadlineBefore(PostingStatus status, LocalDate date);

    long countByStatus(PostingStatus status);

    @EntityGraph(attributePaths = "company")
    @Query("select p from Posting p where p.id = :id")
    Optional<Posting> findWithCompany(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Posting p where p.id = :id")
    Optional<Posting> findByIdForUpdate(@Param("id") Long id);
}
