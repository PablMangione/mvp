package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.common.DeleteResponseDto;
import com.acainfo.mvp.dto.coursegroup.*;
import com.acainfo.mvp.dto.student.CreateStudentDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.dto.student.UpdateStudentDto;
import com.acainfo.mvp.dto.subject.CreateSubjectDto;
import com.acainfo.mvp.dto.subject.SubjectDto;
import com.acainfo.mvp.dto.subject.UpdateSubjectDto;
import com.acainfo.mvp.dto.teacher.CreateTeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.dto.teacher.UpdateTeacherDto;
import com.acainfo.mvp.exception.auth.EmailAlreadyExistsException;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.CourseGroupMapper;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.mapper.SubjectMapper;
import com.acainfo.mvp.mapper.TeacherMapper;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.*;
import com.acainfo.mvp.repository.*;
import com.acainfo.mvp.service.AdminService.SystemStatsDto;
import com.acainfo.mvp.service.AdminService.GroupRequestDetailsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@DisplayName("AdminService Tests")
class AdminServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private CourseGroupRepository courseGroupRepository;
    @Mock
    private GroupSessionRepository groupSessionRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private GroupRequestRepository groupRequestRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private SubjectMapper subjectMapper;
    @Mock
    private CourseGroupMapper courseGroupMapper;

    private AdminService adminService;

    // Datos de prueba comunes
    private Student testStudent;
    private Teacher testTeacher;
    private Subject testSubject;
    private CourseGroup testGroup;
    private GroupSession testSession;
    private Enrollment testEnrollment;
    private GroupRequest testRequest;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                studentRepository, teacherRepository, subjectRepository,
                courseGroupRepository, groupSessionRepository, enrollmentRepository,
                groupRequestRepository, passwordEncoder, studentMapper,
                teacherMapper, subjectMapper, courseGroupMapper
        );

        // Inicializar datos de prueba
        testStudent = Student.builder()
                .name("Juan Pérez")
                .email("juan@example.com")
                .password("encodedPassword")
                .major("Ingeniería Informática")
                .enrollments(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testStudent, "id", 1L);

        testTeacher = Teacher.builder()
                .name("Dr. García")
                .email("garcia@example.com")
                .password("encodedPassword")
                .courseGroups(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testTeacher, "id", 1L);

        testSubject = Subject.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .courseGroups(new HashSet<>())
                .groupRequests(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testSubject, "id", 1L);

        testGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("150.00"))
                .maxCapacity(30)
                .enrollments(new HashSet<>())
                .groupSessions(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testGroup, "id", 1L);

        testSession = GroupSession.builder()
                .courseGroup(testGroup)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .classroom("A101")
                .build();

        testEnrollment = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testGroup)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        testRequest = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .status(RequestStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("Gestión de Alumnos")
    class StudentManagementTests {

        @Test
        @DisplayName("Crear alumno exitosamente")
        void createStudent_Success() {
            // Given
            CreateStudentDto createDto = CreateStudentDto.builder()
                    .name("Juan Pérez")
                    .email("juan@example.com")
                    .password("password123")
                    .major("Ingeniería Informática")
                    .build();

            StudentDto expectedDto = StudentDto.builder()
                    .name("Juan Pérez")
                    .email("juan@example.com")
                    .major("Ingeniería Informática")
                    .build();
            ReflectionTestUtils.setField(expectedDto, "id", 1L);

            when(studentRepository.existsByEmail(anyString())).thenReturn(false);
            when(teacherRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
                Student saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });
            when(studentMapper.toDto(any(Student.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<StudentDto> result = adminService.createStudent(createDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedDto);
            assertThat(result.getMessage()).contains("exitosamente");

            verify(studentRepository).save(any(Student.class));
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("Crear alumno falla con email duplicado")
        void createStudent_EmailAlreadyExists() {
            // Given
            CreateStudentDto createDto = CreateStudentDto.builder()
                    .email("existing@example.com")
                    .build();

            when(studentRepository.existsByEmail(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> adminService.createStudent(createDto))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("El email ya está registrado en el sistema");

            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Actualizar alumno exitosamente")
        void updateStudent_Success() {
            // Given
            UpdateStudentDto updateDto = UpdateStudentDto.builder()
                    .name("Juan Pérez Actualizado")
                    .major("Ingeniería de Sistemas")
                    .build();

            StudentDto expectedDto = StudentDto.builder()
                    .name("Juan Pérez Actualizado")
                    .email("juan@example.com")
                    .major("Ingeniería de Sistemas")
                    .build();

            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
            when(studentMapper.toDto(any(Student.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<StudentDto> result = adminService.updateStudent(1L, updateDto);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedDto);

            verify(studentMapper).updateEntityFromDto(testStudent, updateDto);
            verify(studentRepository).save(testStudent);
        }

        @Test
        @DisplayName("Actualizar alumno falla si no existe")
        void updateStudent_NotFound() {
            // Given
            UpdateStudentDto updateDto = UpdateStudentDto.builder().build();
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> adminService.updateStudent(999L, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Alumno no encontrado");
        }

        @Test
        @DisplayName("Eliminar alumno exitosamente")
        void deleteStudent_Success() {
            // Given
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            doNothing().when(studentRepository).delete(testStudent);

            // When
            ApiResponseDto<DeleteResponseDto> result = adminService.deleteStudent(1L);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getDeletedId()).isEqualTo(1L);
            assertThat(result.getData().getEntityType()).isEqualTo("Student");
            assertThat(result.getData().isSuccess()).isTrue();

            verify(studentRepository).delete(testStudent);
        }

        @Test
        @DisplayName("Eliminar alumno falla si tiene inscripciones activas")
        void deleteStudent_WithActiveEnrollments() {
            // Given
            testStudent.getEnrollments().add(testEnrollment);
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // When/Then
            assertThatThrownBy(() -> adminService.deleteStudent(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("No se puede eliminar un alumno con inscripciones en grupos activos");

            verify(studentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Listar alumnos con paginación")
        void listStudents_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Student> students = Arrays.asList(testStudent);
            Page<Student> studentPage = new PageImpl<>(students, pageable, 1);

            StudentDto dto = StudentDto.builder()
                    .name("Juan Pérez")
                    .email("juan@example.com")
                    .major("Ingeniería Informática")
                    .build();

            when(studentRepository.findAll(pageable)).thenReturn(studentPage);
            when(studentMapper.toDto(any(Student.class))).thenReturn(dto);

            // When
            Page<StudentDto> result = adminService.listStudents(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(dto);
        }
    }

    @Nested
    @DisplayName("Gestión de Profesores")
    class TeacherManagementTests {

        @Test
        @DisplayName("Crear profesor exitosamente")
        void createTeacher_Success() {
            // Given
            CreateTeacherDto createDto = CreateTeacherDto.builder()
                    .name("Dr. García")
                    .email("garcia@example.com")
                    .password("password123")
                    .build();

            TeacherDto expectedDto = TeacherDto.builder()
                    .name("Dr. García")
                    .email("garcia@example.com")
                    .build();
            ReflectionTestUtils.setField(expectedDto, "id", 1L);

            when(teacherRepository.existsByEmail(anyString())).thenReturn(false);
            when(studentRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> {
                Teacher saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });
            when(teacherMapper.toDto(any(Teacher.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<TeacherDto> result = adminService.createTeacher(createDto);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedDto);

            verify(teacherRepository).save(any(Teacher.class));
        }

        @Test
        @DisplayName("Actualizar profesor exitosamente")
        void updateTeacher_Success() {
            // Given
            UpdateTeacherDto updateDto = UpdateTeacherDto.builder()
                    .name("Dr. García Actualizado")
                    .build();

            TeacherDto expectedDto = TeacherDto.builder()
                    .name("Dr. García Actualizado")
                    .email("garcia@example.com")
                    .build();

            when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
            when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher);
            when(teacherMapper.toDto(any(Teacher.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<TeacherDto> result = adminService.updateTeacher(1L, updateDto);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedDto);

            verify(teacherMapper).updateEntityFromDto(testTeacher, updateDto);
        }

        @Test
        @DisplayName("Eliminar profesor exitosamente")
        void deleteTeacher_Success() {
            // Given
            when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
            doNothing().when(teacherRepository).delete(testTeacher);

            // When
            ApiResponseDto<DeleteResponseDto> result = adminService.deleteTeacher(1L);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getEntityType()).isEqualTo("Teacher");

            verify(teacherRepository).delete(testTeacher);
        }

        @Test
        @DisplayName("Eliminar profesor falla si tiene grupos activos")
        void deleteTeacher_WithActiveGroups() {
            // Given
            testTeacher.getCourseGroups().add(testGroup);
            when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));

            // When/Then
            assertThatThrownBy(() -> adminService.deleteTeacher(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("No se puede eliminar un profesor con grupos activos asignados");

            verify(teacherRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Gestión de Asignaturas")
    class SubjectManagementTests {

        @Test
        @DisplayName("Crear asignatura exitosamente")
        void createSubject_Success() {
            // Given
            CreateSubjectDto createDto = CreateSubjectDto.builder()
                    .name("Programación I")
                    .major("Ingeniería Informática")
                    .courseYear(1)
                    .build();

            SubjectDto expectedDto = SubjectDto.builder()
                    .name("Programación I")
                    .major("Ingeniería Informática")
                    .courseYear(1)
                    .build();
            ReflectionTestUtils.setField(expectedDto, "id", 1L);

            when(subjectRepository.existsByNameAndMajor(anyString(), anyString())).thenReturn(false);
            when(subjectMapper.toEntity(createDto)).thenReturn(testSubject);
            when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);
            when(subjectMapper.toDto(any(Subject.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<SubjectDto> result = adminService.createSubject(createDto);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedDto);

            verify(subjectRepository).save(testSubject);
        }

        @Test
        @DisplayName("Crear asignatura falla si ya existe")
        void createSubject_AlreadyExists() {
            // Given
            CreateSubjectDto createDto = CreateSubjectDto.builder()
                    .name("Programación I")
                    .major("Ingeniería Informática")
                    .build();

            when(subjectRepository.existsByNameAndMajor(anyString(), anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> adminService.createSubject(createDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Ya existe una asignatura con ese nombre en esa carrera");
        }

        @Test
        @DisplayName("Actualizar asignatura exitosamente")
        void updateSubject_Success() {
            // Given
            UpdateSubjectDto updateDto = UpdateSubjectDto.builder()
                    .courseYear(2)
                    .build();

            SubjectDto expectedDto = SubjectDto.builder()
                    .name("Programación I")
                    .major("Ingeniería Informática")
                    .courseYear(2)
                    .build();

            when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
            when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject);
            when(subjectMapper.toDto(any(Subject.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<SubjectDto> result = adminService.updateSubject(1L, updateDto);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedDto);

            verify(subjectMapper).updateEntityFromDto(testSubject, updateDto);
        }

        @Test
        @DisplayName("Eliminar asignatura exitosamente")
        void deleteSubject_Success() {
            // Given
            when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
            doNothing().when(subjectRepository).delete(testSubject);

            // When
            ApiResponseDto<DeleteResponseDto> result = adminService.deleteSubject(1L);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getEntityType()).isEqualTo("Subject");

            verify(subjectRepository).delete(testSubject);
        }

        @Test
        @DisplayName("Eliminar asignatura falla si tiene grupos activos")
        void deleteSubject_WithActiveGroups() {
            // Given
            testSubject.getCourseGroups().add(testGroup);
            when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));

            // When/Then
            assertThatThrownBy(() -> adminService.deleteSubject(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("No se puede eliminar una asignatura con grupos activos");
        }

        @Test
        @DisplayName("Eliminar asignatura falla si tiene solicitudes pendientes")
        void deleteSubject_WithPendingRequests() {
            // Given
            testSubject.getGroupRequests().add(testRequest);
            when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));

            // When/Then
            assertThatThrownBy(() -> adminService.deleteSubject(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("No se puede eliminar una asignatura con solicitudes pendientes");
        }
    }

    @Nested
    @DisplayName("Gestión de Grupos")
    class CourseGroupManagementTests {

        @Test
        @DisplayName("Crear grupo exitosamente")
        void createCourseGroup_Success() {
            // Given
            CreateCourseGroupDto createDto = CreateCourseGroupDto.builder()
                    .subjectId(1L)
                    .teacherId(1L)
                    .type(CourseGroupType.REGULAR)
                    .price(new BigDecimal("150.00"))
                    .sessions(Arrays.asList(
                            CreateGroupSessionDto.builder()
                                    .dayOfWeek("MONDAY")
                                    .startTime(LocalTime.of(9, 0))
                                    .endTime(LocalTime.of(11, 0))
                                    .classroom("A101")
                                    .build()
                    ))
                    .build();

            CourseGroupDto expectedDto = CourseGroupDto.builder()
                    .subjectId(1L)
                    .subjectName("Programación I")
                    .teacherId(1L)
                    .teacherName("Dr. García")
                    .status(CourseGroupStatus.PLANNED)
                    .type(CourseGroupType.REGULAR)
                    .price(new BigDecimal("150.00"))
                    .enrolledStudents(0)
                    .build();

            when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
            when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
            when(courseGroupRepository.save(any(CourseGroup.class))).thenAnswer(invocation -> {
                CourseGroup saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });
            when(groupSessionRepository.save(any(GroupSession.class))).thenReturn(testSession);
            when(courseGroupMapper.toDto(any(CourseGroup.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<CourseGroupDto> result = adminService.createCourseGroup(createDto);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(expectedDto);

            verify(courseGroupRepository).save(any(CourseGroup.class));
            verify(groupSessionRepository).save(any(GroupSession.class));
        }

        @Test
        @DisplayName("Actualizar estado de grupo exitosamente")
        void updateGroupStatus_Success() {
            // Given
            CourseGroup plannedGroup = CourseGroup.builder()
                    .subject(testSubject)
                    .teacher(testTeacher)
                    .status(CourseGroupStatus.PLANNED)
                    .type(CourseGroupType.REGULAR)
                    .price(new BigDecimal("150.00"))
                    .groupSessions(new HashSet<>(Arrays.asList(testSession)))
                    .build();
            ReflectionTestUtils.setField(plannedGroup, "id", 1L);

            UpdateGroupStatusDto updateDto = UpdateGroupStatusDto.builder()
                    .status(CourseGroupStatus.ACTIVE)
                    .build();

            CourseGroupDto expectedDto = CourseGroupDto.builder()
                    .status(CourseGroupStatus.ACTIVE)
                    .build();

            when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(plannedGroup));
            when(courseGroupRepository.save(any(CourseGroup.class))).thenReturn(plannedGroup);
            when(courseGroupMapper.toDto(any(CourseGroup.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<CourseGroupDto> result = adminService.updateGroupStatus(1L, updateDto);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getStatus()).isEqualTo(CourseGroupStatus.ACTIVE);

            verify(courseGroupRepository).save(plannedGroup);
        }

        @Test
        @DisplayName("Actualizar estado falla con transición inválida")
        void updateGroupStatus_InvalidTransition() {
            // Given
            UpdateGroupStatusDto updateDto = UpdateGroupStatusDto.builder()
                    .status(CourseGroupStatus.CLOSED)
                    .build();

            CourseGroup plannedGroup = CourseGroup.builder()
                    .status(CourseGroupStatus.PLANNED)
                    .build();

            when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(plannedGroup));

            // When/Then
            assertThatThrownBy(() -> adminService.updateGroupStatus(1L, updateDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Un grupo PLANNED solo puede cambiar a ACTIVE");
        }

        @Test
        @DisplayName("Actualizar estado falla sin profesor asignado")
        void updateGroupStatus_NoTeacher() {
            // Given
            CourseGroup groupWithoutTeacher = CourseGroup.builder()
                    .subject(testSubject)
                    .teacher(null)
                    .status(CourseGroupStatus.PLANNED)
                    .groupSessions(new HashSet<>(Arrays.asList(testSession)))
                    .build();

            UpdateGroupStatusDto updateDto = UpdateGroupStatusDto.builder()
                    .status(CourseGroupStatus.ACTIVE)
                    .build();

            when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(groupWithoutTeacher));

            // When/Then
            assertThatThrownBy(() -> adminService.updateGroupStatus(1L, updateDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("No se puede activar un grupo sin profesor asignado");
        }

        @Test
        @DisplayName("Asignar profesor a grupo exitosamente")
        void assignTeacherToGroup_Success() {
            // Given
            CourseGroup groupWithoutTeacher = CourseGroup.builder()
                    .subject(testSubject)
                    .teacher(null)
                    .status(CourseGroupStatus.PLANNED)
                    .build();

            AssignTeacherDto assignDto = AssignTeacherDto.builder()
                    .teacherId(1L)
                    .build();

            CourseGroupDto expectedDto = CourseGroupDto.builder()
                    .teacherId(1L)
                    .teacherName("Dr. García")
                    .build();

            when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(groupWithoutTeacher));
            when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher));
            when(courseGroupRepository.save(any(CourseGroup.class))).thenReturn(groupWithoutTeacher);
            when(courseGroupMapper.toDto(any(CourseGroup.class))).thenReturn(expectedDto);

            // When
            ApiResponseDto<CourseGroupDto> result = adminService.assignTeacherToGroup(1L, assignDto);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getTeacherId()).isEqualTo(1L);

            verify(courseGroupRepository).save(groupWithoutTeacher);
        }

        @Test
        @DisplayName("Eliminar grupo exitosamente")
        void deleteCourseGroup_Success() {
            // Given
            CourseGroup plannedGroup = CourseGroup.builder()
                    .status(CourseGroupStatus.PLANNED)
                    .enrollments(new HashSet<>())
                    .build();
            ReflectionTestUtils.setField(plannedGroup, "id", 1L);

            when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(plannedGroup));
            doNothing().when(courseGroupRepository).delete(plannedGroup);

            // When
            ApiResponseDto<DeleteResponseDto> result = adminService.deleteCourseGroup(1L);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getEntityType()).isEqualTo("CourseGroup");

            verify(courseGroupRepository).delete(plannedGroup);
        }

        @Test
        @DisplayName("Eliminar grupo falla si no está en estado PLANNED")
        void deleteCourseGroup_NotPlanned() {
            // Given
            when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

            // When/Then
            assertThatThrownBy(() -> adminService.deleteCourseGroup(1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Solo se pueden eliminar grupos en estado PLANNED");
        }

        @Test
        @DisplayName("Listar grupos con filtros")
        void listCourseGroups_WithFilters() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<CourseGroup> groups = Arrays.asList(testGroup);

            CourseGroupDto dto = CourseGroupDto.builder()
                    .status(CourseGroupStatus.ACTIVE)
                    .build();

            when(courseGroupRepository.findByStatus(CourseGroupStatus.ACTIVE)).thenReturn(groups);
            when(courseGroupMapper.toDto(any(CourseGroup.class))).thenReturn(dto);

            // When
            Page<CourseGroupDto> result = adminService.listCourseGroups(
                    CourseGroupStatus.ACTIVE, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseGroupStatus.ACTIVE);
        }
    }
}
