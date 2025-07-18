package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Teacher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    // ✅ Autenticación básica
    Optional<Teacher> findByEmail(String email);
    boolean existsByEmail(String email);
}