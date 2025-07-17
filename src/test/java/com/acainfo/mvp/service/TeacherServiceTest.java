package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.dto.teacher.TeacherDetailDto;
import com.acainfo.mvp.dto.teacher.TeacherScheduleDto;
import com.acainfo.mvp.dto.teacher.ScheduleSlotDto;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.mapper.TeacherMapper;
import com.acainfo.mvp.mapper.TeacherMapper.TeacherGroupSummaryDto;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.*;
import com.acainfo.mvp.repository.CourseGroupRepository;
import com.acainfo.mvp.repository.EnrollmentRepository;
import com.acainfo.mvp.repository.TeacherRepository;
import com.acainfo.mvp.service.TeacherService.TeacherStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherService Tests")
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private CourseGroupRepository courseGroupRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TeacherMapper teacherMapper;

    @Mock
    private StudentMapper studentMapper;

    private TeacherService teacherService;

    private Teacher testTeacher;
    private Subject testSubject;
    private CourseGroup activeGroup;
    private CourseGroup plannedGroup;
    private Student testStudent1;
    private Student testStudent2;
    private Enrollment paidEnrollment;
    private Enrollment pendingEnrollment;
    private GroupSession mondaySession;
    private GroupSession wednesdaySession;

    @BeforeEach
    void setUp() {
        // Inicializar el servicio
        teacherService = new TeacherService(
                teacherRepository,
                courseGroupRepository,
                enrollmentRepository,
                teacherMapper,
                studentMapper
        );

        // Configurar profesor de prueba
        testTeacher = Teacher.builder()
                .name("Dr. García")
                .email("garcia@example.com")
                .password("encodedPassword")
                .courseGroups(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testTeacher, "id", 1L);
        ReflectionTestUtils.setField(testTeacher, "createdAt", LocalDateTime.now());

        // Configurar asignatura
        testSubject = Subject.builder()
                .name("Programación Avanzada")
                .major("Ingeniería Informática")
                .courseYear(3)
                .build();
        ReflectionTestUtils.setField(testSubject, "id", 1L);

        // Configurar grupo activo
        activeGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("200.00"))
                .maxCapacity(30)
                .enrollments(new HashSet<>())
                .groupSessions(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(activeGroup, "id", 1L);

        // Configurar grupo planificado
        plannedGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.INTENSIVE)
                .price(new BigDecimal("300.00"))
                .maxCapacity(20)
                .enrollments(new HashSet<>())
                .groupSessions(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(plannedGroup, "id", 2L);

        // Configurar sesiones
        mondaySession = GroupSession.builder()
                .courseGroup(activeGroup)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .classroom("A101")
                .build();
        ReflectionTestUtils.setField(mondaySession, "id", 1L);

        wednesdaySession = GroupSession.builder()
                .courseGroup(activeGroup)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .classroom("A101")
                .build();
        ReflectionTestUtils.setField(wednesdaySession, "id", 2L);

        activeGroup.getGroupSessions().add(mondaySession);
        activeGroup.getGroupSessions().add(wednesdaySession);

        // Configurar estudiantes
        testStudent1 = Student.builder()
                .name("Juan Pérez")
                .email("juan@example.com")
                .major("Ingeniería Informática")
                .build();
        ReflectionTestUtils.setField(testStudent1, "id", 1L);

        testStudent2 = Student.builder()
                .name("María López")
                .email("maria@example.com")
                .major("Ingeniería Informática")
                .build();
        ReflectionTestUtils.setField(testStudent2, "id", 2L);

        // Configurar inscripciones
        paidEnrollment = Enrollment.builder()
                .student(testStudent1)
                .courseGroup(activeGroup)
                .paymentStatus(PaymentStatus.PAID)
                .build();
        ReflectionTestUtils.setField(paidEnrollment, "id", 1L);

        pendingEnrollment = Enrollment.builder()
                .student(testStudent2)
                .courseGroup(activeGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(pendingEnrollment, "id", 2L);

        activeGroup.getEnrollments().add(paidEnrollment);
        activeGroup.getEnrollments().add(pendingEnrollment);

        // Asociar grupos al profesor
        testTeacher.getCourseGroups().add(activeGroup);
        testTeacher.getCourseGroups().add(plannedGroup);
    }

    @Test
    @DisplayName("Obtener perfil de profesor exitosamente")
    void getTeacherProfile_Success() {
        // Given
        TeacherDetailDto expectedDto = TeacherDetailDto.builder()
                .name("Dr. García")
                .email("garcia@example.com")
                .totalGroups(2)
                .activeGroups(1)
                .build();
        ReflectionTestUtils.setField(expectedDto, "id", 1L);

        when(teacherRepository.findByIdWithFullDetails(1L)).thenReturn(Optional.of(testTeacher));
        when(teacherMapper.toDetailDto(testTeacher)).thenReturn(expectedDto);

        // When
        ApiResponseDto<TeacherDetailDto> result = teacherService.getTeacherProfile(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getName()).isEqualTo("Dr. García");
        assertThat(result.getData().getTotalGroups()).isEqualTo(2);
        assertThat(result.getData().getActiveGroups()).isEqualTo(1);

        verify(teacherMapper).toDetailDto(testTeacher);
    }

    @Test
    @DisplayName("Obtener perfil falla cuando profesor no existe")
    void getTeacherProfile_NotFound() {
        // Given
        when(teacherRepository.findByIdWithFullDetails(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> teacherService.getTeacherProfile(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profesor no encontrado");
    }

    @Test
    @DisplayName("Obtener horario semanal exitosamente")
    void getWeeklySchedule_Success() {
        // Given
        List<ScheduleSlotDto> scheduleSlots = Arrays.asList(
                ScheduleSlotDto.builder()
                        .dayOfWeek("MONDAY")
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(11, 0))
                        .classroom("A101")
                        .subjectName("Programación Avanzada")
                        .courseGroupId(1L)
                        .groupType("REGULAR")
                        .enrolledStudents(2)
                        .build(),
                ScheduleSlotDto.builder()
                        .dayOfWeek("WEDNESDAY")
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(11, 0))
                        .classroom("A101")
                        .subjectName("Programación Avanzada")
                        .courseGroupId(1L)
                        .groupType("REGULAR")
                        .enrolledStudents(2)
                        .build()
        );

        TeacherScheduleDto expectedDto = TeacherScheduleDto.builder()
                .teacherId(1L)
                .teacherName("Dr. García")
                .schedule(scheduleSlots)
                .build();

        when(teacherRepository.findByIdWithFullDetails(1L)).thenReturn(Optional.of(testTeacher));
        when(teacherMapper.toScheduleDto(testTeacher)).thenReturn(expectedDto);

        // When
        ApiResponseDto<TeacherScheduleDto> result = teacherService.getWeeklySchedule(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getSchedule()).hasSize(2);
        assertThat(result.getMessage()).contains("2 sesiones semanales");

        verify(teacherMapper).toScheduleDto(testTeacher);
    }

    @Test
    @DisplayName("Obtener grupos asignados - todos")
    void getAssignedGroups_All() {
        // Given
        List<CourseGroup> groups = Arrays.asList(activeGroup, plannedGroup);
        List<TeacherGroupSummaryDto> expectedDtos = Arrays.asList(
                TeacherGroupSummaryDto.builder()
                        .groupId(1L)
                        .subjectName("Programación Avanzada")
                        .groupType("REGULAR")
                        .groupStatus("ACTIVE")
                        .enrolledStudents(2)
                        .paidStudents(1)
                        .build(),
                TeacherGroupSummaryDto.builder()
                        .groupId(2L)
                        .subjectName("Programación Avanzada")
                        .groupType("INTENSIVE")
                        .groupStatus("PLANNED")
                        .enrolledStudents(0)
                        .paidStudents(0)
                        .build()
        );

        when(teacherRepository.existsById(1L)).thenReturn(true);
        when(courseGroupRepository.findByTeacherId(1L)).thenReturn(groups);
        when(teacherMapper.toGroupSummaryDtoList(groups)).thenReturn(expectedDtos);

        // When
        ApiResponseDto<List<TeacherGroupSummaryDto>> result = teacherService.getAssignedGroups(1L, false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getMessage()).contains("2 grupos");

        verify(courseGroupRepository).findByTeacherId(1L);
        verify(teacherMapper).toGroupSummaryDtoList(groups);
    }

    @Test
    @DisplayName("Obtener grupos asignados - solo activos")
    void getAssignedGroups_OnlyActive() {
        // Given
        List<CourseGroup> allGroups = Arrays.asList(activeGroup, plannedGroup);
        List<TeacherGroupSummaryDto> expectedDto = Collections.singletonList(
                TeacherGroupSummaryDto.builder()
                        .groupId(1L)
                        .subjectName("Programación Avanzada")
                        .groupType("REGULAR")
                        .groupStatus("ACTIVE")
                        .enrolledStudents(2)
                        .paidStudents(1)
                        .build()
        );

        when(teacherRepository.existsById(1L)).thenReturn(true);
        when(courseGroupRepository.findByTeacherId(1L)).thenReturn(allGroups);
        when(teacherMapper.toGroupSummaryDtoList(anyList())).thenReturn(expectedDto);

        // When
        ApiResponseDto<List<TeacherGroupSummaryDto>> result = teacherService.getAssignedGroups(1L, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().getFirst().getGroupStatus()).isEqualTo("ACTIVE");

        verify(courseGroupRepository).findByTeacherId(1L);
    }

    @Test
    @DisplayName("Obtener estudiantes por grupo exitosamente")
    void getStudentsByGroup_Success() {
        // Given
        List<Enrollment> enrollments = Arrays.asList(paidEnrollment, pendingEnrollment);

        StudentDto student1Dto = StudentDto.builder()
                .name("Juan Pérez")
                .email("juan@example.com")
                .major("Ingeniería Informática")
                .build();
        ReflectionTestUtils.setField(student1Dto, "id", 1L);

        StudentDto student2Dto = StudentDto.builder()
                .name("María López")
                .email("maria@example.com")
                .major("Ingeniería Informática")
                .build();
        ReflectionTestUtils.setField(student2Dto, "id", 2L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(enrollmentRepository.findByCourseGroupId(1L)).thenReturn(enrollments);
        when(studentMapper.toDto(testStudent1)).thenReturn(student1Dto);
        when(studentMapper.toDto(testStudent2)).thenReturn(student2Dto);

        // When
        ApiResponseDto<List<StudentDto>> result = teacherService.getStudentsByGroup(1L, 1L, false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getMessage()).contains("2 estudiantes");

        verify(enrollmentRepository).findByCourseGroupId(1L);
        verify(studentMapper, times(2)).toDto(any(Student.class));
    }

    @Test
    @DisplayName("Obtener estudiantes por grupo - solo pagados")
    void getStudentsByGroup_OnlyPaid() {
        // Given
        List<Enrollment> enrollments = Arrays.asList(paidEnrollment, pendingEnrollment);

        StudentDto student1Dto = StudentDto.builder()
                .name("Juan Pérez")
                .email("juan@example.com")
                .major("Ingeniería Informática")
                .build();
        ReflectionTestUtils.setField(student1Dto, "id", 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(enrollmentRepository.findByCourseGroupId(1L)).thenReturn(enrollments);
        when(studentMapper.toDto(testStudent1)).thenReturn(student1Dto);

        // When
        ApiResponseDto<List<StudentDto>> result = teacherService.getStudentsByGroup(1L, 1L, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().getFirst().getName()).isEqualTo("Juan Pérez");

        verify(enrollmentRepository).findByCourseGroupId(1L);
        verify(studentMapper, times(1)).toDto(testStudent1);
    }

    @Test
    @DisplayName("Obtener estudiantes falla si grupo no pertenece al profesor")
    void getStudentsByGroup_UnauthorizedAccess() {
        // Given
        Teacher otherTeacher = Teacher.builder()
                .name("Dr. Otro")
                .email("otro@example.com")
                .build();
        ReflectionTestUtils.setField(otherTeacher, "id", 2L);

        activeGroup.setTeacher(otherTeacher);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));

        // When/Then
        assertThatThrownBy(() -> teacherService.getStudentsByGroup(1L, 1L, false))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Este grupo no está asignado a tu perfil");
    }

    @Test
    @DisplayName("Obtener estudiantes falla si profesor no existe")
    void getStudentsByGroup_TeacherNotFound() {
        // Given
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> teacherService.getStudentsByGroup(999L, 1L, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profesor no encontrado");
    }

    @Test
    @DisplayName("Obtener estudiantes falla si grupo no existe")
    void getStudentsByGroup_GroupNotFound() {
        // Given
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
        when(courseGroupRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> teacherService.getStudentsByGroup(1L, 999L, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Grupo no encontrado");
    }

    @Test
    @DisplayName("Obtener estadísticas del profesor exitosamente")
    void getTeacherStatistics_Success() {
        // Given
        when(teacherRepository.findByIdWithFullDetails(1L)).thenReturn(Optional.of(testTeacher));

        // When
        ApiResponseDto<TeacherStatsDto> result = teacherService.getTeacherStatistics(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTotalGroups()).isEqualTo(2);
        assertThat(result.getData().getActiveGroups()).isEqualTo(1);
        assertThat(result.getData().getTotalStudents()).isEqualTo(2);
        assertThat(result.getData().getActiveStudents()).isEqualTo(2);
        assertThat(result.getData().getPaidStudents()).isEqualTo(1);
        assertThat(result.getData().getWeeklyHours()).isEqualTo(4.0); // 2 sesiones de 2 horas
        assertThat(result.getData().getTotalSessionsPerWeek()).isEqualTo(2);
    }

    @Test
    @DisplayName("Verificar conflicto de horario - sin conflicto")
    void hasScheduleConflict_NoConflict() {
        // Given
        when(teacherRepository.findByIdWithFullDetails(1L)).thenReturn(Optional.of(testTeacher));

        // When - Martes no tiene clases
        boolean result = teacherService.hasScheduleConflict(1L, "TUESDAY",
                LocalTime.of(9, 0), LocalTime.of(11, 0));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Verificar conflicto de horario - con conflicto")
    void hasScheduleConflict_WithConflict() {
        // Given
        when(teacherRepository.findByIdWithFullDetails(1L)).thenReturn(Optional.of(testTeacher));

        // When - Lunes a la misma hora que ya tiene clase
        boolean result = teacherService.hasScheduleConflict(1L, "MONDAY",
                LocalTime.of(9, 30), LocalTime.of(10, 30));

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Verificar conflicto de horario - profesor no existe")
    void hasScheduleConflict_TeacherNotFound() {
        // Given
        when(teacherRepository.findByIdWithFullDetails(999L)).thenReturn(Optional.empty());

        // When
        boolean result = teacherService.hasScheduleConflict(999L, "MONDAY",
                LocalTime.of(9, 0), LocalTime.of(11, 0));

        // Then
        assertThat(result).isFalse();
    }
}