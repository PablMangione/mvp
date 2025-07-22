package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.GroupRequest;
import com.acainfo.mvp.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupRequestRepository extends JpaRepository<GroupRequest, Long> {

    // ✅ Verificar solicitud pendiente (evitar duplicados)
    boolean existsByStudentIdAndSubjectIdAndStatus(
            Long studentId, Long subjectId, RequestStatus status);

    // ✅ Solicitudes por estado (para admin)
    List<GroupRequest> findByStatus(RequestStatus status);

    /**
     * Encuentra todas las solicitudes para una asignatura específica.
     */
    List<GroupRequest> findBySubjectId(Long subjectId);

    List<GroupRequest> findByStudentId(Long studentId);

    @Query("SELECT gr FROM GroupRequest gr WHERE " +
            "(:status IS NULL OR gr.status = :status) AND " +
            "(:studentId IS NULL OR gr.student.id = :studentId) AND " +
            "(:subjectId IS NULL OR gr.subject.id = :subjectId) AND " +
            "(:fromDate IS NULL OR gr.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR gr.createdAt <= :toDate)")
    List<GroupRequest> searchByCriteria(@Param("status") RequestStatus status,
                                        @Param("studentId") Long studentId,
                                        @Param("subjectId") Long subjectId,
                                        @Param("fromDate") LocalDateTime fromDate,
                                        @Param("toDate") LocalDateTime toDate);

}