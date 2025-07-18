package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.auth.ChangePasswordDto;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.coursegroup.CourseGroupDto;
import com.acainfo.mvp.dto.coursegroup.GroupSessionDto;
import com.acainfo.mvp.dto.teacher.CreateTeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherScheduleDto;
import com.acainfo.mvp.exception.auth.EmailAlreadyExistsException;
import com.acainfo.mvp.exception.auth.PasswordMismatchException;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.CourseGroupMapper;
import com.acainfo.mvp.mapper.TeacherMapper;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.DayOfWeek;
import com.acainfo.mvp.repository.GroupSessionRepository;
import com.acainfo.mvp.repository.StudentRepository;
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

import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para TeacherService.
 * Verifica operaciones sobre el perfil del profesor, horarios y gestión administrativa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherService Tests")
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private GroupSessionRepository groupSessionRepository;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private SessionUtils sessionUtils;
    @Mock
    private CourseGroupService courseGroupService;
    @Mock
    private CourseGroupMapper courseGroupMapper;

    private TeacherService teacherService;

    private Teacher testTeacher;
    private Teacher otherTeacher;
    private TeacherDto testTeacherDto;
    private CreateTeacherDto validCreateDto;
    private Subject testSubject;
    private CourseGroup testActiveGroup;
    private CourseGroup testPlannedGroup;
    private GroupSession testSession1;
    private GroupSession testSession2;
    private GroupSessionDto testSessionDto1;
    private GroupSessionDto testSessionDto2;
    private CourseGroupDto testGroupDto;
    private TeacherScheduleDto testScheduleDto;
    private ChangePasswordDto validChangePasswordDto;

    @BeforeEach
    void setUp() {

        teacherService = new TeacherService(
                teacherRepository,
                studentRepository,
                groupSessionRepository,
                teacherMapper,
                courseGroupMapper,
                sessionUtils,
                courseGroupService
        );

        // Configurar datos de prueba
        testTeacher = Teacher.builder()
                .name("Dr. García López")
                .email("garcia@universidad.edu")
                .password("$2a$10$encoded_password")
                .courseGroups(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testTeacher, "id", 1L);

        otherTeacher = Teacher.builder()
                .name("Dra. Martínez")
                .email("martinez@universidad.edu")
                .password("$2a$10$encoded_password")
                .courseGroups(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(otherTeacher, "id", 2L);

        testTeacherDto = TeacherDto.builder()
                .name("Dr. García López")
                .email("garcia@universidad.edu")
                .build();

        validCreateDto = CreateTeacherDto.builder()
                .name("Nuevo Profesor")
                .email("nuevo@universidad.edu")
                .password("password123")
                .build();

        testSubject = Subject.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();
        ReflectionTestUtils.setField(testSubject, "id", 1L);

        testActiveGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testActiveGroup, "id", 1L);

        testPlannedGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testPlannedGroup, "id", 2L);

        testSession1 = GroupSession.builder()
                .courseGroup(testActiveGroup)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();
        ReflectionTestUtils.setField(testSession1, "id", 1L);

        testSession2 = GroupSession.builder()
                .courseGroup(testActiveGroup)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();
        ReflectionTestUtils.setField(testSession2, "id", 2L);

        testSessionDto1 = GroupSessionDto.builder()
                .id(1L)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();

        testSessionDto2 = GroupSessionDto.builder()
                .id(2L)
                .dayOfWeek("WEDNESDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();

        testGroupDto = CourseGroupDto.builder()
                .subjectId(1L)
                .subjectName("Programación I")
                .teacherId(1L)
                .teacherName("Dr. García López")
                .status(CourseGroupStatus.ACTIVE)
                .enrolledStudents(5)
                .build();

        testScheduleDto = TeacherScheduleDto.builder()
                .teacherId(1L)
                .teacherName("Dr. García López")
                .schedule(Arrays.asList())
                .build();

        validChangePasswordDto = ChangePasswordDto.builder()
                .currentPassword("currentPassword123")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        // Agregar grupos al profesor
        testTeacher.getCourseGroups().add(testActiveGroup);
        testTeacher.getCourseGroups().add(testPlannedGroup);
    }

    // ========== TESTS DE PERFIL DE PROFESOR ==========

    @Test
    @DisplayName("Debe obtener mi perfil como profesor")
    void shouldGetMyProfile() {
        // Given
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(teacherMapper.toDto(testTeacher)).thenReturn(testTeacherDto);

        // When
        TeacherDto result = teacherService.getMyProfile();

        // Then
        assertThat(result).isEqualTo(testTeacherDto);
        verify(teacherRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe fallar al obtener perfil si no es profesor")
    void shouldFailGetMyProfileIfNotTeacher() {
        // Given
        when(sessionUtils.isTeacher()).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> teacherService.getMyProfile())
                .isInstanceOf(ValidationException.class)
                .hasMessage("Esta operación es solo para profesores");
    }

    @Test
    @DisplayName("Debe obtener profesor por ID como el mismo profesor")
    void shouldGetTeacherByIdAsSelf() {
        // Given
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(teacherMapper.toDto(testTeacher)).thenReturn(testTeacherDto);

        // When
        TeacherDto result = teacherService.getTeacherById(1L);

        // Then
        assertThat(result).isEqualTo(testTeacherDto);
    }

    @Test
    @DisplayName("Debe fallar al obtener otro profesor")
    void shouldFailGetOtherTeacherProfile() {
        // Given
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> teacherService.getTeacherById(2L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No puede acceder a información de otros profesores");
    }

    @Test
    @DisplayName("Debe actualizar perfil propio")
    void shouldUpdateOwnProfile() {
        // Given
        TeacherDto updateDto = TeacherDto.builder()
                .name("Dr. García López Actualizado")
                .build();

        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);
        when(teacherMapper.toDto(testTeacher)).thenReturn(testTeacherDto);

        // When
        TeacherDto result = teacherService.updateProfile(1L, updateDto);

        // Then
        assertThat(result).isEqualTo(testTeacherDto);
        verify(teacherMapper).updateBasicInfo(testTeacher, updateDto);
        verify(teacherRepository).save(testTeacher);
    }

    @Test
    @DisplayName("Debe cambiar contraseña exitosamente")
    void shouldChangePasswordSuccessfully() {
        // Given
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(teacherMapper.passwordMatches("currentPassword123", testTeacher.getPassword()))
                .thenReturn(true);
        when(teacherMapper.encodePassword("newPassword123")).thenReturn("$2a$10$new_encoded");
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);

        // When
        ApiResponseDto<Void> result = teacherService.changePassword(1L, validChangePasswordDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Contraseña actualizada exitosamente");
        verify(teacherRepository).save(testTeacher);
    }

    @Test
    @DisplayName("Debe fallar cambio de contraseña si no coinciden")
    void shouldFailChangePasswordIfMismatch() {
        // Given
        validChangePasswordDto.setConfirmPassword("differentPassword");
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> teacherService.changePassword(1L, validChangePasswordDto))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessage("Las contraseñas nuevas no coinciden");
    }

    // ========== TESTS DE HORARIOS Y GRUPOS ==========

    @Test
    @DisplayName("Debe obtener mi horario completo")
    void shouldGetMySchedule() {
        // Given
        List<GroupSession> sessions = Arrays.asList(testSession1, testSession2);

        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(groupSessionRepository.findByTeacherId(1L)).thenReturn(sessions);
        when(teacherMapper.toScheduleDto(testTeacher, sessions)).thenReturn(testScheduleDto);

        // When
        TeacherScheduleDto result = teacherService.getMySchedule();

        // Then
        assertThat(result).isEqualTo(testScheduleDto);
        verify(groupSessionRepository).findByTeacherId(1L);
    }

    @Test
    @DisplayName("Debe obtener horario por día")
    void shouldGetScheduleByDay() {
        // Given
        List<GroupSession> allSessions = Arrays.asList(testSession1, testSession2);

        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(groupSessionRepository.findByTeacherId(1L)).thenReturn(allSessions);
        when(courseGroupMapper.toSessionDto(testSession1)).thenReturn(testSessionDto1);

        // When
        List<GroupSessionDto> result = teacherService.getMyScheduleByDay("MONDAY");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testSessionDto1);
    }

    @Test
    @DisplayName("Debe validar día de semana inválido")
    void shouldValidateInvalidDayOfWeek() {
        // Given
        when(sessionUtils.isTeacher()).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> teacherService.getMyScheduleByDay("INVALID"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Día de la semana inválido: INVALID");
    }

    @Test
    @DisplayName("Debe obtener mis grupos")
    void shouldGetMyGroups() {
        // Given
        List<CourseGroupDto> expectedGroups = Arrays.asList(testGroupDto);

        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(courseGroupService.getTeacherGroups(1L)).thenReturn(expectedGroups);

        // When
        List<CourseGroupDto> result = teacherService.getMyGroups();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testGroupDto);
    }

    @Test
    @DisplayName("Debe obtener mis estadísticas")
    void shouldGetMyStats() {
        // Given
        // Agregar inscripciones a un grupo
        for (int i = 0; i < 5; i++) {
            testActiveGroup.getEnrollments().add(mock(com.acainfo.mvp.model.Enrollment.class));
        }

        List<GroupSession> sessions = Arrays.asList(testSession1, testSession2);

        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(groupSessionRepository.findByTeacherId(1L)).thenReturn(sessions);

        // When
        TeacherStatsDto stats = teacherService.getMyStats();

        // Then
        assertThat(stats.getTeacherId()).isEqualTo(1L);
        assertThat(stats.getTeacherName()).isEqualTo("Dr. García López");
        assertThat(stats.getTotalGroups()).isEqualTo(2);
        assertThat(stats.getActiveGroups()).isEqualTo(1);
        assertThat(stats.getPlannedGroups()).isEqualTo(1);
        assertThat(stats.getTotalStudents()).isEqualTo(5);
        assertThat(stats.getWeeklyHours()).isEqualTo(4); // 2 sesiones de 2 horas
        assertThat(stats.getUniqueSubjects()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe verificar disponibilidad - sin conflictos")
    void shouldCheckAvailabilityNoConflict() {
        // Given
        List<GroupSession> sessions = Arrays.asList(testSession1); // Lunes 10-12

        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(groupSessionRepository.findByTeacherId(1L)).thenReturn(sessions);

        // When - Verificar miércoles (sin conflicto)
        boolean available = teacherService.isAvailable(1L, "WEDNESDAY",
                LocalTime.of(14, 0), LocalTime.of(16, 0));

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Debe verificar disponibilidad - con conflicto")
    void shouldCheckAvailabilityWithConflict() {
        // Given
        List<GroupSession> sessions = Arrays.asList(testSession1); // Lunes 10-12

        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(groupSessionRepository.findByTeacherId(1L)).thenReturn(sessions);

        // When - Verificar lunes en horario conflictivo
        boolean available = teacherService.isAvailable(1L, "MONDAY",
                LocalTime.of(11, 0), LocalTime.of(13, 0));

        // Then
        assertThat(available).isFalse();
    }

    // ========== TESTS DE OPERACIONES ADMINISTRATIVAS ==========

    @Test
    @DisplayName("Debe obtener todos los profesores como admin")
    void shouldGetAllTeachersAsAdmin() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Teacher> teachers = Arrays.asList(testTeacher, otherTeacher);
        Page<Teacher> teacherPage = new PageImpl<>(teachers, pageable, 2);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(teacherRepository.findAll(pageable)).thenReturn(teacherPage);
        when(teacherMapper.toDto(any(Teacher.class))).thenReturn(testTeacherDto);

        // When
        Page<TeacherDto> result = teacherService.getAllTeachers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Debe crear profesor como admin")
    void shouldCreateTeacherAsAdmin() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(teacherRepository.existsByEmail(validCreateDto.getEmail())).thenReturn(false);
        when(studentRepository.existsByEmail(validCreateDto.getEmail())).thenReturn(false);
        when(teacherMapper.toEntity(validCreateDto)).thenReturn(testTeacher);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);
        when(teacherMapper.toDto(testTeacher)).thenReturn(testTeacherDto);

        // When
        TeacherDto result = teacherService.createTeacher(validCreateDto);

        // Then
        assertThat(result).isEqualTo(testTeacherDto);
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    @DisplayName("Debe fallar al crear profesor con email existente")
    void shouldFailCreateTeacherWithExistingEmail() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(teacherRepository.existsByEmail(validCreateDto.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> teacherService.createTeacher(validCreateDto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("El email ya está registrado");
    }

    @Test
    @DisplayName("Debe actualizar profesor como admin")
    void shouldUpdateTeacherAsAdmin() {
        // Given
        TeacherDto updateDto = TeacherDto.builder()
                .name("Nombre Actualizado")
                .build();

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);
        when(teacherMapper.toDto(testTeacher)).thenReturn(testTeacherDto);

        // When
        TeacherDto result = teacherService.updateTeacher(1L, updateDto);

        // Then
        assertThat(result).isEqualTo(testTeacherDto);
        verify(teacherMapper).updateBasicInfo(testTeacher, updateDto);
        verify(teacherRepository).save(testTeacher);
    }

    @Test
    @DisplayName("Debe eliminar profesor sin grupos activos")
    void shouldDeleteTeacherWithoutActiveGroups() {
        // Given
        // Solo tiene un grupo planificado
        testTeacher.getCourseGroups().clear();
        testTeacher.getCourseGroups().add(testPlannedGroup);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        // When
        teacherService.deleteTeacher(1L);

        // Then
        verify(teacherRepository).delete(testTeacher);
    }

    @Test
    @DisplayName("Debe fallar al eliminar profesor con grupos activos")
    void shouldFailDeleteTeacherWithActiveGroups() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

        // When/Then
        assertThatThrownBy(() -> teacherService.deleteTeacher(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se puede eliminar el profesor porque tiene grupos activos asignados");
    }

    @Test
    @DisplayName("Debe obtener profesores disponibles")
    void shouldGetAvailableTeachers() {
        // Given
        List<Teacher> allTeachers = Arrays.asList(testTeacher, otherTeacher);
        List<GroupSession> teacher1Sessions = Arrays.asList(testSession1); // Lunes 10-12
        List<GroupSession> teacher2Sessions = Arrays.asList(); // Sin sesiones

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(teacherRepository.findAll()).thenReturn(allTeachers);
        when(groupSessionRepository.findByTeacherId(1L)).thenReturn(teacher1Sessions);
        when(groupSessionRepository.findByTeacherId(2L)).thenReturn(teacher2Sessions);
        when(teacherMapper.toDto(any(Teacher.class))).thenReturn(testTeacherDto);

        // When - Buscar disponibles el miércoles
        List<TeacherDto> result = teacherService.getAvailableTeachers("WEDNESDAY",
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        // Then
        assertThat(result).hasSize(2); // Ambos están disponibles el miércoles
    }

    @Test
    @DisplayName("Debe obtener estadísticas administrativas")
    void shouldGetAdminTeacherStats() {
        // Given
        // Configurar profesores con diferentes estados
        for (int i = 0; i < 3; i++) {
            testActiveGroup.getEnrollments().add(mock(com.acainfo.mvp.model.Enrollment.class));
        }

        otherTeacher.getCourseGroups().clear(); // Sin grupos

        List<Teacher> allTeachers = Arrays.asList(testTeacher, otherTeacher);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(teacherRepository.findAll()).thenReturn(allTeachers);

        // When
        AdminTeacherStatsDto stats = teacherService.getAdminTeacherStats();

        // Then
        assertThat(stats.getTotalTeachers()).isEqualTo(2);
        assertThat(stats.getTeachersWithActiveGroups()).isEqualTo(1);
        assertThat(stats.getTeachersWithoutGroups()).isEqualTo(1);
        assertThat(stats.getTotalGroups()).isEqualTo(2);
        assertThat(stats.getTotalStudents()).isEqualTo(3);
        assertThat(stats.getAvgStudentsPerTeacher()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("Debe fallar operaciones admin sin permisos")
    void shouldFailAdminOperationsWithoutPermission() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(false);
        when(sessionUtils.getCurrentUserEmail()).thenReturn("teacher@test.com");

        // When/Then
        assertThatThrownBy(() -> teacherService.getAllTeachers(PageRequest.of(0, 10)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");

        assertThatThrownBy(() -> teacherService.createTeacher(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");

        assertThatThrownBy(() -> teacherService.deleteTeacher(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");
    }

    @Test
    @DisplayName("Debe manejar error de integridad al actualizar perfil")
    void shouldHandleDataIntegrityViolationOnUpdate() {
        // Given
        TeacherDto updateDto = TeacherDto.builder()
                .name("Nombre Actualizado")
                .build();

        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(teacherRepository.save(any(Teacher.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        // When/Then
        assertThatThrownBy(() -> teacherService.updateProfile(1L, updateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Error al actualizar el perfil");
    }

    @Test
    @DisplayName("Debe manejar profesor no encontrado")
    void shouldHandleTeacherNotFound() {
        // Given
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> teacherService.getTeacherById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profesor no encontrado con ID: 1");
    }

    @Test
    @DisplayName("Debe fallar al verificar disponibilidad de otro profesor")
    void shouldFailCheckAvailabilityOfOtherTeacher() {
        // Given
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> teacherService.isAvailable(2L, "MONDAY",
                LocalTime.of(10, 0), LocalTime.of(12, 0)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No puede consultar la disponibilidad de otros profesores");
    }
}
