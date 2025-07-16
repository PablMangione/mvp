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

    // Búsqueda por email
    Optional<Teacher> findByEmail(String email);

    // Búsqueda por email con grupos cargados
    @EntityGraph(attributePaths = {"courseGroups", "courseGroups.subject"})
    Optional<Teacher> findWithGroupsByEmail(String email);

    // Verificar si existe por email
    boolean existsByEmail(String email);

    // Búsqueda por nombre
    List<Teacher> findByNameContainingIgnoreCase(String name);

    // Profesores con grupos activos - Optimizado
    @Query("SELECT DISTINCT t FROM Teacher t " +
            "JOIN FETCH t.courseGroups cg " +
            "JOIN FETCH cg.subject " +
            "WHERE cg.status = 'ACTIVE'")
    List<Teacher> findTeachersWithActiveGroups();

    // Profesores por asignatura - Optimizado
    @Query("SELECT DISTINCT t FROM Teacher t " +
            "JOIN FETCH t.courseGroups cg " +
            "JOIN FETCH cg.subject s " +
            "WHERE s.id = :subjectId")
    List<Teacher> findBySubjectId(@Param("subjectId") Long subjectId);

    // Profesores disponibles (sin grupos activos)
    @Query("SELECT t FROM Teacher t " +
            "WHERE NOT EXISTS (" +
            "  SELECT cg FROM CourseGroup cg " +
            "  WHERE cg.teacher = t " +
            "  AND cg.status = 'ACTIVE'" +
            ")")
    List<Teacher> findAvailableTeachers();

    // Contar grupos por profesor - Optimizado para DTO
    @Query("SELECT new map(t.id as teacherId, t.name as teacherName, " +
            "COUNT(cg) as groupCount, " +
            "COUNT(CASE WHEN cg.status = 'ACTIVE' THEN 1 END) as activeGroupCount) " +
            "FROM Teacher t " +
            "LEFT JOIN t.courseGroups cg " +
            "GROUP BY t.id, t.name")
    List<Object> findTeachersWithGroupCount();

    // Profesor con todos sus grupos y sesiones
    @EntityGraph(attributePaths = {
            "courseGroups.subject",
            "courseGroups.groupSessions",
            "courseGroups.enrollments"
    })
    @Query("SELECT t FROM Teacher t WHERE t.id = :id")
    Optional<Teacher> findByIdWithFullDetails(@Param("id") Long id);

    // Profesores con horario
    @Query("SELECT DISTINCT t FROM Teacher t " +
            "JOIN FETCH t.courseGroups cg " +
            "JOIN FETCH cg.groupSessions gs " +
            "WHERE cg.status = 'ACTIVE' " +
            "ORDER BY t.name")
    List<Teacher> findTeachersWithSchedule();
}