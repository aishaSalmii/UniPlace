package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.DocumentType;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

    List<StudentDocument> findByStudent(Student student);

    Optional<StudentDocument> findByStudentAndType(Student student, DocumentType type);
}
