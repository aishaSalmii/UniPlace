package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByStudent(Student student);

    List<User> findByCompanyAndRole(Company company, Role role);

    List<User> findByRole(Role role);

    List<User> findAllByOrderByUsernameAsc();

    boolean existsByUsername(String username);
}
