package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.enrollment.CreateEnrollmentDto;
import com.acainfo.mvp.dto.enrollment.EnrollmentDto;
import com.acainfo.mvp.dto.enrollment.EnrollmentResponseDto;
import com.acainfo.mvp.dto.student.EnrollmentSummaryDto;
import com.acainfo.mvp.exception.student.DuplicateRequestException;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.EnrollmentMapper;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import com.acainfo.mvp.model.enums.PaymentStatus;
import com.acainfo.mvp.repository.CourseGroupRepository;
import com.acainfo.mvp.repository.EnrollmentRepository;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para EnrollmentService.
 * Verifica el proceso de inscripción y gestión de matrículas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService Tests")
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseGroupRepository courseGroupRepository;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private SessionUtils sessionUtils;

    private EnrollmentService enrollmentService;

    private Student testStudent;
    private Subject testSubject;
    private Teacher testTeacher;
    private CourseGroup testActiveGroup;
    private CourseGroup testFullGroup;
    private CourseGroup testPlannedGroup;
    private Enrollment testEnrollment;
    private CreateEnrollmentDto validCreateDto;
    private EnrollmentDto testEnrollmentDto;
    private EnrollmentSummaryDto testEnrollmentSummaryDto;
    private EnrollmentResponseDto successResponseDto;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(
                enrollmentRepository,
                studentRepository,
                courseGroupRepository,
                enrollmentMapper,
                sessionUtils
        );

        // Configurar entidades de prueba con IDs usando reflection
        testStudent = Student.builder()
                .name("Juan Pérez")
                .email("juan@estudiante.edu")
                .major("Ingeniería Informática")
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testStudent, "id", 1L);

        testSubject = Subject.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();
        ReflectionTestUtils.setField(testSubject, "id", 1L);

        testTeacher = Teacher.builder()
                .name("Dr. García")
                .email("garcia@universidad.edu")
                .build();
        ReflectionTestUtils.setField(testTeacher, "id", 1L);

        testActiveGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .maxCapacity(30)
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testActiveGroup, "id", 1L);

        testFullGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .maxCapacity(2)
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testFullGroup, "id", 2L);

        // Llenar el grupo completo
        for (int i = 0; i < 2; i++) {
            Enrollment e = Enrollment.builder().build();
            testFullGroup.getEnrollments().add(e);
        }

        testPlannedGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .maxCapacity(30)
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testPlannedGroup, "id", 3L);

        testEnrollment = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testActiveGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(testEnrollment, "id", 1L);

        validCreateDto = CreateEnrollmentDto.builder()
                .studentId(1L)
                .courseGroupId(1L)
                .build();

        testEnrollmentDto = EnrollmentDto.builder()
                .studentId(1L)
                .studentName("Juan Pérez")
                .courseGroupId(1L)
                .subjectName("Programación I")
                .teacherName("Dr. García")
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        testEnrollmentSummaryDto = EnrollmentSummaryDto.builder()
                .enrollmentId(1L)
                .courseGroupId(1L)
                .subjectName("Programación I")
                .teacherName("Dr. García")
                .groupType("REGULAR")
                .groupStatus("ACTIVE")
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        successResponseDto = EnrollmentResponseDto.builder()
                .enrollmentId(1L)
                .success(true)
                .message("Inscripción realizada exitosamente. Estado de pago: PENDIENTE")
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    // ========== TESTS DE INSCRIPCIÓN DE ESTUDIANTE ==========

    @Test
    @DisplayName("Debe inscribir estudiante exitosamente")
    void shouldEnrollStudentSuccessfully() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(testActiveGroup));
        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(1L, 1L)).thenReturn(false);
        when(enrollmentMapper.toEntity(validCreateDto, testStudent, testActiveGroup))
                .thenReturn(testEnrollment);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);

        // Aquí está la corrección - usar eq() para los valores literales
        when(enrollmentMapper.toResponseDto(eq(testEnrollment), eq(true), any(String.class)))
                .thenReturn(successResponseDto);

        // When
        EnrollmentResponseDto result = enrollmentService.enrollStudent(validCreateDto);

        // Then
        assertThat(result).isEqualTo(successResponseDto);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Debe fallar inscripción cuando estudiante intenta inscribir a otro")
    void shouldFailEnrollmentWhenStudentTriesToEnrollAnother() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        validCreateDto.setStudentId(2L); // Intentando inscribir a otro estudiante

        // When/Then
        assertThatThrownBy(() -> enrollmentService.enrollStudent(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Solo puede inscribirse a sí mismo");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar inscripción cuando grupo no está activo")
    void shouldFailEnrollmentWhenGroupNotActive() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseGroupRepository.findById(3L)).thenReturn(Optional.of(testPlannedGroup));

        validCreateDto.setCourseGroupId(3L);

        // When/Then
        assertThatThrownBy(() -> enrollmentService.enrollStudent(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Solo se puede inscribir en grupos activos");
    }

    @Test
    @DisplayName("Debe fallar inscripción cuando grupo está lleno")
    void shouldFailEnrollmentWhenGroupIsFull() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseGroupRepository.findById(2L)).thenReturn(Optional.of(testFullGroup));

        validCreateDto.setCourseGroupId(2L);

        // When/Then
        assertThatThrownBy(() -> enrollmentService.enrollStudent(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El grupo ha alcanzado su capacidad máxima");
    }

    @Test
    @DisplayName("Debe fallar inscripción cuando estudiante ya está inscrito")
    void shouldFailEnrollmentWhenAlreadyEnrolled() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(testActiveGroup));
        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(1L, 1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> enrollmentService.enrollStudent(validCreateDto))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessage("El estudiante ya está inscrito en este grupo");
    }

    @Test
    @DisplayName("Debe fallar inscripción cuando asignatura no es de la carrera del estudiante")
    void shouldFailEnrollmentWhenSubjectNotFromStudentMajor() {
        // Given
        Subject otherMajorSubject = Subject.builder()
                .name("Química")
                .major("Ingeniería Química")
                .courseYear(1)
                .build();

        CourseGroup otherMajorGroup = CourseGroup.builder()
                .subject(otherMajorSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .maxCapacity(30)
                .enrollments(new HashSet<>())
                .build();

        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(otherMajorGroup));

        // When/Then
        assertThatThrownBy(() -> enrollmentService.enrollStudent(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Solo puede inscribirse en asignaturas de su carrera: Ingeniería Informática");
    }

    @Test
    @DisplayName("Debe manejar error de integridad al crear inscripción duplicada")
    void shouldHandleDataIntegrityViolationOnDuplicateEnrollment() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(testActiveGroup));
        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(1L, 1L)).thenReturn(false);
        when(enrollmentMapper.toEntity(any(), any(), any())).thenReturn(testEnrollment);
        when(enrollmentRepository.save(any())).thenThrow(
                new DataIntegrityViolationException("Duplicate entry"));

        // When/Then
        assertThatThrownBy(() -> enrollmentService.enrollStudent(validCreateDto))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessage("Ya existe una inscripción para este estudiante en este grupo");
    }

    // ========== TESTS DE CONSULTA DE INSCRIPCIONES ==========

    @Test
    @DisplayName("Debe obtener inscripciones del estudiante")
    void shouldGetStudentEnrollments() {
        // Given
        testStudent.getEnrollments().add(testEnrollment);
        List<EnrollmentSummaryDto> expectedDtos = Arrays.asList(testEnrollmentSummaryDto);

        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(enrollmentMapper.toSummaryDtoList(any())).thenReturn(expectedDtos);

        // When
        List<EnrollmentSummaryDto> result = enrollmentService.getStudentEnrollments(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testEnrollmentSummaryDto);
    }

    @Test
    @DisplayName("Debe fallar al obtener inscripciones de otro estudiante")
    void shouldFailGetEnrollmentsOfAnotherStudent() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> enrollmentService.getStudentEnrollments(2L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No puede acceder a información de otros estudiantes");
    }

    @Test
    @DisplayName("Debe obtener inscripción por ID")
    void shouldGetEnrollmentById() {
        // Given
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(enrollmentMapper.toDto(testEnrollment)).thenReturn(testEnrollmentDto);

        // When
        EnrollmentDto result = enrollmentService.getEnrollmentById(1L);

        // Then
        assertThat(result).isEqualTo(testEnrollmentDto);
    }

    // ========== TESTS DE CANCELACIÓN ==========

    @Test
    @DisplayName("Debe cancelar inscripción exitosamente")
    void shouldCancelEnrollmentSuccessfully() {
        // Given
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When
        EnrollmentResponseDto result = enrollmentService.cancelEnrollment(1L);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Inscripción cancelada exitosamente");
        verify(enrollmentRepository).delete(testEnrollment);
    }

    @Test
    @DisplayName("Debe fallar cancelación cuando pago está confirmado")
    void shouldFailCancellationWhenPaymentConfirmed() {
        // Given
        testEnrollment.setPaymentStatus(PaymentStatus.PAID);
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se puede cancelar una inscripción con pago confirmado");

        verify(enrollmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe fallar cancelación cuando grupo está cerrado")
    void shouldFailCancellationWhenGroupClosed() {
        // Given
        testActiveGroup.setStatus(CourseGroupStatus.CLOSED);
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se puede cancelar la inscripción de un grupo cerrado");
    }

    // ========== TESTS DE OPERACIONES ADMINISTRATIVAS ==========

    @Test
    @DisplayName("Debe obtener todas las inscripciones como admin")
    void shouldGetAllEnrollmentsAsAdmin() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Enrollment> enrollments = Arrays.asList(testEnrollment);
        Page<Enrollment> enrollmentPage = new PageImpl<>(enrollments, pageable, 1);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(enrollmentRepository.findAll(pageable)).thenReturn(enrollmentPage);
        when(enrollmentMapper.toDto(testEnrollment)).thenReturn(testEnrollmentDto);

        // When
        Page<EnrollmentDto> result = enrollmentService.getAllEnrollments(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testEnrollmentDto);
    }

    @Test
    @DisplayName("Debe actualizar estado de pago como admin")
    void shouldUpdatePaymentStatusAsAdmin() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);
        when(enrollmentMapper.toDto(testEnrollment)).thenReturn(testEnrollmentDto);

        // When
        EnrollmentDto result = enrollmentService.updatePaymentStatus(1L, PaymentStatus.PAID);

        // Then
        verify(enrollmentMapper).updatePaymentStatus(testEnrollment, PaymentStatus.PAID);
        verify(enrollmentRepository).save(testEnrollment);
    }

    @Test
    @DisplayName("Debe obtener inscripciones por grupo como admin")
    void shouldGetEnrollmentsByGroupAsAdmin() {
        // Given
        testActiveGroup.getEnrollments().add(testEnrollment);
        List<EnrollmentDto> expectedDtos = Arrays.asList(testEnrollmentDto);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(testActiveGroup));
        when(enrollmentMapper.toDtoList(any())).thenReturn(expectedDtos);

        // When
        List<EnrollmentDto> result = enrollmentService.getEnrollmentsByGroup(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testEnrollmentDto);
    }

    @Test
    @DisplayName("Debe eliminar inscripción forzosamente como admin")
    void shouldForceDeleteEnrollmentAsAdmin() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));

        // When
        enrollmentService.forceDeleteEnrollment(1L, "Solicitud del estudiante");

        // Then
        verify(enrollmentRepository).delete(testEnrollment);
    }

    @Test
    @DisplayName("Debe fallar operaciones admin sin permisos")
    void shouldFailAdminOperationsWithoutPermission() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(false);
        when(sessionUtils.getCurrentUserEmail()).thenReturn("student@test.com");

        // When/Then
        assertThatThrownBy(() -> enrollmentService.getAllEnrollments(PageRequest.of(0, 10)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");

        assertThatThrownBy(() -> enrollmentService.updatePaymentStatus(1L, PaymentStatus.PAID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");
    }

    // ========== TESTS DE MÉTODOS AUXILIARES ==========

    @Test
    @DisplayName("Debe verificar si estudiante puede inscribirse - caso positivo")
    void shouldReturnTrueWhenStudentCanEnroll() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(testActiveGroup));
        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(1L, 1L)).thenReturn(false);

        // When
        boolean canEnroll = enrollmentService.canEnrollInGroup(1L, 1L);

        // Then
        assertThat(canEnroll).isTrue();
    }

    @Test
    @DisplayName("Debe verificar si estudiante puede inscribirse - caso negativo")
    void shouldReturnFalseWhenStudentCannotEnroll() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseGroupRepository.findById(2L)).thenReturn(Optional.of(testFullGroup));

        // When
        boolean canEnroll = enrollmentService.canEnrollInGroup(1L, 2L);

        // Then
        assertThat(canEnroll).isFalse();
    }

    @Test
    @DisplayName("Debe obtener estadísticas de inscripciones del estudiante")
    void shouldGetStudentEnrollmentStats() {
        // Given
        Enrollment paidEnrollment = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testActiveGroup)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        testStudent.getEnrollments().addAll(Arrays.asList(testEnrollment, paidEnrollment));

        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        // When
        EnrollmentStatsDto stats = enrollmentService.getStudentEnrollmentStats(1L);

        // Then
        assertThat(stats.getStudentId()).isEqualTo(1L);
        assertThat(stats.getTotalEnrollments()).isEqualTo(2);
        assertThat(stats.getActiveEnrollments()).isEqualTo(2);
        assertThat(stats.getPaidEnrollments()).isEqualTo(1);
        assertThat(stats.getPendingPayments()).isEqualTo(1);
    }
}
