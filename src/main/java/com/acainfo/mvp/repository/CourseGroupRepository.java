package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseGroupRepository extends JpaRepository<CourseGroup, Long> {

    // Búsqueda por estado
    List<CourseGroup> findByStatus(CourseGroupStatus status);

    // Búsqueda por estado con relaciones cargadas
    @EntityGraph(attributePaths = {"subject", "teacher", "groupSessions"})
    List<CourseGroup> findWithDetailsByStatus(CourseGroupStatus status);

    // Búsqueda por tipo
    List<CourseGroup> findByType(CourseGroupType type);

    // Búsqueda por profesor
    @EntityGraph(attributePaths = {"subject", "groupSessions"})
    List<CourseGroup> findByTeacherId(Long teacherId);

    // Búsqueda por asignatura
    @EntityGraph(attributePaths = {"teacher", "groupSessions"})
    List<CourseGroup> findBySubjectId(Long subjectId);

    // Grupos sin profesor (teacher_id es null)
    @EntityGraph(attributePaths = {"subject", "groupSessions"})
    List<CourseGroup> findByTeacherIsNull();

    // Búsqueda por rango de precio
    List<CourseGroup> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Grupos con espacio disponible - Optimizado
    @Query("SELECT cg FROM CourseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "LEFT JOIN FETCH cg.groupSessions " +
            "WHERE cg.status = 'ACTIVE' " +
            "AND SIZE(cg.enrollments) < :maxCapacity")
    List<CourseGroup> findAvailableGroups(@Param("maxCapacity") int maxCapacity);

    // Grupos por estudiante - Optimizado
    @Query("SELECT DISTINCT cg FROM CourseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "JOIN FETCH cg.enrollments e " +
            "LEFT JOIN FETCH cg.groupSessions " +
            "WHERE e.student.id = :studentId")
    List<CourseGroup> findByStudentId(@Param("studentId") Long studentId);

    // Grupos con sesiones en un día específico - Optimizado
    @Query("SELECT DISTINCT cg FROM CourseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "JOIN FETCH cg.groupSessions gs " +
            "WHERE gs.dayOfWeek = :dayOfWeek")
    List<CourseGroup> findByDayOfWeek(@Param("dayOfWeek") String dayOfWeek);

    // Estadísticas de grupos - DTO projection
    @Query("SELECT new map(" +
            "cg.status as status, " +
            "COUNT(cg) as count, " +
            "AVG(cg.price) as avgPrice) " +
            "FROM CourseGroup cg " +
            "GROUP BY cg.status")
    List<Object> countByStatus();

    // Grupos activos por asignatura y profesor
    @EntityGraph(attributePaths = {"subject", "teacher", "groupSessions"})
    List<CourseGroup> findBySubjectIdAndTeacherIdAndStatus(
            Long subjectId, Long teacherId, CourseGroupStatus status);

    // Grupo con todos los detalles
    @EntityGraph(attributePaths = {
            "subject",
            "teacher",
            "enrollments.student",
            "groupSessions"
    })
    @Query("SELECT cg FROM CourseGroup cg WHERE cg.id = :id")
    Optional<CourseGroup> findByIdWithFullDetails(@Param("id") Long id);

    // Grupos con ingresos calculados
    @Query("SELECT cg, " +
            "COUNT(e) as enrollmentCount, " +
            "COUNT(CASE WHEN e.paymentStatus = 'PAID' THEN 1 END) as paidCount, " +
            "COUNT(CASE WHEN e.paymentStatus = 'PAID' THEN 1 END) * cg.price as revenue " +
            "FROM CourseGroup cg " +
            "LEFT JOIN cg.enrollments e " +
            "GROUP BY cg")
    List<Object[]> findGroupsWithRevenue();
}