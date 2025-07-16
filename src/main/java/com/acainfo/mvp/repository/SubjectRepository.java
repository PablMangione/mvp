package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Subject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

  // Búsqueda por nombre y major (único)
  Optional<Subject> findByNameAndMajor(String name, String major);

  // Búsqueda con grupos cargados
  @EntityGraph(attributePaths = {"courseGroups", "courseGroups.teacher"})
  Optional<Subject> findWithGroupsByNameAndMajor(String name, String major);

  // Verificar si existe
  boolean existsByNameAndMajor(String name, String major);

  // Búsqueda por major
  List<Subject> findByMajor(String major);

  // Búsqueda por major con grupos
  @EntityGraph(attributePaths = {"courseGroups"})
  List<Subject> findWithGroupsByMajor(String major);

  // Búsqueda por año de curso
  List<Subject> findByCourseYear(Integer courseYear);

  // Búsqueda por nombre
  List<Subject> findByNameContainingIgnoreCase(String name);

  // Asignaturas con grupos activos - Optimizado
  @Query("SELECT DISTINCT s FROM Subject s " +
          "JOIN FETCH s.courseGroups cg " +
          "LEFT JOIN FETCH cg.teacher " +
          "WHERE cg.status = 'ACTIVE'")
  List<Subject> findSubjectsWithActiveGroups();

  // Asignaturas con solicitudes pendientes - Optimizado
  @Query("SELECT DISTINCT s FROM Subject s " +
          "JOIN FETCH s.groupRequests gr " +
          "JOIN FETCH gr.student " +
          "WHERE gr.status = 'PENDING'")
  List<Subject> findSubjectsWithPendingRequests();

  // Asignaturas por profesor - Optimizado
  @Query("SELECT DISTINCT s FROM Subject s " +
          "JOIN FETCH s.courseGroups cg " +
          "JOIN FETCH cg.teacher t " +
          "WHERE t.id = :teacherId")
  List<Subject> findByTeacherId(@Param("teacherId") Long teacherId);

  // Contar estudiantes por asignatura - Optimizado con DTO projection
  @Query("SELECT new map(" +
          "s.id as subjectId, " +
          "s.name as subjectName, " +
          "s.major as major, " +
          "COUNT(DISTINCT e.student.id) as studentCount, " +
          "COUNT(DISTINCT cg.id) as groupCount) " +
          "FROM Subject s " +
          "LEFT JOIN s.courseGroups cg " +
          "LEFT JOIN cg.enrollments e " +
          "GROUP BY s.id, s.name, s.major")
  List<Object> findSubjectsWithStudentCount();

  // Asignatura con todos los detalles
  @EntityGraph(attributePaths = {
          "courseGroups.teacher",
          "courseGroups.enrollments.student",
          "courseGroups.groupSessions",
          "groupRequests.student"
  })
  @Query("SELECT s FROM Subject s WHERE s.id = :id")
  Optional<Subject> findByIdWithFullDetails(@Param("id") Long id);

  // Asignaturas más demandadas
  @Query("SELECT s, COUNT(gr) as requestCount " +
          "FROM Subject s " +
          "JOIN s.groupRequests gr " +
          "WHERE gr.status = 'PENDING' " +
          "GROUP BY s " +
          "ORDER BY requestCount DESC")
  List<Object[]> findMostDemandedSubjects();
}