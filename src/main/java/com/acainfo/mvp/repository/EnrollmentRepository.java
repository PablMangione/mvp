package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Enrollment;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    List<Enrollment> findByStudentId(Long studentId, Sort attr0);
}