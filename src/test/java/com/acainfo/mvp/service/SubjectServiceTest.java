package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.subject.*;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.SubjectMapper;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupRequest;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.RequestStatus;
import com.acainfo.mvp.repository.SubjectRepository;
import com.acainfo.mvp.util.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para SubjectService.
 * Verifica operaciones de consulta y gestión de asignaturas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectService Tests")
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private SubjectMapper subjectMapper;
    @Mock
    private SessionUtils sessionUtils;

    private SubjectService subjectService;

    private Subject testSubject1;
    private Subject testSubject2;
    private SubjectDto testSubjectDto1;
    private SubjectDto testSubjectDto2;
    private SubjectWithGroupsDto testSubjectWithGroupsDto;
    private CreateSubjectDto validCreateDto;
    private UpdateSubjectDto validUpdateDto;

    @BeforeEach
    void setUp() {
        subjectService = new SubjectService(subjectRepository, subjectMapper, sessionUtils);

        // Configurar datos de prueba - sin IDs (se simulan con mocks)
        testSubject1 = Subject.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .courseGroups(new HashSet<>())
                .groupRequests(new HashSet<>())
                .build();

        testSubject2 = Subject.builder()
                .name("Base de Datos")
                .major("Ingeniería Informática")
                .courseYear(2)
                .courseGroups(new HashSet<>())
                .groupRequests(new HashSet<>())
                .build();

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

        testSubjectWithGroupsDto = SubjectWithGroupsDto.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .activeGroups(2)
                .totalGroups(3)
                .hasActiveGroups(true)
                .availableGroups(new ArrayList<>())
                .build();

        validCreateDto = CreateSubjectDto.builder()
                .name("Algoritmos")
                .major("Ingeniería Informática")
                .courseYear(2)
                .build();

        validUpdateDto = UpdateSubjectDto.builder()
                .name("Algoritmos Avanzados")
                .courseYear(3)
                .build();
    }

    // ========== TESTS DE CONSULTAS PÚBLICAS ==========

    @Test
    @DisplayName("Debe obtener todas las asignaturas")
    void shouldGetAllSubjects() {
        // Given
        List<Subject> subjects = Arrays.asList(testSubject1, testSubject2);
        List<SubjectDto> expectedDtos = Arrays.asList(testSubjectDto1, testSubjectDto2);

        when(subjectRepository.findAll()).thenReturn(subjects);
        when(subjectMapper.toDtoList(subjects)).thenReturn(expectedDtos);

        // When
        List<SubjectDto> result = subjectService.getAllSubjects();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testSubjectDto1, testSubjectDto2);
        verify(subjectRepository).findAll();
    }

    @Test
    @DisplayName("Debe obtener asignatura por ID existente")
    void shouldGetSubjectByIdWhenExists() {
        // Given
        // Simulamos que el subject tiene ID cuando viene del repositorio
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject1));
        when(subjectMapper.toDto(testSubject1)).thenReturn(testSubjectDto1);

        // When
        SubjectDto result = subjectService.getSubjectById(1L);

        // Then
        assertThat(result).isEqualTo(testSubjectDto1);
        verify(subjectRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando asignatura no existe")
    void shouldThrowExceptionWhenSubjectNotFound() {
        // Given
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> subjectService.getSubjectById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Asignatura no encontrada con ID: 99");
    }

    @Test
    @DisplayName("Debe obtener asignaturas por carrera")
    void shouldGetSubjectsByMajor() {
        // Given
        String major = "Ingeniería Informática";
        List<Subject> subjects = Arrays.asList(testSubject1, testSubject2);
        List<SubjectDto> expectedDtos = Arrays.asList(testSubjectDto1, testSubjectDto2);

        when(subjectRepository.findByMajor(major)).thenReturn(subjects);
        when(subjectMapper.toDtoList(subjects)).thenReturn(expectedDtos);

        // When
        List<SubjectDto> result = subjectService.getSubjectsByMajor(major);

        // Then
        assertThat(result).hasSize(2);
        verify(subjectRepository).findByMajor(major);
    }

    @Test
    @DisplayName("Debe validar carrera no vacía")
    void shouldValidateMajorNotEmpty() {
        // When/Then
        assertThatThrownBy(() -> subjectService.getSubjectsByMajor(""))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La carrera no puede estar vacía");

        assertThatThrownBy(() -> subjectService.getSubjectsByMajor(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La carrera no puede estar vacía");
    }

    @Test
    @DisplayName("Debe obtener asignaturas por año de curso")
    void shouldGetSubjectsByCourseYear() {
        // Given
        List<Subject> subjects = Collections.singletonList(testSubject1);
        List<SubjectDto> expectedDtos = Collections.singletonList(testSubjectDto1);

        when(subjectRepository.findByCourseYear(1)).thenReturn(subjects);
        when(subjectMapper.toDtoList(subjects)).thenReturn(expectedDtos);

        // When
        List<SubjectDto> result = subjectService.getSubjectsByCourseYear(1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testSubjectDto1);
    }

    @Test
    @DisplayName("Debe validar año de curso entre 1 y 6")
    void shouldValidateCourseYearRange() {
        // When/Then
        assertThatThrownBy(() -> subjectService.getSubjectsByCourseYear(0))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El año de curso debe estar entre 1 y 6");

        assertThatThrownBy(() -> subjectService.getSubjectsByCourseYear(7))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El año de curso debe estar entre 1 y 6");

        assertThatThrownBy(() -> subjectService.getSubjectsByCourseYear(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El año de curso debe estar entre 1 y 6");
    }

    @Test
    @DisplayName("Debe obtener asignaturas con grupos activos")
    void shouldGetSubjectsWithActiveGroups() {
        // Given
        List<Subject> subjects = Collections.singletonList(testSubject1);
        List<SubjectWithGroupsDto> expectedDtos = Collections.singletonList(testSubjectWithGroupsDto);

        when(subjectRepository.findSubjectsWithActiveGroups()).thenReturn(subjects);
        when(subjectMapper.toWithGroupsDtoList(subjects)).thenReturn(expectedDtos);

        // When
        List<SubjectWithGroupsDto> result = subjectService.getSubjectsWithActiveGroups();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testSubjectWithGroupsDto);
    }

    @Test
    @DisplayName("Debe filtrar asignaturas según criterios")
    void shouldFilterSubjects() {
        // Given
        SubjectFilterDto filter = SubjectFilterDto.builder()
                .major("Ingeniería Informática")
                .courseYear(1)
                .onlyWithActiveGroups(false)
                .build();

        // Agregar un grupo activo a testSubject1
        CourseGroup activeGroup = CourseGroup.builder()
                .status(CourseGroupStatus.ACTIVE)
                .build();
        testSubject1.getCourseGroups().add(activeGroup);

        List<Subject> allSubjects = Arrays.asList(testSubject1, testSubject2);
        when(subjectRepository.findAll()).thenReturn(allSubjects);
        when(subjectMapper.toDtoList(any())).thenReturn(Collections.singletonList(testSubjectDto1));

        // When
        List<SubjectDto> result = subjectService.getSubjectsFiltered(filter);

        // Then
        assertThat(result).hasSize(1);
        verify(subjectRepository).findAll();
    }

    @Test
    @DisplayName("Debe obtener asignatura con grupos")
    void shouldGetSubjectWithGroups() {
        // Given
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject1));
        when(subjectMapper.toWithGroupsDto(testSubject1)).thenReturn(testSubjectWithGroupsDto);

        // When
        SubjectWithGroupsDto result = subjectService.getSubjectWithGroups(1L);

        // Then
        assertThat(result).isEqualTo(testSubjectWithGroupsDto);
        assertThat(result.isHasActiveGroups()).isTrue();
    }

    // ========== TESTS DE OPERACIONES ADMINISTRATIVAS ==========

    @Test
    @DisplayName("Debe crear asignatura como administrador")
    void shouldCreateSubjectAsAdmin() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(subjectRepository.findAll()).thenReturn(new ArrayList<>());
        when(subjectMapper.toEntity(validCreateDto)).thenReturn(testSubject1);
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject1);
        when(subjectMapper.toDto(testSubject1)).thenReturn(testSubjectDto1);

        // When
        SubjectDto result = subjectService.createSubject(validCreateDto);

        // Then
        assertThat(result).isEqualTo(testSubjectDto1);
        verify(subjectRepository).save(any(Subject.class));
    }

    @Test
    @DisplayName("Debe fallar al crear asignatura sin permisos de admin")
    void shouldFailToCreateSubjectWithoutAdminRole() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(false);
        when(sessionUtils.getCurrentUserEmail()).thenReturn("user@test.com");

        // When/Then
        assertThatThrownBy(() -> subjectService.createSubject(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No tiene permisos para realizar esta operación");

        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al crear asignatura duplicada")
    void shouldFailToCreateDuplicateSubject() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);

        Subject existingSubject = Subject.builder()
                .name("Algoritmos")
                .major("Ingeniería Informática")
                .build();
        when(subjectRepository.findAll()).thenReturn(Collections.singletonList(existingSubject));

        // When/Then
        assertThatThrownBy(() -> subjectService.createSubject(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Ya existe una asignatura con ese nombre en la carrera especificada");
    }

    @Test
    @DisplayName("Debe actualizar asignatura como administrador")
    void shouldUpdateSubjectAsAdmin() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject1));
        when(subjectRepository.findAll()).thenReturn(Collections.emptyList());
        when(subjectRepository.save(any(Subject.class))).thenReturn(testSubject1);
        when(subjectMapper.toDto(testSubject1)).thenReturn(testSubjectDto1);

        // When
        SubjectDto result = subjectService.updateSubject(1L, validUpdateDto);

        // Then
        verify(subjectMapper).updateFromDto(testSubject1, validUpdateDto);
        verify(subjectRepository).save(testSubject1);
    }

    @Test
    @DisplayName("Debe eliminar asignatura sin dependencias")
    void shouldDeleteSubjectWithoutDependencies() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject1));

        // When
        subjectService.deleteSubject(1L);

        // Then
        verify(subjectRepository).delete(testSubject1);
    }

    @Test
    @DisplayName("Debe fallar al eliminar asignatura con grupos")
    void shouldFailToDeleteSubjectWithGroups() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);

        CourseGroup group = CourseGroup.builder()
                .status(CourseGroupStatus.ACTIVE)
                .build();
        testSubject1.getCourseGroups().add(group);

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject1));

        // When/Then
        assertThatThrownBy(() -> subjectService.deleteSubject(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se puede eliminar la asignatura porque tiene grupos asociados");

        verify(subjectRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe fallar al eliminar asignatura con solicitudes")
    void shouldFailToDeleteSubjectWithRequests() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);

        GroupRequest request = GroupRequest.builder()
                .status(RequestStatus.PENDING)
                .build();
        testSubject1.getGroupRequests().add(request);

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject1));

        // When/Then
        assertThatThrownBy(() -> subjectService.deleteSubject(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No se puede eliminar la asignatura porque tiene solicitudes de grupo pendientes");

        verify(subjectRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe obtener estadísticas de asignatura")
    void shouldGetSubjectStats() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);

        // Configurar grupos con diferentes estados
        CourseGroup activeGroup = CourseGroup.builder()
                .status(CourseGroupStatus.ACTIVE)
                .enrollments(new HashSet<>())
                .build();
        CourseGroup plannedGroup = CourseGroup.builder()
                .status(CourseGroupStatus.PLANNED)
                .enrollments(new HashSet<>())
                .build();
        CourseGroup closedGroup = CourseGroup.builder()
                .status(CourseGroupStatus.CLOSED)
                .enrollments(new HashSet<>())
                .build();

        testSubject1.getCourseGroups().addAll(Arrays.asList(activeGroup, plannedGroup, closedGroup));

        // Agregar solicitud pendiente
        GroupRequest pendingRequest = GroupRequest.builder()
                .status(RequestStatus.PENDING)
                .build();
        testSubject1.getGroupRequests().add(pendingRequest);

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject1));

        // When
        SubjectStatsDto stats = subjectService.getSubjectStats(1L);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getSubjectId()).isEqualTo(1L);
        assertThat(stats.getTotalGroups()).isEqualTo(3);
        assertThat(stats.getActiveGroups()).isEqualTo(1);
        assertThat(stats.getPlannedGroups()).isEqualTo(1);
        assertThat(stats.getClosedGroups()).isEqualTo(1);
        assertThat(stats.getPendingGroupRequests()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe manejar excepción de integridad al crear")
    void shouldHandleDataIntegrityViolationOnCreate() {
        // Given
        when(sessionUtils.isAdmin()).thenReturn(true);
        when(subjectRepository.findAll()).thenReturn(new ArrayList<>());
        when(subjectMapper.toEntity(validCreateDto)).thenReturn(testSubject1);
        when(subjectRepository.save(any(Subject.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        // When/Then
        assertThatThrownBy(() -> subjectService.createSubject(validCreateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Error al crear la asignatura. Verifique que no exista una con el mismo nombre y carrera");
    }
}