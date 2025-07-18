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

    //Sesiones por grupo (para mostrar horarios)
    @EntityGraph(attributePaths = {"courseGroup.subject", "courseGroup.teacher"})
    List<GroupSession> findByCourseGroupId(Long courseGroupId);

    //Horario del profesor
    @Query("SELECT gs FROM GroupSession gs " +
            "JOIN FETCH gs.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "JOIN FETCH cg.teacher t " +
            "WHERE t.id = :teacherId " +
            "ORDER BY gs.dayOfWeek, gs.startTime")
    List<GroupSession> findByTeacherId(@Param("teacherId") Long teacherId);
}