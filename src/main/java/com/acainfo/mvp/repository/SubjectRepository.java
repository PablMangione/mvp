package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

  // ✅ Consulta de asignaturas por carrera (para alumnos)
  List<Subject> findByMajor(String major);

  // ✅ Búsqueda por año de curso
  List<Subject> findByCourseYear(Integer courseYear);

  // ✅ Asignaturas con grupos activos
  @Query("SELECT DISTINCT s FROM Subject s " +
          "JOIN FETCH s.courseGroups cg " +
          "LEFT JOIN FETCH cg.teacher " +
          "WHERE cg.status = 'ACTIVE'")
  List<Subject> findSubjectsWithActiveGroups();
}