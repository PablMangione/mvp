package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Enrollment;
import com.acainfo.mvp.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    //Verificar inscripción existente
    boolean existsByStudentIdAndCourseGroupId(Long studentId, Long courseGroupId);

    //Buscar inscripción específica
    Optional<Enrollment> findByStudentIdAndCourseGroupId(Long studentId, Long courseGroupId);

    //Contar inscripciones en un grupo (para verificar capacidad)
    Long countByCourseGroupId(Long courseGroupId);
}