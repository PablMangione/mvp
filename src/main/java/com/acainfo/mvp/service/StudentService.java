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
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.RequestStatus;
import com.acainfo.mvp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar operaciones específicas de estudiantes.
 * Incluye consultas de asignaturas, grupos, inscripciones y solicitudes.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupRequestRepository groupRequestRepository;
    private final StudentMapper studentMapper;
    private final SubjectMapper subjectMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final GroupRequestMapper groupRequestMapper;

    /**
     * Obtiene el perfil detallado de un estudiante.
     *
     * @param studentId ID del estudiante
     * @return perfil detallado del estudiante
     */
    public ApiResponseDto<StudentDetailDto> getStudentProfile(Long studentId) {
        log.info("Obteniendo perfil del estudiante ID: {}", studentId);

        Student student = studentRepository.findByIdWithFullDetails(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        StudentDetailDto dto = studentMapper.toDetailDto(student);

        return ApiResponseDto.success(dto, "Perfil obtenido exitosamente");
    }

    /**
     * Obtiene las asignaturas disponibles para un estudiante según su carrera.
     *
     * @param studentId ID del estudiante
     * @param courseYear año de curso (opcional)
     * @param onlyWithActiveGroups mostrar solo asignaturas con grupos activos
     * @return lista de asignaturas
     */
    public ApiResponseDto<List<SubjectDto>> getAvailableSubjects(
            Long studentId,
            Integer courseYear,
            boolean onlyWithActiveGroups) {

        log.info("Obteniendo asignaturas para estudiante ID: {}, año: {}, solo activos: {}",
                studentId, courseYear, onlyWithActiveGroups);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        List<Subject> subjects;

        if (onlyWithActiveGroups) {
            subjects = subjectRepository.findSubjectsWithActiveGroups().stream()
                    .filter(s -> s.getMajor().equals(student.getMajor()))
                    .filter(s -> courseYear == null || s.getCourseYear().equals(courseYear))
                    .collect(Collectors.toList());
        } else {
            if (courseYear != null) {
                subjects = subjectRepository.findByMajor(student.getMajor()).stream()
                        .filter(s -> s.getCourseYear().equals(courseYear))
                        .collect(Collectors.toList());
            } else {
                subjects = subjectRepository.findByMajor(student.getMajor());
            }
        }

        List<SubjectDto> subjectDtos = subjectMapper.toDtoList(subjects);

        return ApiResponseDto.success(subjectDtos,
                String.format("Se encontraron %d asignaturas", subjectDtos.size()));
    }

    /**
     * Obtiene los grupos activos de una asignatura con información de disponibilidad.
     *
     * @param studentId ID del estudiante
     * @param subjectId ID de la asignatura
     * @return asignatura con sus grupos disponibles
     */
    public ApiResponseDto<SubjectWithGroupsDto> getSubjectWithActiveGroups(Long studentId, Long subjectId) {
        log.info("Obteniendo grupos activos de asignatura {} para estudiante {}", subjectId, studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        Subject subject = subjectRepository.findByIdWithFullDetails(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada"));

        // Verificar que la asignatura sea de la carrera del estudiante
        if (!subject.getMajor().equals(student.getMajor())) {
            throw new ValidationException("Esta asignatura no pertenece a tu carrera");
        }

        SubjectWithGroupsDto dto = subjectMapper.toWithGroupsDto(subject);

        return ApiResponseDto.success(dto, "Grupos obtenidos exitosamente");
    }

    /**
     * Crea una solicitud de grupo para una asignatura sin grupos activos.
     *
     * @param studentId ID del estudiante
     * @param requestDto datos de la solicitud
     * @return respuesta de la solicitud creada
     */
    @Transactional
    public ApiResponseDto<GroupRequestResponseDto> createGroupRequest(
            Long studentId,
            CreateGroupRequestDto requestDto) {

        log.info("Creando solicitud de grupo para estudiante {} y asignatura {}",
                studentId, requestDto.getSubjectId());

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        Subject subject = subjectRepository.findById(requestDto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada"));

        // Validaciones
        if (!subject.getMajor().equals(student.getMajor())) {
            throw new ValidationException("No puedes solicitar grupos de asignaturas de otras carreras");
        }

        // Verificar si ya tiene una solicitud pendiente
        boolean hasExistingRequest = groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(
                studentId, requestDto.getSubjectId(), RequestStatus.PENDING);

        if (hasExistingRequest) {
            throw new DuplicateRequestException("Ya tienes una solicitud pendiente para esta asignatura");
        }

        // Verificar si hay grupos activos
        boolean hasActiveGroups = subject.getCourseGroups().stream()
                .anyMatch(g -> g.getStatus() == CourseGroupStatus.ACTIVE);

        if (hasActiveGroups) {
            throw new ValidationException("Esta asignatura ya tiene grupos activos disponibles");
        }

        // Crear solicitud
        GroupRequest request = GroupRequest.builder()
                .student(student)
                .subject(subject)
                .requestDate(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();

        request = groupRequestRepository.save(request);

        // Contar total de solicitudes para esta asignatura
        int totalRequests = groupRequestRepository.countBySubjectIdAndStatus(
                subject.getId(), RequestStatus.PENDING).intValue();

        GroupRequestResponseDto response = GroupRequestResponseDto.builder()
                .requestId(request.getId())
                .success(true)
                .message("Solicitud creada exitosamente")
                .totalRequests(totalRequests)
                .build();

        log.info("Solicitud creada con ID: {}. Total solicitudes para la asignatura: {}",
                request.getId(), totalRequests);

        return ApiResponseDto.success(response, "Solicitud registrada exitosamente");
    }

    /**
     * Obtiene las inscripciones de un estudiante.
     *
     * @param studentId ID del estudiante
     * @param onlyActive mostrar solo inscripciones en grupos activos
     * @return lista de inscripciones
     */
    public ApiResponseDto<List<EnrollmentSummaryDto>> getStudentEnrollments(
            Long studentId,
            boolean onlyActive) {

        log.info("Obteniendo inscripciones del estudiante {}, solo activas: {}", studentId, onlyActive);

        List<Enrollment> enrollments;

        if (onlyActive) {
            enrollments = enrollmentRepository.findActiveEnrollmentsByStudent(studentId);
        } else {
            enrollments = enrollmentRepository.findByStudentId(studentId);
        }

        List<EnrollmentSummaryDto> summaries = enrollmentMapper.toSummaryDtoList(enrollments);

        return ApiResponseDto.success(summaries,
                String.format("Se encontraron %d inscripciones", summaries.size()));
    }

    /**
     * Obtiene las solicitudes de grupo de un estudiante.
     *
     * @param studentId ID del estudiante
     * @param status filtrar por estado (opcional)
     * @return lista de solicitudes
     */
    public ApiResponseDto<List<GroupRequestSummaryDto>> getStudentGroupRequests(
            Long studentId,
            RequestStatus status) {

        log.info("Obteniendo solicitudes del estudiante {}, estado: {}", studentId, status);

        List<GroupRequest> requests;

        if (status != null) {
            requests = groupRequestRepository.findByStudentIdAndStatus(studentId, status);
        } else {
            requests = groupRequestRepository.findByStudentId(studentId);
        }

        List<GroupRequestSummaryDto> summaries = groupRequestMapper.toSummaryDtoList(requests);

        return ApiResponseDto.success(summaries,
                String.format("Se encontraron %d solicitudes", summaries.size()));
    }

    /**
     * Verifica si un estudiante puede inscribirse en un grupo.
     *
     * @param studentId ID del estudiante
     * @param courseGroupId ID del grupo
     * @return true si puede inscribirse
     */
    public boolean canEnrollInGroup(Long studentId, Long courseGroupId) {
        log.debug("Verificando si estudiante {} puede inscribirse en grupo {}", studentId, courseGroupId);

        // Verificar si ya está inscrito
        if (enrollmentRepository.existsByStudentIdAndCourseGroupId(studentId, courseGroupId)) {
            log.debug("El estudiante ya está inscrito en el grupo");
            return false;
        }

        // Verificar estado del grupo y capacidad
        CourseGroup group = courseGroupRepository.findById(courseGroupId).orElse(null);
        if (group == null) {
            log.debug("El grupo no existe");
            return false;
        }

        if (group.getStatus() != CourseGroupStatus.ACTIVE) {
            log.debug("El grupo no está activo. Estado actual: {}", group.getStatus());
            return false;
        }

        // Verificar capacidad usando el campo maxCapacity
        boolean hasCapacity = group.hasCapacity();
        if (!hasCapacity) {
            log.debug("El grupo está lleno. Capacidad: {}/{}",
                    group.getEnrollments().size(), group.getMaxCapacity());
        }

        return hasCapacity;
    }

    /**
     * Obtiene información de capacidad de un grupo.
     *
     * @param courseGroupId ID del grupo
     * @return información de capacidad
     */
    public ApiResponseDto<GroupCapacityDto> getGroupCapacity(Long courseGroupId) {
        log.info("Obteniendo información de capacidad del grupo {}", courseGroupId);

        CourseGroup group = courseGroupRepository.findById(courseGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        GroupCapacityDto capacityDto = GroupCapacityDto.builder()
                .groupId(group.getId())
                .maxCapacity(group.getMaxCapacity())
                .currentEnrollments(group.getEnrollments().size())
                .availableSpots(group.getAvailableSpots())
                .isFull(!group.hasCapacity())
                .build();

        return ApiResponseDto.success(capacityDto, "Información de capacidad obtenida");
    }

    /**
     * DTO para información de capacidad del grupo
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GroupCapacityDto {
        private Long groupId;
        private Integer maxCapacity;
        private Integer currentEnrollments;
        private Integer availableSpots;
        private boolean isFull;
    }
}