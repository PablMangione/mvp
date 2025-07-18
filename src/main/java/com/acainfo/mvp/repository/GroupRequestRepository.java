package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.GroupRequest;
import com.acainfo.mvp.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRequestRepository extends JpaRepository<GroupRequest, Long> {

    // ✅ Verificar solicitud pendiente (evitar duplicados)
    boolean existsByStudentIdAndSubjectIdAndStatus(
            Long studentId, Long subjectId, RequestStatus status);

    // ✅ Solicitudes por estado (para admin)
    List<GroupRequest> findByStatus(RequestStatus status);
}