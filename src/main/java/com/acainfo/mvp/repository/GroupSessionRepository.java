package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.enums.DayOfWeek;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface GroupSessionRepository extends JpaRepository<GroupSession, Long> {

    // Sesiones por grupo
    @EntityGraph(attributePaths = {"courseGroup.subject", "courseGroup.teacher"})
    List<GroupSession> findByCourseGroupId(Long courseGroupId);

    // Sesiones por día de la semana
    List<GroupSession> findByDayOfWeek(DayOfWeek dayOfWeek);

    // Sesiones por día con detalles
    @EntityGraph(attributePaths = {"courseGroup.subject", "courseGroup.teacher"})
    List<GroupSession> findWithDetailsByDayOfWeek(DayOfWeek dayOfWeek);

    // Sesiones por aula
    List<GroupSession> findByClassroom(String classroom);

    // Sesiones en un rango de hora
    List<GroupSession> findByStartTimeBetween(LocalTime start, LocalTime end);

    // Sesiones por grupo y día
    List<GroupSession> findByCourseGroupIdAndDayOfWeek(Long courseGroupId, DayOfWeek dayOfWeek);

    // Verificar conflictos de aula - Optimizado
    @Query("SELECT gs FROM GroupSession gs " +
            "JOIN FETCH gs.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "WHERE gs.classroom = :classroom " +
            "AND gs.dayOfWeek = :dayOfWeek " +
            "AND ((gs.startTime <= :startTime AND gs.endTime > :startTime) " +
            "OR (gs.startTime < :endTime AND gs.endTime >= :endTime) " +
            "OR (gs.startTime >= :startTime AND gs.endTime <= :endTime))")
    List<GroupSession> findConflictingSessions(
            @Param("classroom") String classroom,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    // Sesiones por profesor - Optimizado
    @Query("SELECT gs FROM GroupSession gs " +
            "JOIN FETCH gs.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "JOIN FETCH cg.teacher t " +
            "WHERE t.id = :teacherId " +
            "ORDER BY gs.dayOfWeek, gs.startTime")
    List<GroupSession> findByTeacherId(@Param("teacherId") Long teacherId);

    // Sesiones por estudiante - Optimizado
    @Query("SELECT DISTINCT gs FROM GroupSession gs " +
            "JOIN FETCH gs.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "JOIN cg.enrollments e " +
            "WHERE e.student.id = :studentId " +
            "AND e.paymentStatus = 'PAID' " +
            "ORDER BY gs.dayOfWeek, gs.startTime")
    List<GroupSession> findByStudentId(@Param("studentId") Long studentId);

    // Horario semanal por aula - Optimizado
    @Query("SELECT gs FROM GroupSession gs " +
            "JOIN FETCH gs.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "WHERE gs.classroom = :classroom " +
            "ORDER BY gs.dayOfWeek, gs.startTime")
    List<GroupSession> findWeeklyScheduleByClassroom(@Param("classroom") String classroom);

    // Aulas disponibles en un horario
    @Query("SELECT DISTINCT gs.classroom FROM GroupSession gs " +
            "WHERE gs.classroom IS NOT NULL " +
            "AND gs.classroom NOT IN (" +
            "  SELECT gs2.classroom FROM GroupSession gs2 " +
            "  WHERE gs2.dayOfWeek = :dayOfWeek " +
            "  AND gs2.classroom IS NOT NULL " +
            "  AND ((gs2.startTime <= :startTime AND gs2.endTime > :startTime) " +
            "  OR (gs2.startTime < :endTime AND gs2.endTime >= :endTime))" +
            ")")
    List<String> findAvailableClassrooms(
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    // Horario completo de un profesor con detalles
    @Query("SELECT gs FROM GroupSession gs " +
            "JOIN FETCH gs.courseGroup cg " +
            "JOIN FETCH cg.subject s " +
            "JOIN FETCH cg.teacher t " +
            "LEFT JOIN FETCH cg.enrollments " +
            "WHERE t.id = :teacherId " +
            "AND cg.status = 'ACTIVE' " +
            "ORDER BY gs.dayOfWeek, gs.startTime")
    List<GroupSession> findActiveScheduleByTeacher(@Param("teacherId") Long teacherId);

    // Ocupación de aulas por día - DTO projection
    @Query("SELECT new map(" +
            "gs.dayOfWeek as day, " +
            "gs.classroom as classroom, " +
            "COUNT(gs) as sessionCount, " +
            "MIN(gs.startTime) as firstSession, " +
            "MAX(gs.endTime) as lastSession) " +
            "FROM GroupSession gs " +
            "WHERE gs.classroom IS NOT NULL " +
            "GROUP BY gs.dayOfWeek, gs.classroom " +
            "ORDER BY gs.dayOfWeek, gs.classroom")
    List<Object> getClassroomUtilization();

    // Verificar conflictos de horario para un profesor
    @Query("SELECT gs FROM GroupSession gs " +
            "JOIN FETCH gs.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "WHERE cg.teacher.id = :teacherId " +
            "AND gs.dayOfWeek = :dayOfWeek " +
            "AND ((gs.startTime <= :startTime AND gs.endTime > :startTime) " +
            "OR (gs.startTime < :endTime AND gs.endTime >= :endTime))")
    List<GroupSession> findTeacherScheduleConflicts(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    // Sesiones activas con capacidad
    @Query("SELECT gs, " +
            "cg.subject.name as subjectName, " +
            "cg.teacher.name as teacherName, " +
            "SIZE(cg.enrollments) as currentCapacity " +
            "FROM GroupSession gs " +
            "JOIN gs.courseGroup cg " +
            "LEFT JOIN cg.teacher t " +
            "WHERE cg.status = 'ACTIVE' " +
            "AND gs.dayOfWeek = :dayOfWeek " +
            "ORDER BY gs.startTime")
    List<Object[]> findActiveSessionsWithCapacity(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    // Verificar solapamiento de sesiones para un estudiante
    @Query("SELECT CASE WHEN COUNT(gs) > 0 THEN true ELSE false END " +
            "FROM GroupSession gs " +
            "JOIN gs.courseGroup cg " +
            "JOIN cg.enrollments e " +
            "WHERE e.student.id = :studentId " +
            "AND e.paymentStatus = 'PAID' " +
            "AND gs.dayOfWeek = :dayOfWeek " +
            "AND ((gs.startTime <= :startTime AND gs.endTime > :startTime) " +
            "OR (gs.startTime < :endTime AND gs.endTime >= :endTime))")
    boolean hasStudentScheduleConflict(
            @Param("studentId") Long studentId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    // Todas las aulas únicas
    @Query("SELECT DISTINCT gs.classroom FROM GroupSession gs " +
            "WHERE gs.classroom IS NOT NULL " +
            "ORDER BY gs.classroom")
    List<String> findAllClassrooms();
}