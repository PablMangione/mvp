package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Autenticación básica y validaciones en registro
    Optional<Student> findByEmail(String email);
    boolean existsByEmail(String email);
}