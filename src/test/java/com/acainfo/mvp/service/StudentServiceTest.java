package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.auth.ChangePasswordDto;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.student.CreateStudentDto;
import com.acainfo.mvp.dto.student.EnrollmentSummaryDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.dto.subject.SubjectDto;
import com.acainfo.mvp.exception.auth.EmailAlreadyExistsException;
import com.acainfo.mvp.exception.auth.InvalidCredentialsException;
import com.acainfo.mvp.exception.auth.PasswordMismatchException;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.mapper.SubjectMapper;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.Enrollment;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.PaymentStatus;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.SubjectRepository;
import com.acainfo.mvp.repository.TeacherRepository;
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

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para StudentService.
 * Verifica operaciones sobre el perfil del estudiante y consultas académicas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Tests")
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private SubjectMapper subjectMapper;
    @Mock
    private SessionUtils sessionUtils;
    @Mock
    private EnrollmentService enrollmentService;

    private StudentService studentService;

    private Student testStudent;
    private Student otherStudent;
    private StudentDto testStudentDto;
    private CreateStudentDto validCreateDto;
    private Subject testSubject1;
    private Subject testSubject2;
    private SubjectDto testSubjectDto1;
    private SubjectDto testSubjectDto2;
    private EnrollmentSummaryDto testEnrollmentSummaryDto;
    private ChangePasswordDto validChangePasswordDto;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                studentRepository,
                teacherRepository,
                subjectRepository,
                studentMapper,
                subjectMapper,
                sessionUtils,
                enrollmentService
        );

        // Configurar datos de prueba
        testStudent = Student.builder()
                .name("Juan Pérez")
                .email("juan@estudiante.edu")
                .password("$2a$10$encoded_password")
                .major("Ingeniería Informática")
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testStudent, "id", 1L);

        otherStudent = Student.builder()
                .name("María García")
                .email("maria@estudiante.edu")
                .password("$2a$10$encoded_password")
                .major("Ingeniería Informática")
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(otherStudent, "id", 2L);

        testStudentDto = StudentDto.builder()
                .name("Juan Pérez")
                .email("juan@estudiante.edu")
                .major("Ingeniería Informática")
                .build();

        validCreateDto = CreateStudentDto.builder()
                .name("Nuevo Estudiante")
                .email("nuevo@estudiante.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();

        testSubject1 = Subject.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();
        ReflectionTestUtils.setField(testSubject1, "id", 1L);

        testSubject2 = Subject.builder()
                .name("Base de Datos")
                .major("Ingeniería Informática")
                .courseYear(2)
                .build();
        ReflectionTestUtils.setField(testSubject2, "id", 2L);

        testSubjectDto1 = SubjectDto.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();

        testSubjectDto2 = SubjectDto.builder()
                .name("Base de Datos")
                .major("Ingeniería Informática")
                .courseYear(2)
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

        validChangePasswordDto = ChangePasswordDto.builder()
                .currentPassword("currentPassword123")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();
    }

    // ========== TESTS DE PERFIL DE ESTUDIANTE ==========

    @Test
    @DisplayName("Debe obtener mi perfil como estudiante")
    void shouldGetMyProfile() {
        // Given
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isStudent()).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDto);

        // When
        StudentDto result = studentService.getMyProfile();

        // Then
        assertThat(result).isEqualTo(testStudentDto);
        verify(studentRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe fallar al obtener perfil si no es estudiante")
    void shouldFailGetMyProfileIfNotStudent() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> studentService.getMyProfile())
                .isInstanceOf(ValidationException.class)
                .hasMessage("Esta operación es solo para estudiantes");
    }

    @Test
    @DisplayName("Debe obtener estudiante por ID como el mismo estudiante")
    void shouldGetStudentByIdAsSelf() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDto);

        // When
        StudentDto result = studentService.getStudentById(1L);

        // Then
        assertThat(result).isEqualTo(testStudentDto);
    }

    @Test
    @DisplayName("Debe fallar al obtener otro estudiante")
    void shouldFailGetOtherStudentProfile() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> studentService.getStudentById(2L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No puede acceder a información de otros estudiantes");
    }

    @Test
    @DisplayName("Debe obtener cualquier estudiante como admin")
    void shouldGetAnyStudentAsAdmin() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(false);
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(otherStudent));
        when(studentMapper.toDto(otherStudent)).thenReturn(testStudentDto);

        // When
        StudentDto result = studentService.getStudentById(2L);

        // Then
        assertThat(result).isEqualTo(testStudentDto);
    }

    @Test
    @DisplayName("Debe actualizar perfil propio")
    void shouldUpdateOwnProfile() {
        // Given
        StudentDto updateDto = StudentDto.builder()
                .name("Juan Pérez Actualizado")
                .major("Ingeniería de Software")
                .build();

        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDto);

        // When
        StudentDto result = studentService.updateProfile(1L, updateDto);

        // Then
        assertThat(result).isEqualTo(testStudentDto);
        verify(studentMapper).updateBasicInfo(testStudent, updateDto);
        verify(studentRepository).save(testStudent);
    }

    @Test
    @DisplayName("Debe cambiar contraseña exitosamente")
    void shouldChangePasswordSuccessfully() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentMapper.passwordMatches("currentPassword123", testStudent.getPassword()))
                .thenReturn(true);
        when(studentMapper.encodePassword("newPassword123")).thenReturn("$2a$10$new_encoded");
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        // When
        ApiResponseDto<Void> result = studentService.changePassword(1L, validChangePasswordDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Contraseña actualizada exitosamente");
        verify(studentRepository).save(testStudent);
    }

    @Test
    @DisplayName("Debe fallar cambio de contraseña si no coinciden")
    void shouldFailChangePasswordIfMismatch() {
        // Given
        validChangePasswordDto.setConfirmPassword("differentPassword");
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> studentService.changePassword(1L, validChangePasswordDto))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessage("Las contraseñas nuevas no coinciden");
    }

    @Test
    @DisplayName("Debe fallar cambio de contraseña si la actual es incorrecta")
    void shouldFailChangePasswordIfCurrentIncorrect() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentMapper.passwordMatches("currentPassword123", testStudent.getPassword()))
                .thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> studentService.changePassword(1L, validChangePasswordDto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("La contraseña actual es incorrecta");
    }

    // ========== TESTS DE CONSULTAS ACADÉMICAS ==========

    @Test
    @DisplayName("Debe obtener asignaturas de mi carrera")
    void shouldGetMyMajorSubjects() {
        // Given
        List<Subject> subjects = Arrays.asList(testSubject1, testSubject2);
        List<SubjectDto> expectedDtos = Arrays.asList(testSubjectDto1, testSubjectDto2);

        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isStudent()).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findByMajor("Ingeniería Informática")).thenReturn(subjects);
        when(subjectMapper.toDtoList(subjects)).thenReturn(expectedDtos);

        // When
        List<SubjectDto> result = studentService.getMyMajorSubjects();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testSubjectDto1, testSubjectDto2);
    }

    @Test
    @DisplayName("Debe obtener asignaturas de mi carrera por año")
    void shouldGetMyMajorSubjectsByYear() {
        // Given
        List<Subject> allSubjects = Arrays.asList(testSubject1, testSubject2);
        List<SubjectDto> expectedDtos = Arrays.asList(testSubjectDto1);

        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isStudent()).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findByMajor("Ingeniería Informática")).thenReturn(allSubjects);
        when(subjectMapper.toDtoList(any())).thenReturn(expectedDtos);

        // When
        List<SubjectDto> result = studentService.getMyMajorSubjectsByYear(1);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Debe validar año de curso en consulta")
    void shouldValidateCourseYearInQuery() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> studentService.getMyMajorSubjectsByYear(0))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El año de curso debe estar entre 1 y 6");

        assertThatThrownBy(() -> studentService.getMyMajorSubjectsByYear(7))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El año de curso debe estar entre 1 y 6");
    }

    @Test
    @DisplayName("Debe obtener mis inscripciones")
    void shouldGetMyEnrollments() {
        // Given
        List<EnrollmentSummaryDto> expectedEnrollments = Arrays.asList(testEnrollmentSummaryDto);

        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isStudent()).thenReturn(true);
        when(enrollmentService.getStudentEnrollments(1L)).thenReturn(expectedEnrollments);

        // When
        List<EnrollmentSummaryDto> result = studentService.getMyEnrollments();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testEnrollmentSummaryDto);
    }

    @Test
    @DisplayName("Debe obtener mis estadísticas")
    void shouldGetMyStats() {
        // Given
        // Configurar inscripciones del estudiante
        CourseGroup activeGroup = CourseGroup.builder()
                .subject(testSubject1)
                .status(CourseGroupStatus.ACTIVE)
                .build();
        Enrollment enrollment = Enrollment.builder()
                .courseGroup(activeGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        testStudent.getEnrollments().add(enrollment);

        EnrollmentStatsDto enrollmentStats = EnrollmentStatsDto.builder()
                .studentId(1L)
                .totalEnrollments(1)
                .activeEnrollments(1)
                .pendingPayments(1)
                .build();

        List<Subject> majorSubjects = Arrays.asList(testSubject1, testSubject2);

        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isStudent()).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(enrollmentService.getStudentEnrollmentStats(1L)).thenReturn(enrollmentStats);
        when(subjectRepository.findByMajor("Ingeniería Informática")).thenReturn(majorSubjects);

        // When
        StudentStatsDto stats = studentService.getMyStats();

        // Then
        assertThat(stats.getStudentId()).isEqualTo(1L);
        assertThat(stats.getStudentName()).isEqualTo("Juan Pérez");
        assertThat(stats.getMajor()).isEqualTo("Ingeniería Informática");
        assertThat(stats.getTotalEnrollments()).isEqualTo(1);
        assertThat(stats.getActiveEnrollments()).isEqualTo(1);
        assertThat(stats.getPendingPayments()).isEqualTo(1);
        assertThat(stats.getTotalSubjectsInMajor()).isEqualTo(2);
        assertThat(stats.getEnrolledSubjects()).isEqualTo(1);
        assertThat(stats.getRemainingSubjects()).isEqualTo(1);
    }

    // ========== TESTS DE OPERACIONES ADMINISTRATIVAS ==========

    @Test
    @DisplayName("Debe obtener todos los estudiantes como admin")
    void shouldGetAllStudentsAsAdmin() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Student> students = Arrays.asList(testStudent, otherStudent);
        Page<Student> studentPage = new PageImpl<>(students, pageable, 2);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.findAll(pageable)).thenReturn(studentPage);
        when(studentMapper.toDto(any(Student.class))).thenReturn(testStudentDto);

        // When
        Page<StudentDto> result = studentService.getAllStudents(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Debe crear estudiante como admin")
    void shouldCreateStudentAsAdmin() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.existsByEmail(validCreateDto.getEmail())).thenReturn(false);
        when(teacherRepository.existsByEmail(validCreateDto.getEmail())).thenReturn(false);
        when(studentMapper.toEntity(validCreateDto)).thenReturn(testStudent);
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDto);

        // When
        StudentDto result = studentService.createStudent(validCreateDto);

        // Then
        assertThat(result).isEqualTo(testStudentDto);
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("Debe fallar al crear estudiante con email existente")
    void shouldFailCreateStudentWithExistingEmail() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.existsByEmail(validCreateDto.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> studentService.createStudent(validCreateDto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("El email ya está registrado");
    }

    @Test
    @DisplayName("Debe actualizar estudiante como admin")
    void shouldUpdateStudentAsAdmin() {
        // Given
        StudentDto updateDto = StudentDto.builder()
                .name("Nombre Actualizado")
                .major("Nueva Carrera")
                .build();

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
        when(studentMapper.toDto(testStudent)).thenReturn(testStudentDto);

        // When
        StudentDto result = studentService.updateStudent(1L, updateDto);

        // Then
        assertThat(result).isEqualTo(testStudentDto);
        verify(studentMapper).updateBasicInfo(testStudent, updateDto);
        verify(studentRepository).save(testStudent);
    }

    @Test
    @DisplayName("Debe eliminar estudiante sin inscripciones activas")
    void shouldDeleteStudentWithoutActiveEnrollments() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        // When
        studentService.deleteStudent(1L);

        // Then
        verify(studentRepository).delete(testStudent);
    }

    @Test
    @DisplayName("Debe fallar al eliminar estudiante con inscripciones activas")
    void shouldFailDeleteStudentWithActiveEnrollments() {
        // Given
        CourseGroup activeGroup = CourseGroup.builder()
                .status(CourseGroupStatus.ACTIVE)
                .build();
        Enrollment activeEnrollment = Enrollment.builder()
                .courseGroup(activeGroup)
                .build();
        testStudent.getEnrollments().add(activeEnrollment);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        // When/Then
        assertThatThrownBy(() -> studentService.deleteStudent(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se puede eliminar el estudiante porque tiene inscripciones activas");
    }

    @Test
    @DisplayName("Debe buscar estudiantes por carrera")
    void shouldGetStudentsByMajor() {
        // Given
        List<Student> allStudents = Arrays.asList(testStudent, otherStudent);
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.findAll()).thenReturn(allStudents);
        when(studentMapper.toDto(any(Student.class))).thenReturn(testStudentDto);

        // When
        List<StudentDto> result = studentService.getStudentsByMajor("Ingeniería Informática");

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Debe obtener estadísticas administrativas")
    void shouldGetAdminStudentStats() {
        // Given
        // Configurar estudiantes con inscripciones
        CourseGroup activeGroup = CourseGroup.builder()
                .status(CourseGroupStatus.ACTIVE)
                .build();
        Enrollment enrollment1 = Enrollment.builder()
                .courseGroup(activeGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        testStudent.getEnrollments().add(enrollment1);

        List<Student> allStudents = Arrays.asList(testStudent, otherStudent);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(studentRepository.findAll()).thenReturn(allStudents);

        // When
        AdminStudentStatsDto stats = studentService.getAdminStudentStats();

        // Then
        assertThat(stats.getTotalStudents()).isEqualTo(2);
        assertThat(stats.getStudentsByMajor()).containsEntry("Ingeniería Informática", 2L);
        assertThat(stats.getTotalActiveEnrollments()).isEqualTo(1);
        assertThat(stats.getTotalPendingPayments()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe fallar operaciones admin sin permisos")
    void shouldFailAdminOperationsWithoutPermission() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(false);
        when(sessionUtils.getCurrentUserEmail()).thenReturn("student@test.com");

        // When/Then
        assertThatThrownBy(() -> studentService.getAllStudents(PageRequest.of(0, 10)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");

        assertThatThrownBy(() -> studentService.createStudent(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");

        assertThatThrownBy(() -> studentService.deleteStudent(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");
    }

    @Test
    @DisplayName("Debe manejar error de integridad al actualizar perfil")
    void shouldHandleDataIntegrityViolationOnUpdate() {
        // Given
        StudentDto updateDto = StudentDto.builder()
                .name("Nombre Actualizado")
                .build();

        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.save(any(Student.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        // When/Then
        assertThatThrownBy(() -> studentService.updateProfile(1L, updateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Error al actualizar el perfil");
    }

    @Test
    @DisplayName("Debe manejar estudiante no encontrado")
    void shouldHandleStudentNotFound() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> studentService.getStudentById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Estudiante no encontrado con ID: 1");
    }
}