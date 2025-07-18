package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.grouprequest.*;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.GroupRequestMapper;
import com.acainfo.mvp.model.GroupRequest;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.RequestStatus;
import com.acainfo.mvp.repository.GroupRequestRepository;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.SubjectRepository;
import com.acainfo.mvp.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para GroupRequestService.
 * Verifica la lógica de negocio para solicitudes de creación de grupos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroupRequest Service Tests")
class GroupRequestServiceTest {

    @Mock
    private GroupRequestRepository groupRequestRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private GroupRequestMapper groupRequestMapper;

    @Mock
    private SessionUtils sessionUtils;

    private GroupRequestService groupRequestService;

    private Student testStudent;
    private Subject testSubject;
    private GroupRequest testGroupRequest;
    private CreateGroupRequestDto createDto;

    @BeforeEach
    void setUp() throws Exception {
        groupRequestService = new GroupRequestService(
                groupRequestRepository,
                studentRepository,
                subjectRepository,
                groupRequestMapper,
                sessionUtils
        );

        // Configurar entidades de prueba con IDs usando reflection
        testStudent = Student.builder()
                .name("Juan Pérez")
                .email("juan.perez@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();
        setId(testStudent, 1L);

        testSubject = Subject.builder()
                .name("Programación Avanzada")
                .major("Ingeniería Informática")
                .courseYear(2)
                .build();
        setId(testSubject, 10L);

        testGroupRequest = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .status(RequestStatus.PENDING)
                .build();
        setId(testGroupRequest, 100L);
        setCreatedAt(testGroupRequest, LocalDateTime.now());

        createDto = CreateGroupRequestDto.builder()
                .subjectId(10L)
                .comments("Necesitamos un grupo adicional")
                .build();
    }

    // Método helper para setear ID usando reflection
    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    // Método helper para setear createdAt usando reflection
    private void setCreatedAt(Object entity, LocalDateTime createdAt) throws Exception {
        Field field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(entity, createdAt);
    }

    @Test
    @DisplayName("Debe crear solicitud de grupo exitosamente")
    void shouldCreateGroupRequestSuccessfully() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(testSubject));
        when(groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(1L, 10L, RequestStatus.PENDING))
                .thenReturn(false);
        when(groupRequestMapper.toEntity(createDto, testStudent, testSubject)).thenReturn(testGroupRequest);
        when(groupRequestRepository.save(any(GroupRequest.class))).thenReturn(testGroupRequest);
        when(groupRequestRepository.findByStatus(RequestStatus.PENDING))
                .thenReturn(Arrays.asList(testGroupRequest));

        GroupRequestResponseDto expectedResponse = GroupRequestResponseDto.builder()
                .requestId(100L)
                .success(true)
                .message("Solicitud creada exitosamente. El administrador revisará tu solicitud.")
                .totalRequests(1)
                .build();
        when(groupRequestMapper.toResponseDto(eq(testGroupRequest), eq(true), anyString(), eq(1)))
                .thenReturn(expectedResponse);

        // When
        GroupRequestResponseDto result = groupRequestService.createGroupRequest(createDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRequestId()).isEqualTo(100L);
        assertThat(result.getTotalRequests()).isEqualTo(1);

        verify(groupRequestRepository).save(any(GroupRequest.class));
        verify(groupRequestMapper).toEntity(createDto, testStudent, testSubject);
    }

    @Test
    @DisplayName("Debe fallar al crear solicitud si no es estudiante")
    void shouldFailToCreateRequestIfNotStudent() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(false);
        when(groupRequestMapper.toErrorResponse(anyString()))
                .thenReturn(GroupRequestResponseDto.builder()
                        .success(false)
                        .message("Solo los estudiantes pueden solicitar la creación de grupos")
                        .build());

        // When
        GroupRequestResponseDto result = groupRequestService.createGroupRequest(createDto);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Solo los estudiantes");

        verify(groupRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar si el estudiante no está en la carrera de la asignatura")
    void shouldFailIfStudentNotInSubjectMajor() {
        // Given
        Student otherMajorStudent = Student.builder()
                .name("María García")
                .email("maria@universidad.edu")
                .major("Ingeniería Civil")
                .build();

        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(2L);
        when(studentRepository.findById(2L)).thenReturn(Optional.of(otherMajorStudent));
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(testSubject));
        when(groupRequestMapper.toErrorResponse(anyString()))
                .thenReturn(GroupRequestResponseDto.builder()
                        .success(false)
                        .message("La asignatura no pertenece a tu carrera")
                        .build());

        // When
        GroupRequestResponseDto result = groupRequestService.createGroupRequest(createDto);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("no pertenece a tu carrera");
    }

    @Test
    @DisplayName("Debe fallar si ya existe solicitud pendiente")
    void shouldFailIfPendingRequestExists() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(testSubject));
        when(groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(1L, 10L, RequestStatus.PENDING))
                .thenReturn(true);
        when(groupRequestMapper.toErrorResponse(anyString()))
                .thenReturn(GroupRequestResponseDto.builder()
                        .success(false)
                        .message("Ya tienes una solicitud pendiente para esta asignatura")
                        .build());

        // When
        GroupRequestResponseDto result = groupRequestService.createGroupRequest(createDto);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("solicitud pendiente");
    }

    @Test
    @DisplayName("Debe obtener solicitudes del estudiante actual")
    void shouldGetMyGroupRequests() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(groupRequestRepository.findAll()).thenReturn(Arrays.asList(testGroupRequest));

        GroupRequestDto expectedDto = GroupRequestDto.builder()
                .studentId(1L)
                .studentName("Juan Pérez")
                .subjectId(10L)
                .subjectName("Programación Avanzada")
                .status(RequestStatus.PENDING)
                .build();
        when(groupRequestMapper.toDtoList(anyList())).thenReturn(Arrays.asList(expectedDto));

        // When
        List<GroupRequestDto> result = groupRequestService.getMyGroupRequests();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentId()).isEqualTo(1L);
        assertThat(result.get(0).getSubjectId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Debe fallar al obtener solicitudes si no es estudiante")
    void shouldFailToGetRequestsIfNotStudent() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> groupRequestService.getMyGroupRequests())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Solo los estudiantes pueden consultar sus solicitudes");
    }

    @Test
    @DisplayName("Debe obtener solicitudes pendientes para admin")
    void shouldGetPendingRequestsForAdmin() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(groupRequestRepository.findByStatus(RequestStatus.PENDING))
                .thenReturn(Arrays.asList(testGroupRequest));

        GroupRequestDto expectedDto = GroupRequestDto.builder()
                .studentId(1L)
                .studentName("Juan Pérez")
                .subjectId(10L)
                .subjectName("Programación Avanzada")
                .status(RequestStatus.PENDING)
                .build();
        when(groupRequestMapper.toDtoList(anyList())).thenReturn(Arrays.asList(expectedDto));

        // When
        List<GroupRequestDto> result = groupRequestService.getPendingRequests();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RequestStatus.PENDING);

        verify(groupRequestRepository).findByStatus(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("Debe actualizar estado de solicitud como admin")
    void shouldUpdateRequestStatusAsAdmin() throws Exception {
        // Given
        UpdateRequestStatusDto updateDto = UpdateRequestStatusDto.builder()
                .status(RequestStatus.APPROVED)
                .adminComments("Grupo aprobado")
                .build();

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(groupRequestRepository.findById(100L)).thenReturn(Optional.of(testGroupRequest));
        when(groupRequestRepository.save(any(GroupRequest.class))).thenReturn(testGroupRequest);

        GroupRequestDto expectedDto = GroupRequestDto.builder()
                .studentId(1L)
                .studentName("Juan Pérez")
                .subjectId(10L)
                .subjectName("Programación Avanzada")
                .status(RequestStatus.APPROVED)
                .build();
        when(groupRequestMapper.toDto(testGroupRequest)).thenReturn(expectedDto);

        // When
        GroupRequestDto result = groupRequestService.updateRequestStatus(100L, updateDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);

        verify(groupRequestMapper).updateStatus(testGroupRequest, updateDto);
        verify(groupRequestRepository).save(testGroupRequest);
    }

    @Test
    @DisplayName("Debe fallar al actualizar si no es admin")
    void shouldFailToUpdateIfNotAdmin() {
        // Given
        UpdateRequestStatusDto updateDto = UpdateRequestStatusDto.builder()
                .status(RequestStatus.APPROVED)
                .build();

        when(sessionUtils.isAdmin()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> groupRequestService.updateRequestStatus(100L, updateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Solo los administradores");
    }

    @Test
    @DisplayName("Debe verificar si estudiante puede solicitar grupo")
    void shouldCheckIfCanRequestGroup() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(1L, 10L, RequestStatus.PENDING))
                .thenReturn(false);

        // When
        boolean canRequest = groupRequestService.canRequestGroup(10L);

        // Then
        assertThat(canRequest).isTrue();
    }

    @Test
    @DisplayName("Debe obtener estadísticas de solicitudes por asignatura")
    void shouldGetRequestStatsBySubject() throws Exception {
        // Given
        GroupRequest approvedRequest = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .status(RequestStatus.APPROVED)
                .build();
        setId(approvedRequest, 101L);

        when(sessionUtils.isAdmin()).thenReturn(true);
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(testSubject));
        when(groupRequestRepository.findAll())
                .thenReturn(Arrays.asList(testGroupRequest, approvedRequest));

        // When
        GroupRequestStatsDto stats = groupRequestService.getRequestStatsBySubject(10L);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getSubjectId()).isEqualTo(10L);
        assertThat(stats.getSubjectName()).isEqualTo("Programación Avanzada");
        assertThat(stats.getTotalRequests()).isEqualTo(2);
        assertThat(stats.getPendingRequests()).isEqualTo(1);
        assertThat(stats.getApprovedRequests()).isEqualTo(1);
        assertThat(stats.getRejectedRequests()).isEqualTo(0);
    }

    @Test
    @DisplayName("Debe manejar error cuando asignatura no existe")
    void shouldHandleSubjectNotFound() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(subjectRepository.findById(10L)).thenReturn(Optional.empty());
        when(groupRequestMapper.toErrorResponse(anyString()))
                .thenReturn(GroupRequestResponseDto.builder()
                        .success(false)
                        .message("Asignatura no encontrada con ID: 10")
                        .build());

        // When
        GroupRequestResponseDto result = groupRequestService.createGroupRequest(createDto);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Asignatura no encontrada");
    }

    @Test
    @DisplayName("Debe manejar lista vacía de solicitudes")
    void shouldHandleEmptyRequestList() {
        // Given
        when(sessionUtils.isStudent()).thenReturn(true);
        when(sessionUtils.getCurrentUserId()).thenReturn(1L);
        when(groupRequestRepository.findAll()).thenReturn(Collections.emptyList());
        when(groupRequestMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());

        // When
        List<GroupRequestDto> result = groupRequestService.getMyGroupRequests();

        // Then
        assertThat(result).isEmpty();
    }
}
