package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.coursegroup.*;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.CourseGroupMapper;
import com.acainfo.mvp.mapper.GroupSessionMapper;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import com.acainfo.mvp.model.enums.DayOfWeek;
import com.acainfo.mvp.model.enums.PaymentStatus;
import com.acainfo.mvp.repository.*;
import com.acainfo.mvp.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CourseGroupService.
 * Verifica operaciones de consulta y gestión de grupos de curso.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CourseGroupService Tests")
class CourseGroupServiceTest {

    @Mock
    private CourseGroupRepository courseGroupRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private GroupSessionRepository groupSessionRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseGroupMapper courseGroupMapper;
    @Mock
    private GroupSessionMapper groupSessionMapper;
    @Mock
    private SessionUtils sessionUtils;

    private CourseGroupService courseGroupService;

    private Subject testSubject;
    private Teacher testTeacher;
    private Student testStudent;
    private CourseGroup testGroup1;
    private CourseGroup testGroup2;
    private CourseGroupDto testGroupDto1;
    private CourseGroupDto testGroupDto2;
    private CourseGroupDetailDto testGroupDetailDto;
    private GroupSession testSession1;
    private GroupSession testSession2;
    private CreateCourseGroupDto validCreateDto;
    private UpdateGroupStatusDto validStatusUpdateDto;
    private AssignTeacherDto validAssignTeacherDto;
    private CreateGroupSessionDto validSessionDto;

    @BeforeEach
    void setUp() {
        courseGroupService = new CourseGroupService(
                courseGroupRepository,
                subjectRepository,
                teacherRepository,
                groupSessionRepository,
                enrollmentRepository,
                courseGroupMapper,
                groupSessionMapper,
                sessionUtils
        );

        // Configurar datos de prueba
        testSubject = Subject.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();

        testTeacher = Teacher.builder()
                .name("Dr. García")
                .email("garcia@universidad.edu")
                .build();
        ReflectionTestUtils.setField(testTeacher, "id", 1L);

        testStudent = Student.builder()
                .name("Juan Pérez")
                .email("juan@estudiante.edu")
                .major("Ingeniería Informática")
                .build();

        testGroup1 = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .maxCapacity(30)
                .enrollments(new HashSet<>())
                .groupSessions(new HashSet<>())
                .build();

        testGroup2 = CourseGroup.builder()
                .subject(testSubject)
                .teacher(null)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.INTENSIVE)
                .price(BigDecimal.valueOf(250.00))
                .maxCapacity(25)
                .enrollments(new HashSet<>())
                .groupSessions(new HashSet<>())
                .build();

        testGroupDto1 = CourseGroupDto.builder()
                .subjectId(1L)
                .subjectName("Programación I")
                .teacherId(1L)
                .teacherName("Dr. García")
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .enrolledStudents(0)
                .build();

        testGroupDto2 = CourseGroupDto.builder()
                .subjectId(1L)
                .subjectName("Programación I")
                .teacherId(null)
                .teacherName("Sin asignar")
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.INTENSIVE)
                .price(BigDecimal.valueOf(250.00))
                .enrolledStudents(0)
                .build();

        testGroupDetailDto = CourseGroupDetailDto.builder()
                .subject(SubjectInfo.builder()
                        .id(1L)
                        .name("Programación I")
                        .major("Ingeniería Informática")
                        .courseYear(1)
                        .build())
                .teacher(TeacherInfo.builder()
                        .id(1L)
                        .name("Dr. García")
                        .email("garcia@universidad.edu")
                        .build())
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .enrolledStudents(0)
                .maxCapacity(30)
                .sessions(new ArrayList<>())
                .canEnroll(true)
                .enrollmentMessage("Plazas disponibles: 30 de 30")
                .build();

        testSession1 = GroupSession.builder()
                .courseGroup(testGroup1)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();

        testSession2 = GroupSession.builder()
                .courseGroup(testGroup1)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();

        validCreateDto = CreateCourseGroupDto.builder()
                .subjectId(1L)
                .teacherId(1L)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .sessions(new ArrayList<>())
                .build();

        validStatusUpdateDto = UpdateGroupStatusDto.builder()
                .status(CourseGroupStatus.ACTIVE)
                .reason("Grupo listo para iniciar")
                .build();

        validAssignTeacherDto = AssignTeacherDto.builder()
                .teacherId(1L)
                .build();

        validSessionDto = CreateGroupSessionDto.builder()
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();
    }

    // ========== TESTS DE CONSULTAS PÚBLICAS ==========

    @Test
    @DisplayName("Debe obtener grupos activos")
    void shouldGetActiveGroups() {
        // Given
        List<CourseGroup> activeGroups = Arrays.asList(testGroup1);
        List<CourseGroupDto> expectedDtos = Arrays.asList(testGroupDto1);

        when(courseGroupRepository.findByStatus(CourseGroupStatus.ACTIVE)).thenReturn(activeGroups);
        when(courseGroupMapper.toDtoList(activeGroups)).thenReturn(expectedDtos);

        // When
        List<CourseGroupDto> result = courseGroupService.getActiveGroups();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testGroupDto1);
        verify(courseGroupRepository).findByStatus(CourseGroupStatus.ACTIVE);
    }

    @Test
    @DisplayName("Debe obtener grupos por asignatura")
    void shouldGetGroupsBySubject() {
        // Given
        Long subjectId = 1L;
        List<CourseGroup> groups = Arrays.asList(testGroup1, testGroup2);
        List<CourseGroupDto> expectedDtos = Arrays.asList(testGroupDto1, testGroupDto2);

        when(subjectRepository.existsById(subjectId)).thenReturn(true);
        when(courseGroupRepository.findBySubjectId(subjectId)).thenReturn(groups);
        when(courseGroupMapper.toDtoList(groups)).thenReturn(expectedDtos);

        // When
        List<CourseGroupDto> result = courseGroupService.getGroupsBySubject(subjectId);

        // Then
        assertThat(result).hasSize(2);
        verify(subjectRepository).existsById(subjectId);
        verify(courseGroupRepository).findBySubjectId(subjectId);
    }

    @Test
    @DisplayName("Debe fallar al obtener grupos de asignatura inexistente")
    void shouldFailGetGroupsBySubjectWhenSubjectNotFound() {
        // Given
        Long subjectId = 99L;
        when(subjectRepository.existsById(subjectId)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> courseGroupService.getGroupsBySubject(subjectId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Asignatura no encontrada con ID: 99");

        verify(courseGroupRepository, never()).findBySubjectId(any());
    }

    @Test
    @DisplayName("Debe obtener detalle de grupo")
    void shouldGetGroupDetail() {
        // Given
        Long groupId = 1L;
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));
        when(courseGroupMapper.toDetailDto(testGroup1)).thenReturn(testGroupDetailDto);

        // When
        CourseGroupDetailDto result = courseGroupService.getGroupDetail(groupId);

        // Then
        assertThat(result).isEqualTo(testGroupDetailDto);
        assertThat(result.isCanEnroll()).isTrue();
        verify(courseGroupRepository).findById(groupId);
    }

    @Test
    @DisplayName("Debe obtener grupos disponibles")
    void shouldGetAvailableGroups() {
        // Given
        List<CourseGroup> availableGroups = Arrays.asList(testGroup1);
        List<CourseGroupDto> expectedDtos = Arrays.asList(testGroupDto1);

        when(courseGroupRepository.findAvailableGroups(30)).thenReturn(availableGroups);
        when(courseGroupMapper.toDtoList(availableGroups)).thenReturn(expectedDtos);

        // When
        List<CourseGroupDto> result = courseGroupService.getAvailableGroups();

        // Then
        assertThat(result).hasSize(1);
        verify(courseGroupRepository).findAvailableGroups(30);
    }

    @Test
    @DisplayName("Debe obtener grupos sin profesor")
    void shouldGetGroupsWithoutTeacher() {
        // Given
        List<CourseGroup> groups = Arrays.asList(testGroup2);
        List<CourseGroupDto> expectedDtos = Arrays.asList(testGroupDto2);

        when(courseGroupRepository.findByTeacherIsNull()).thenReturn(groups);
        when(courseGroupMapper.toDtoList(groups)).thenReturn(expectedDtos);

        // When
        List<CourseGroupDto> result = courseGroupService.getGroupsWithoutTeacher();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTeacherName()).isEqualTo("Sin asignar");
    }

    @Test
    @DisplayName("Debe verificar si estudiante puede inscribirse - caso positivo")
    void shouldReturnTrueWhenStudentCanEnroll() {
        // Given
        Long studentId = 1L;
        Long groupId = 1L;

        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));
        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(studentId, groupId))
                .thenReturn(false);

        // When
        boolean canEnroll = courseGroupService.canStudentEnroll(studentId, groupId);

        // Then
        assertThat(canEnroll).isTrue();
    }

    @Test
    @DisplayName("Debe verificar si estudiante puede inscribirse - grupo lleno")
    void shouldReturnFalseWhenGroupIsFull() {
        // Given
        Long studentId = 1L;
        Long groupId = 1L;

        // Llenar el grupo
        for (int i = 0; i < 30; i++) {
            testGroup1.getEnrollments().add(Enrollment.builder().build());
        }

        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));

        // When
        boolean canEnroll = courseGroupService.canStudentEnroll(studentId, groupId);

        // Then
        assertThat(canEnroll).isFalse();
    }

    @Test
    @DisplayName("Debe verificar si estudiante puede inscribirse - ya inscrito")
    void shouldReturnFalseWhenStudentAlreadyEnrolled() {
        // Given
        Long studentId = 1L;
        Long groupId = 1L;

        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));
        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(studentId, groupId))
                .thenReturn(true);

        // When
        boolean canEnroll = courseGroupService.canStudentEnroll(studentId, groupId);

        // Then
        assertThat(canEnroll).isFalse();
    }

    // ========== TESTS DE OPERACIONES DE PROFESOR ==========

    @Test
    @DisplayName("Debe obtener grupos del profesor")
    void shouldGetTeacherGroups() {
        // Given
        Long teacherId = 1L;
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(teacherId);
        when(teacherRepository.existsById(teacherId)).thenReturn(true);

        testGroup1.getGroupSessions().add(testSession1);
        List<GroupSession> sessions = Arrays.asList(testSession1);
        List<CourseGroupDto> expectedDtos = Arrays.asList(testGroupDto1);

        when(groupSessionRepository.findByTeacherId(teacherId)).thenReturn(sessions);
        when(courseGroupMapper.toDtoList(any())).thenReturn(expectedDtos);

        // When
        List<CourseGroupDto> result = courseGroupService.getTeacherGroups(teacherId);

        // Then
        assertThat(result).hasSize(1);
        verify(groupSessionRepository).findByTeacherId(teacherId);
    }

    @Test
    @DisplayName("Debe fallar al obtener grupos de otro profesor")
    void shouldFailGetTeacherGroupsWhenAccessingOtherTeacher() {
        // Given
        Long currentTeacherId = 1L;
        Long otherTeacherId = 2L;

        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(currentTeacherId);

        // When/Then
        assertThatThrownBy(() -> courseGroupService.getTeacherGroups(otherTeacherId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No puede acceder a información de otros profesores");
    }

    @Test
    @DisplayName("Debe obtener horario del profesor")
    void shouldGetTeacherSchedule() {
        // Given
        Long teacherId = 1L;
        when(sessionUtils.isTeacher()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(teacherId);

        List<GroupSession> sessions = Arrays.asList(testSession1, testSession2);
        GroupSessionDto sessionDto1 = GroupSessionDto.builder()
                .id(1L)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();

        when(groupSessionRepository.findByTeacherId(teacherId)).thenReturn(sessions);
        when(courseGroupMapper.toSessionDto(testSession1)).thenReturn(sessionDto1);
        when(courseGroupMapper.toSessionDto(testSession2)).thenReturn(sessionDto1);

        // When
        List<GroupSessionDto> result = courseGroupService.getTeacherSchedule(teacherId);

        // Then
        assertThat(result).hasSize(2);
        verify(groupSessionRepository).findByTeacherId(teacherId);
    }

    // ========== TESTS DE OPERACIONES ADMINISTRATIVAS ==========

    @Test
    @DisplayName("Debe crear grupo como administrador")
    void shouldCreateGroupAsAdmin() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(courseGroupMapper.toEntity(validCreateDto, testSubject, testTeacher))
                .thenReturn(testGroup1);
        when(courseGroupRepository.save(any(CourseGroup.class))).thenReturn(testGroup1);
        when(courseGroupMapper.toDto(testGroup1)).thenReturn(testGroupDto1);

        // When
        CourseGroupDto result = courseGroupService.createGroup(validCreateDto);

        // Then
        assertThat(result).isEqualTo(testGroupDto1);
        verify(courseGroupRepository).save(any(CourseGroup.class));
    }

    @Test
    @DisplayName("Debe crear grupo sin profesor")
    void shouldCreateGroupWithoutTeacher() {
        // Given
        validCreateDto.setTeacherId(null);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(courseGroupMapper.toEntity(validCreateDto, testSubject, null))
                .thenReturn(testGroup2);
        when(courseGroupRepository.save(any(CourseGroup.class))).thenReturn(testGroup2);
        when(courseGroupMapper.toDto(testGroup2)).thenReturn(testGroupDto2);

        // When
        CourseGroupDto result = courseGroupService.createGroup(validCreateDto);

        // Then
        assertThat(result.getTeacherId()).isNull();
        assertThat(result.getTeacherName()).isEqualTo("Sin asignar");
    }

    @Test
    @DisplayName("Debe fallar al crear grupo sin permisos de admin")
    void shouldFailToCreateGroupWithoutAdminRole() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(false);
        when(sessionUtils.getCurrentUserEmail()).thenReturn("teacher@test.com");

        // When/Then
        assertThatThrownBy(() -> courseGroupService.createGroup(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");

        verify(courseGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar estado del grupo")
    void shouldUpdateGroupStatus() {
        // Given
        Long groupId = 1L;
        testGroup2.setStatus(CourseGroupStatus.PLANNED); // Estado inicial

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup2));
        when(courseGroupRepository.save(any(CourseGroup.class))).thenReturn(testGroup2);
        when(courseGroupMapper.toDto(testGroup2)).thenReturn(testGroupDto2);

        // When
        CourseGroupDto result = courseGroupService.updateGroupStatus(groupId, validStatusUpdateDto);

        // Then
        verify(courseGroupMapper).updateStatus(testGroup2, validStatusUpdateDto);
        verify(courseGroupRepository).save(testGroup2);
    }

    @Test
    @DisplayName("Debe fallar al actualizar grupo cerrado")
    void shouldFailToUpdateClosedGroup() {
        // Given
        Long groupId = 1L;
        testGroup1.setStatus(CourseGroupStatus.CLOSED);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));

        // When/Then
        assertThatThrownBy(() -> courseGroupService.updateGroupStatus(groupId, validStatusUpdateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se puede cambiar el estado de un grupo cerrado");
    }

    @Test
    @DisplayName("Debe asignar profesor a grupo")
    void shouldAssignTeacherToGroup() {
        // Given
        Long groupId = 2L;
        testGroup2.setTeacher(null); // Sin profesor inicial

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup2));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(courseGroupRepository.save(any(CourseGroup.class))).thenReturn(testGroup2);
        when(courseGroupMapper.toDto(testGroup2)).thenReturn(testGroupDto2);

        // When
        CourseGroupDto result = courseGroupService.assignTeacher(groupId, validAssignTeacherDto);

        // Then
        verify(courseGroupMapper).assignTeacher(testGroup2, testTeacher);
        verify(courseGroupRepository).save(testGroup2);
    }

    @Test
    @DisplayName("Debe fallar al asignar profesor a grupo que ya tiene profesor")
    void shouldFailToAssignTeacherWhenGroupAlreadyHasTeacher() {
        // Given
        Long groupId = 1L;

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));

        // When/Then
        assertThatThrownBy(() -> courseGroupService.assignTeacher(groupId, validAssignTeacherDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El grupo ya tiene un profesor asignado");
    }


    @Test
    @DisplayName("Debe validar horario de sesión")
    void shouldValidateSessionTime() {
        // Given - Hora de fin antes que hora de inicio
        Long groupId = 1L;
        validSessionDto.setStartTime(LocalTime.of(12, 0));
        validSessionDto.setEndTime(LocalTime.of(10, 0));

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));

        // When/Then
        assertThatThrownBy(() -> courseGroupService.createGroupSession(groupId, validSessionDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La hora de fin debe ser posterior a la hora de inicio");
    }

    @Test
    @DisplayName("Debe detectar conflicto de horario en sesión")
    void shouldDetectSessionTimeConflict() {
        // Given
        Long groupId = 1L;
        testGroup1.getGroupSessions().add(testSession1); // Ya tiene sesión lunes 10-12

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));

        // When/Then
        assertThatThrownBy(() -> courseGroupService.createGroupSession(groupId, validSessionDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Ya existe una sesión en ese horario para este grupo");
    }

    @Test
    @DisplayName("Debe eliminar grupo sin inscripciones")
    void shouldDeleteGroupWithoutEnrollments() {
        // Given
        Long groupId = 2L;
        testGroup2.setStatus(CourseGroupStatus.PLANNED);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup2));

        // When
        courseGroupService.deleteGroup(groupId);

        // Then
        verify(courseGroupRepository).delete(testGroup2);
    }

    @Test
    @DisplayName("Debe fallar al eliminar grupo con inscripciones")
    void shouldFailToDeleteGroupWithEnrollments() {
        // Given
        Long groupId = 1L;
        Enrollment enrollment = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testGroup1)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        testGroup1.getEnrollments().add(enrollment);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));

        // When/Then
        assertThatThrownBy(() -> courseGroupService.deleteGroup(groupId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se puede eliminar el grupo porque tiene estudiantes inscritos");

        verify(courseGroupRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe obtener estadísticas de grupo")
    void shouldGetGroupStats() {
        // Given
        Long groupId = 1L;

        // Agregar algunas inscripciones
        Enrollment enrollment1 = Enrollment.builder()
                .paymentStatus(PaymentStatus.PAID)
                .build();
        Enrollment enrollment2 = Enrollment.builder()
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        testGroup1.getEnrollments().addAll(Arrays.asList(enrollment1, enrollment2));
        testGroup1.getGroupSessions().add(testSession1);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));

        // When
        GroupStatsDto stats = courseGroupService.getGroupStats(groupId);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getGroupId()).isEqualTo(groupId);
        assertThat(stats.getEnrolledStudents()).isEqualTo(2);
        assertThat(stats.getAvailableSpots()).isEqualTo(28);
        assertThat(stats.getOccupancyRate()).isEqualTo(6.666666666666667 ); // 2/30 * 100
        assertThat(stats.getPaidEnrollments()).isEqualTo(1);
        assertThat(stats.getPendingPayments()).isEqualTo(1);
        assertThat(stats.getTotalSessions()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe manejar excepción de integridad al crear grupo")
    void shouldHandleDataIntegrityViolationOnCreate() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(courseGroupMapper.toEntity(any(), any(), any())).thenReturn(testGroup1);
        when(courseGroupRepository.save(any(CourseGroup.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        // When/Then
        assertThatThrownBy(() -> courseGroupService.createGroup(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Error al crear el grupo. Verifique los datos ingresados");
    }

    @Test
    @DisplayName("Debe validar conflicto de horario del profesor")
    void shouldValidateTeacherScheduleConflict() {
        // Given
        Long groupId = 1L;

        // Configurar testGroup2 con una sesión que entrará en conflicto
        GroupSession conflictingSession = GroupSession.builder()
                .courseGroup(testGroup2)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 102")
                .build();
        testGroup2.getGroupSessions().add(conflictingSession);

        // El profesor ya tiene una sesión el lunes de 10-12 con otro grupo
        testGroup1.getGroupSessions().add(testSession1);
        List<GroupSession> teacherSessions = Arrays.asList(testSession1);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(courseGroupRepository.findById(groupId)).thenReturn(Optional.of(testGroup2));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(groupSessionRepository.findByTeacherId(1L)).thenReturn(teacherSessions);

        // When/Then
        assertThatThrownBy(() -> courseGroupService.assignTeacher(testTeacher.getId(), validAssignTeacherDto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("El profesor tiene conflicto de horario el MONDAY a las 10:00");
    }
}
