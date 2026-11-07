package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findAllByOrderByNameAsc();

    List<Company> findByStatusOrderByNameAsc(CompanyStatus status);

    @Query("select count(c) > 0 from Company c where lower(trim(c.name)) = lower(trim(:name)) and (:excludeId is null or c.id <> :excludeId)")
    boolean nameTaken(@Param("name") String name, @Param("excludeId") Long excludeId);

    boolean existsByNameAndIdNot(String name, Long id);
}
