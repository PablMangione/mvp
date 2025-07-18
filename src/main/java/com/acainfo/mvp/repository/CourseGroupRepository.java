package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseGroupRepository extends JpaRepository<CourseGroup, Long> {

    //Grupos por estado (para ver grupos activos)
    List<CourseGroup> findByStatus(CourseGroupStatus status);

    //Grupos por asignatura (para consulta de alumno)
    @EntityGraph(attributePaths = {"teacher", "groupSessions"})
    List<CourseGroup> findBySubjectId(Long subjectId);

    //Grupos sin profesor (útil para admin)
    @EntityGraph(attributePaths = {"subject", "groupSessions"})
    List<CourseGroup> findByTeacherIsNull();

    //Grupos con espacio disponible (para inscripción)
    @Query("SELECT cg FROM CourseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "LEFT JOIN FETCH cg.groupSessions " +
            "WHERE cg.status = 'ACTIVE' " +
            "AND SIZE(cg.enrollments) < :maxCapacity")
    List<CourseGroup> findAvailableGroups(@Param("maxCapacity") int maxCapacity);
}