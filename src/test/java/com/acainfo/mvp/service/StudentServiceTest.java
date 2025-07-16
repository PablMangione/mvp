package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.grouprequest.CreateGroupRequestDto;
import com.acainfo.mvp.dto.grouprequest.GroupRequestResponseDto;
import com.acainfo.mvp.dto.student.*;
import com.acainfo.mvp.dto.subject.SubjectDto;
import com.acainfo.mvp.dto.subject.SubjectWithGroupsDto;
import com.acainfo.mvp.exception.student.*;
import com.acainfo.mvp.exception.student.DuplicateRequestException;
import com.acainfo.mvp.mapper.EnrollmentMapper;
import com.acainfo.mvp.mapper.GroupRequestMapper;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.mapper.SubjectMapper;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.*;
import com.acainfo.mvp.repository.*;
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
@DisplayName("StudentService Tests")
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private CourseGroupRepository courseGroupRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private GroupRequestRepository groupRequestRepository;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private SubjectMapper subjectMapper;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @Mock
    private GroupRequestMapper groupRequestMapper;

    private StudentService studentService;

    private Student testStudent;
    private Subject testSubject;
    private CourseGroup activeGroup;
    private Teacher testTeacher;
    private Enrollment testEnrollment;
    private GroupRequest testRequest;

    @BeforeEach
    void setUp() {
        // Inicializar el servicio manualmente con todos los mocks
        studentService = new StudentService(
                studentRepository,
                subjectRepository,
                courseGroupRepository,
                enrollmentRepository,
                groupRequestRepository,
                studentMapper,
                subjectMapper,
                enrollmentMapper,
                groupRequestMapper
        );
        // Configurar estudiante de prueba
        testStudent = Student.builder()
                .name("Juan Pérez")
                .email("juan.perez@example.com")
                .major("Ingeniería Informática")
                .enrollments(new HashSet<>())
                .groupRequests(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testStudent, "id", 1L);
        ReflectionTestUtils.setField(testStudent, "createdAt", LocalDateTime.now());

        // Configurar profesor
        testTeacher = Teacher.builder()
                .name("Dr. García")
                .email("garcia@example.com")
                .build();
        ReflectionTestUtils.setField(testTeacher, "id", 1L);

        // Configurar asignatura
        testSubject = Subject.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .courseGroups(new HashSet<>())
                .groupRequests(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(testSubject, "id", 1L);
        ReflectionTestUtils.setField(testSubject, "createdAt", LocalDateTime.now());

        // Configurar grupo activo
        activeGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("150.00"))
                .enrollments(new HashSet<>())
                .groupSessions(new HashSet<>())
                .build();
        ReflectionTestUtils.setField(activeGroup, "id", 1L);

        // Configurar sesión
        GroupSession session = GroupSession.builder()
                .courseGroup(activeGroup)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .classroom("A101")
                .build();
        activeGroup.getGroupSessions().add(session);

        // Configurar inscripción
        testEnrollment = Enrollment.builder()
                .student(testStudent)
                .courseGroup(activeGroup)
                .enrollmentDate(LocalDateTime.now())
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(testEnrollment, "id", 1L);

        // Configurar solicitud
        testRequest = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .requestDate(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(testRequest, "id", 1L);
    }

    @Test
    @DisplayName("Obtener perfil de estudiante exitosamente")
    void getStudentProfile_Success() {
        // Given
        testStudent.getEnrollments().add(testEnrollment);
        testStudent.getGroupRequests().add(testRequest);

        StudentDetailDto expectedDto = StudentDetailDto.builder()
                .name("Juan Pérez")
                .email("juan.perez@example.com")
                .major("Ingeniería Informática")
                .activeEnrollments(1)
                .pendingPayments(1)
                .pendingRequests(1)
                .build();
        ReflectionTestUtils.setField(expectedDto, "id", 1L);

        when(studentRepository.findByIdWithFullDetails(1L)).thenReturn(Optional.of(testStudent));
        when(studentMapper.toDetailDto(testStudent)).thenReturn(expectedDto);

        // When
        ApiResponseDto<StudentDetailDto> result = studentService.getStudentProfile(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getName()).isEqualTo("Juan Pérez");
        assertThat(result.getData().getActiveEnrollments()).isEqualTo(1);
        assertThat(result.getData().getPendingPayments()).isEqualTo(1);
        assertThat(result.getData().getPendingRequests()).isEqualTo(1);

        verify(studentMapper).toDetailDto(testStudent);
    }

    @Test
    @DisplayName("Obtener perfil falla cuando estudiante no existe")
    void getStudentProfile_NotFound() {
        // Given
        when(studentRepository.findByIdWithFullDetails(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> studentService.getStudentProfile(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Estudiante no encontrado");
    }

    @Test
    @DisplayName("Obtener asignaturas disponibles exitosamente")
    void getAvailableSubjects_Success() {
        // Given
        testSubject.getCourseGroups().add(activeGroup);
        List<Subject> subjects = Collections.singletonList(testSubject);

        SubjectDto expectedDto = SubjectDto.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();
        ReflectionTestUtils.setField(expectedDto, "id", 1L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findByMajor("Ingeniería Informática")).thenReturn(subjects);
        when(subjectMapper.toDtoList(subjects)).thenReturn(List.of(expectedDto));

        // When
        ApiResponseDto<List<SubjectDto>> result = studentService.getAvailableSubjects(1L, null, false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().getFirst().getName()).isEqualTo("Programación I");

        verify(subjectMapper).toDtoList(subjects);
    }

    @Test
    @DisplayName("Obtener solo asignaturas con grupos activos")
    void getAvailableSubjects_OnlyActive() {
        // Given
        testSubject.getCourseGroups().add(activeGroup);
        List<Subject> activeSubjects = Collections.singletonList(testSubject);

        SubjectDto expectedDto = SubjectDto.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();
        ReflectionTestUtils.setField(expectedDto, "id", 1L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findSubjectsWithActiveGroups()).thenReturn(activeSubjects);
        when(subjectMapper.toDtoList(any())).thenReturn(List.of(expectedDto));

        // When
        ApiResponseDto<List<SubjectDto>> result = studentService.getAvailableSubjects(1L, null, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().getFirst().getName()).isEqualTo("Programación I");
        verify(subjectRepository).findSubjectsWithActiveGroups();
        verify(subjectMapper).toDtoList(any());
    }

    @Test
    @DisplayName("Obtener asignatura con grupos activos exitosamente")
    void getSubjectWithActiveGroups_Success() {
        // Given
        testSubject.getCourseGroups().add(activeGroup);

        SubjectWithGroupsDto expectedDto = SubjectWithGroupsDto.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .activeGroups(1)
                .totalGroups(1)
                .hasActiveGroups(true)
                .availableGroups(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(expectedDto, "id", 1L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findByIdWithFullDetails(1L)).thenReturn(Optional.of(testSubject));
        when(subjectMapper.toWithGroupsDto(testSubject)).thenReturn(expectedDto);

        // When
        ApiResponseDto<SubjectWithGroupsDto> result = studentService.getSubjectWithActiveGroups(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getName()).isEqualTo("Programación I");
        assertThat(result.getData().getActiveGroups()).isEqualTo(1);
        assertThat(result.getData().isHasActiveGroups()).isTrue();

        verify(subjectMapper).toWithGroupsDto(testSubject);
    }

    @Test
    @DisplayName("Obtener asignatura falla si no es de la carrera del estudiante")
    void getSubjectWithActiveGroups_WrongMajor() {
        // Given
        testSubject.setMajor("Ingeniería Civil");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findByIdWithFullDetails(1L)).thenReturn(Optional.of(testSubject));

        // When/Then
        assertThatThrownBy(() -> studentService.getSubjectWithActiveGroups(1L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Esta asignatura no pertenece a tu carrera");
    }

    @Test
    @DisplayName("Crear solicitud de grupo exitosamente")
    void createGroupRequest_Success() {
        // Given
        CreateGroupRequestDto requestDto = CreateGroupRequestDto.builder()
                .subjectId(1L)
                .comments("Necesito este grupo")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(1L, 1L, RequestStatus.PENDING))
                .thenReturn(false);
        when(groupRequestRepository.save(any(GroupRequest.class))).thenAnswer(invocation -> {
            GroupRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        when(groupRequestRepository.countBySubjectIdAndStatus(1L, RequestStatus.PENDING)).thenReturn(5L);

        // When
        ApiResponseDto<GroupRequestResponseDto> result = studentService.createGroupRequest(1L, requestDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRequestId()).isEqualTo(1L);
        assertThat(result.getData().getTotalRequests()).isEqualTo(5);
        verify(groupRequestRepository).save(any(GroupRequest.class));
    }

    @Test
    @DisplayName("Crear solicitud falla si ya existe una pendiente")
    void createGroupRequest_DuplicateRequest() {
        // Given
        CreateGroupRequestDto requestDto = CreateGroupRequestDto.builder()
                .subjectId(1L)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(1L, 1L, RequestStatus.PENDING))
                .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> studentService.createGroupRequest(1L, requestDto))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessage("Ya tienes una solicitud pendiente para esta asignatura");
    }

    @Test
    @DisplayName("Crear solicitud falla si hay grupos activos")
    void createGroupRequest_ActiveGroupsExist() {
        // Given
        CreateGroupRequestDto requestDto = CreateGroupRequestDto.builder()
                .subjectId(1L)
                .build();

        testSubject.getCourseGroups().add(activeGroup);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(1L, 1L, RequestStatus.PENDING))
                .thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> studentService.createGroupRequest(1L, requestDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Esta asignatura ya tiene grupos activos disponibles");
    }

    @Test
    @DisplayName("Obtener inscripciones del estudiante")
    void getStudentEnrollments_Success() {
        // Given
        List<Enrollment> enrollments = Collections.singletonList(testEnrollment);

        EnrollmentSummaryDto summaryDto = EnrollmentSummaryDto.builder()
                .enrollmentId(1L)
                .courseGroupId(1L)
                .subjectName("Programación I")
                .teacherName("Dr. García")
                .groupType("REGULAR")
                .groupStatus("ACTIVE")
                .enrollmentDate(testEnrollment.getEnrollmentDate())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(enrollmentRepository.findByStudentId(1L)).thenReturn(enrollments);
        when(enrollmentMapper.toSummaryDtoList(enrollments)).thenReturn(Collections.singletonList(summaryDto));

        // When
        ApiResponseDto<List<EnrollmentSummaryDto>> result = studentService.getStudentEnrollments(1L, false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().getFirst().getSubjectName()).isEqualTo("Programación I");
        assertThat(result.getData().getFirst().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(enrollmentMapper).toSummaryDtoList(enrollments);
    }

    @Test
    @DisplayName("Obtener solicitudes del estudiante")
    void getStudentGroupRequests_Success() {
        // Given
        List<GroupRequest> requests = Collections.singletonList(testRequest);

        GroupRequestSummaryDto summaryDto = GroupRequestSummaryDto.builder()
                .requestId(1L)
                .subjectId(1L)
                .subjectName("Programación I")
                .requestDate(testRequest.getRequestDate())
                .status(RequestStatus.PENDING)
                .build();

        when(groupRequestRepository.findByStudentId(1L)).thenReturn(requests);
        when(groupRequestMapper.toSummaryDtoList(requests)).thenReturn(Collections.singletonList(summaryDto));

        // When
        ApiResponseDto<List<GroupRequestSummaryDto>> result = studentService.getStudentGroupRequests(1L, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().getFirst().getSubjectName()).isEqualTo("Programación I");
        assertThat(result.getData().getFirst().getStatus()).isEqualTo(RequestStatus.PENDING);

        verify(groupRequestMapper).toSummaryDtoList(requests);
    }

    @Test
    @DisplayName("Verificar si puede inscribirse en grupo - puede")
    void canEnrollInGroup_True() {
        // Given
        activeGroup.setMaxCapacity(30);
        // Simular que ya hay 10 inscripciones
        for (int i = 0; i < 10; i++) {
            Enrollment enrollment = Enrollment.builder()
                    .student(Student.builder().build())
                    .courseGroup(activeGroup)
                    .build();
            activeGroup.getEnrollments().add(enrollment);
        }
        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(1L, 1L)).thenReturn(false);
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        // When
        boolean result = studentService.canEnrollInGroup(1L, 1L);
        // Then
        assertThat(result).isTrue();
    }



    @Test
    @DisplayName("Verificar si puede inscribirse en grupo - ya inscrito")
    void canEnrollInGroup_AlreadyEnrolled() {
        // Given
        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(1L, 1L)).thenReturn(true);

        // When
        boolean result = studentService.canEnrollInGroup(1L, 1L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Verificar si puede inscribirse en grupo - grupo lleno")
    void canEnrollInGroup_GroupFull() {
        // Given
        activeGroup.setMaxCapacity(30);
        // Simular que el grupo está lleno
        for (int i = 0; i < 30; i++) {
            Enrollment enrollment = Enrollment.builder()
                    .student(Student.builder().build())
                    .courseGroup(activeGroup)
                    .build();
            activeGroup.getEnrollments().add(enrollment);
        }

        when(enrollmentRepository.existsByStudentIdAndCourseGroupId(1L, 1L)).thenReturn(false);
        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));

        // When
        boolean result = studentService.canEnrollInGroup(1L, 1L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Obtener información de capacidad del grupo")
    void getGroupCapacity_Success() {
        // Given
        activeGroup.setMaxCapacity(30);
        // Agregar algunas inscripciones
        for (int i = 0; i < 20; i++) {
            Enrollment enrollment = Enrollment.builder()
                    .student(Student.builder().build())
                    .courseGroup(activeGroup)
                    .build();
            activeGroup.getEnrollments().add(enrollment);
        }

        when(courseGroupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));

        // When
        ApiResponseDto<StudentService.GroupCapacityDto> result = studentService.getGroupCapacity(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getGroupId()).isEqualTo(1L);
        assertThat(result.getData().getMaxCapacity()).isEqualTo(30);
        assertThat(result.getData().getCurrentEnrollments()).isEqualTo(20);
        assertThat(result.getData().getAvailableSpots()).isEqualTo(10);
        assertThat(result.getData().isFull()).isFalse();
    }
}