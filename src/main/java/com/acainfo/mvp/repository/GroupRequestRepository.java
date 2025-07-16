package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.GroupRequest;
import com.acainfo.mvp.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupRequestRepository extends JpaRepository<GroupRequest, Long> {

    // Solicitudes por estudiante
    @EntityGraph(attributePaths = {"subject"})
    List<GroupRequest> findByStudentId(Long studentId);

    // Solicitudes por asignatura
    @EntityGraph(attributePaths = {"student"})
    List<GroupRequest> findBySubjectId(Long subjectId);

    // Solicitudes por estado
    List<GroupRequest> findByStatus(RequestStatus status);

    // Solicitudes por estado con detalles
    @EntityGraph(attributePaths = {"student", "subject"})
    List<GroupRequest> findWithDetailsByStatus(RequestStatus status);

    // Solicitudes por estudiante y estado - Optimizado
    @EntityGraph(attributePaths = {"subject"})
    List<GroupRequest> findByStudentIdAndStatus(Long studentId, RequestStatus status);

    // Solicitudes por asignatura y estado - Optimizado
    @EntityGraph(attributePaths = {"student"})
    List<GroupRequest> findBySubjectIdAndStatus(Long subjectId, RequestStatus status);

    // Solicitudes por fecha
    List<GroupRequest> findByRequestDateBetween(LocalDateTime start, LocalDateTime end);

    // Solicitudes pendientes más antiguas - Optimizado
    @Query("SELECT gr FROM GroupRequest gr " +
            "JOIN FETCH gr.student " +
            "JOIN FETCH gr.subject " +
            "WHERE gr.status = :status " +
            "ORDER BY gr.requestDate ASC")
    List<GroupRequest> findByStatusOrderByRequestDateAsc(@Param("status") RequestStatus status);

    // Contar solicitudes por asignatura - DTO projection
    @Query("SELECT new map(" +
            "s.id as subjectId, " +
            "s.name as subjectName, " +
            "s.major as major, " +
            "COUNT(gr) as requestCount) " +
            "FROM GroupRequest gr " +
            "JOIN gr.subject s " +
            "WHERE gr.status = 'PENDING' " +
            "GROUP BY s.id, s.name, s.major " +
            "ORDER BY COUNT(gr) DESC")
    List<Object> countPendingRequestsBySubject();

    // Asignaturas más solicitadas - Optimizado
    @Query("SELECT s, COUNT(gr) as requestCount " +
            "FROM GroupRequest gr " +
            "JOIN gr.subject s " +
            "GROUP BY s " +
            "ORDER BY requestCount DESC")
    List<Object[]> findMostRequestedSubjects();

    // Verificar si existe solicitud pendiente
    boolean existsByStudentIdAndSubjectIdAndStatus(
            Long studentId, Long subjectId, RequestStatus status);

    // Solicitudes pendientes antiguas - Optimizado
    @Query("SELECT gr FROM GroupRequest gr " +
            "JOIN FETCH gr.student " +
            "JOIN FETCH gr.subject " +
            "WHERE gr.status = 'PENDING' " +
            "AND gr.requestDate < :cutoffDate " +
            "ORDER BY gr.requestDate")
    List<GroupRequest> findOldPendingRequests(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Solicitudes con información completa para dashboard
    @Query("SELECT gr FROM GroupRequest gr " +
            "JOIN FETCH gr.student s " +
            "JOIN FETCH gr.subject sub " +
            "LEFT JOIN FETCH sub.courseGroups cg " +
            "WHERE gr.status = :status")
    List<GroupRequest> findRequestsWithFullDetails(@Param("status") RequestStatus status);

    // Estadísticas de solicitudes por estudiante
    @Query("SELECT new map(" +
            "s.id as studentId, " +
            "s.name as studentName, " +
            "s.major as major, " +
            "COUNT(gr) as totalRequests, " +
            "COUNT(CASE WHEN gr.status = 'PENDING' THEN 1 END) as pendingRequests) " +
            "FROM GroupRequest gr " +
            "JOIN gr.student s " +
            "GROUP BY s.id, s.name, s.major")
    List<Object> getRequestStatsByStudent();
}