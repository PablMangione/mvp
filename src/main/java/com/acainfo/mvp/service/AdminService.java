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
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.DayOfWeek;
import com.acainfo.mvp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de administración del sistema.
 * Gestiona operaciones de alta/baja de usuarios, asignaturas, grupos y cambios de estado.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final GroupSessionRepository groupSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupRequestRepository groupRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final SubjectMapper subjectMapper;
    private final CourseGroupMapper courseGroupMapper;

    // ============================================
    // GESTIÓN DE ALUMNOS
    // ============================================

    /**
     * Crea un nuevo alumno en el sistema.
     *
     * @param createDto datos del alumno a crear
     * @return alumno creado
     */
    public ApiResponseDto<StudentDto> createStudent(CreateStudentDto createDto) {
        log.info("Creando nuevo alumno: {}", createDto.getEmail());

        // Verificar si el email ya existe
        if (studentRepository.existsByEmail(createDto.getEmail()) ||
                teacherRepository.existsByEmail(createDto.getEmail())) {
            log.warn("Email ya existe: {}", createDto.getEmail());
            throw new EmailAlreadyExistsException("El email ya está registrado en el sistema");
        }

        // Crear nuevo alumno
        Student student = Student.builder()
                .name(createDto.getName())
                .email(createDto.getEmail())
                .password(passwordEncoder.encode(createDto.getPassword()))
                .major(createDto.getMajor())
                .build();

        student = studentRepository.save(student);
        log.info("Alumno creado exitosamente con ID: {}", student.getId());

        StudentDto dto = studentMapper.toDto(student);
        return ApiResponseDto.success(dto, "Alumno creado exitosamente");
    }

    /**
     * Actualiza los datos de un alumno.
     *
     * @param studentId ID del alumno
     * @param updateDto datos a actualizar
     * @return alumno actualizado
     */
    public ApiResponseDto<StudentDto> updateStudent(Long studentId, UpdateStudentDto updateDto) {
        log.info("Actualizando alumno ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        // Verificar email si se está cambiando
        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(student.getEmail())) {
            if (studentRepository.existsByEmail(updateDto.getEmail()) ||
                    teacherRepository.existsByEmail(updateDto.getEmail())) {
                throw new EmailAlreadyExistsException("El email ya está registrado");
            }
        }

        studentMapper.updateEntityFromDto(student, updateDto);
        student = studentRepository.save(student);

        log.info("Alumno actualizado exitosamente");
        StudentDto dto = studentMapper.toDto(student);
        return ApiResponseDto.success(dto, "Alumno actualizado exitosamente");
    }

    /**
     * Elimina un alumno del sistema.
     *
     * @param studentId ID del alumno
     * @return confirmación de eliminación
     */
    public ApiResponseDto<DeleteResponseDto> deleteStudent(Long studentId) {
        log.info("Eliminando alumno ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        // Verificar si tiene inscripciones activas
        boolean hasActiveEnrollments = student.getEnrollments().stream()
                .anyMatch(e -> e.getCourseGroup().getStatus() == CourseGroupStatus.ACTIVE);

        if (hasActiveEnrollments) {
            throw new ValidationException("No se puede eliminar un alumno con inscripciones en grupos activos");
        }

        studentRepository.delete(student);
        log.info("Alumno eliminado exitosamente");

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(studentId)
                .entityType("Student")
                .success(true)
                .message("Alumno eliminado exitosamente")
                .build();

        return ApiResponseDto.success(response, "Alumno eliminado exitosamente");
    }

    /**
     * Lista todos los alumnos con paginación.
     *
     * @param pageable configuración de paginación
     * @return página de alumnos
     */
    @Transactional(readOnly = true)
    public Page<StudentDto> listStudents(Pageable pageable) {
        log.info("Listando alumnos, página: {}, tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return studentRepository.findAll(pageable)
                .map(studentMapper::toDto);
    }

    // ============================================
    // GESTIÓN DE PROFESORES
    // ============================================

    /**
     * Crea un nuevo profesor en el sistema.
     *
     * @param createDto datos del profesor a crear
     * @return profesor creado
     */
    public ApiResponseDto<TeacherDto> createTeacher(CreateTeacherDto createDto) {
        log.info("Creando nuevo profesor: {}", createDto.getEmail());

        // Verificar si el email ya existe
        if (teacherRepository.existsByEmail(createDto.getEmail()) ||
                studentRepository.existsByEmail(createDto.getEmail())) {
            log.warn("Email ya existe: {}", createDto.getEmail());
            throw new EmailAlreadyExistsException("El email ya está registrado en el sistema");
        }

        // Crear nuevo profesor
        Teacher teacher = Teacher.builder()
                .name(createDto.getName())
                .email(createDto.getEmail())
                .password(passwordEncoder.encode(createDto.getPassword()))
                .build();

        teacher = teacherRepository.save(teacher);
        log.info("Profesor creado exitosamente con ID: {}", teacher.getId());

        TeacherDto dto = teacherMapper.toDto(teacher);
        return ApiResponseDto.success(dto, "Profesor creado exitosamente");
    }

    /**
     * Actualiza los datos de un profesor.
     *
     * @param teacherId ID del profesor
     * @param updateDto datos a actualizar
     * @return profesor actualizado
     */
    public ApiResponseDto<TeacherDto> updateTeacher(Long teacherId, UpdateTeacherDto updateDto) {
        log.info("Actualizando profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        // Verificar email si se está cambiando
        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(teacher.getEmail())) {
            if (teacherRepository.existsByEmail(updateDto.getEmail()) ||
                    studentRepository.existsByEmail(updateDto.getEmail())) {
                throw new EmailAlreadyExistsException("El email ya está registrado");
            }
        }

        teacherMapper.updateEntityFromDto(teacher, updateDto);
        teacher = teacherRepository.save(teacher);

        log.info("Profesor actualizado exitosamente");
        TeacherDto dto = teacherMapper.toDto(teacher);
        return ApiResponseDto.success(dto, "Profesor actualizado exitosamente");
    }

    /**
     * Elimina un profesor del sistema.
     *
     * @param teacherId ID del profesor
     * @return confirmación de eliminación
     */
    public ApiResponseDto<DeleteResponseDto> deleteTeacher(Long teacherId) {
        log.info("Eliminando profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        // Verificar si tiene grupos activos
        boolean hasActiveGroups = teacher.getCourseGroups().stream()
                .anyMatch(g -> g.getStatus() == CourseGroupStatus.ACTIVE);

        if (hasActiveGroups) {
            throw new ValidationException("No se puede eliminar un profesor con grupos activos asignados");
        }

        // La eliminación establecerá teacher_id = NULL en los grupos gracias a ON DELETE SET NULL
        teacherRepository.delete(teacher);
        log.info("Profesor eliminado exitosamente");

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(teacherId)
                .entityType("Teacher")
                .success(true)
                .message("Profesor eliminado exitosamente")
                .build();

        return ApiResponseDto.success(response, "Profesor eliminado exitosamente");
    }

    /**
     * Lista todos los profesores con paginación.
     *
     * @param pageable configuración de paginación
     * @return página de profesores
     */
    @Transactional(readOnly = true)
    public Page<TeacherDto> listTeachers(Pageable pageable) {
        log.info("Listando profesores, página: {}, tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return teacherRepository.findAll(pageable)
                .map(teacherMapper::toDto);
    }

    // ============================================
    // GESTIÓN DE ASIGNATURAS
    // ============================================

    /**
     * Crea una nueva asignatura.
     *
     * @param createDto datos de la asignatura
     * @return asignatura creada
     */
    public ApiResponseDto<SubjectDto> createSubject(CreateSubjectDto createDto) {
        log.info("Creando nueva asignatura: {} - {}", createDto.getName(), createDto.getMajor());

        // Verificar si ya existe
        if (subjectRepository.existsByNameAndMajor(createDto.getName(), createDto.getMajor())) {
            throw new ValidationException("Ya existe una asignatura con ese nombre en esa carrera");
        }

        Subject subject = subjectMapper.toEntity(createDto);
        subject = subjectRepository.save(subject);

        log.info("Asignatura creada exitosamente con ID: {}", subject.getId());
        SubjectDto dto = subjectMapper.toDto(subject);
        return ApiResponseDto.success(dto, "Asignatura creada exitosamente");
    }

    /**
     * Actualiza una asignatura.
     *
     * @param subjectId ID de la asignatura
     * @param updateDto datos a actualizar
     * @return asignatura actualizada
     */
    public ApiResponseDto<SubjectDto> updateSubject(Long subjectId, UpdateSubjectDto updateDto) {
        log.info("Actualizando asignatura ID: {}", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada"));

        // Verificar unicidad si se cambia nombre o carrera
        if ((updateDto.getName() != null || updateDto.getMajor() != null)) {
            String newName = updateDto.getName() != null ? updateDto.getName() : subject.getName();
            String newMajor = updateDto.getMajor() != null ? updateDto.getMajor() : subject.getMajor();

            if (!newName.equals(subject.getName()) || !newMajor.equals(subject.getMajor())) {
                if (subjectRepository.existsByNameAndMajor(newName, newMajor)) {
                    throw new ValidationException("Ya existe una asignatura con ese nombre en esa carrera");
                }
            }
        }

        subjectMapper.updateEntityFromDto(subject, updateDto);
        subject = subjectRepository.save(subject);

        log.info("Asignatura actualizada exitosamente");
        SubjectDto dto = subjectMapper.toDto(subject);
        return ApiResponseDto.success(dto, "Asignatura actualizada exitosamente");
    }

    /**
     * Elimina una asignatura.
     *
     * @param subjectId ID de la asignatura
     * @return confirmación de eliminación
     */
    public ApiResponseDto<DeleteResponseDto> deleteSubject(Long subjectId) {
        log.info("Eliminando asignatura ID: {}", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada"));

        // Verificar si tiene grupos activos
        boolean hasActiveGroups = subject.getCourseGroups().stream()
                .anyMatch(g -> g.getStatus() == CourseGroupStatus.ACTIVE);

        if (hasActiveGroups) {
            throw new ValidationException("No se puede eliminar una asignatura con grupos activos");
        }

        // Verificar si tiene solicitudes pendientes
        boolean hasPendingRequests = subject.getGroupRequests().stream()
                .anyMatch(r -> r.getStatus() == com.acainfo.mvp.model.enums.RequestStatus.PENDING);

        if (hasPendingRequests) {
            throw new ValidationException("No se puede eliminar una asignatura con solicitudes pendientes");
        }

        subjectRepository.delete(subject);
        log.info("Asignatura eliminada exitosamente");

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(subjectId)
                .entityType("Subject")
                .success(true)
                .message("Asignatura eliminada exitosamente")
                .build();

        return ApiResponseDto.success(response, "Asignatura eliminada exitosamente");
    }

    /**
     * Lista todas las asignaturas con paginación.
     *
     * @param pageable configuración de paginación
     * @return página de asignaturas
     */
    @Transactional(readOnly = true)
    public Page<SubjectDto> listSubjects(Pageable pageable) {
        log.info("Listando asignaturas, página: {}, tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return subjectRepository.findAll(pageable)
                .map(subjectMapper::toDto);
    }

    // ============================================
    // GESTIÓN DE GRUPOS
    // ============================================

    /**
     * Crea un nuevo grupo.
     *
     * @param createDto datos del grupo
     * @return grupo creado
     */
    public ApiResponseDto<CourseGroupDto> createCourseGroup(CreateCourseGroupDto createDto) {
        log.info("Creando nuevo grupo para asignatura ID: {}", createDto.getSubjectId());

        Subject subject = subjectRepository.findById(createDto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada"));

        Teacher teacher = null;
        if (createDto.getTeacherId() != null) {
            teacher = teacherRepository.findById(createDto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));
        }

        // Crear grupo
        CourseGroup group = CourseGroup.builder()
                .subject(subject)
                .teacher(teacher)
                .status(CourseGroupStatus.PLANNED)
                .type(createDto.getType())
                .price(createDto.getPrice())
                .maxCapacity(30) // Valor por defecto
                .build();

        group = courseGroupRepository.save(group);

        // Crear sesiones si se proporcionaron
        if (createDto.getSessions() != null && !createDto.getSessions().isEmpty()) {
            for (CreateGroupSessionDto sessionDto : createDto.getSessions()) {
                GroupSession session = GroupSession.builder()
                        .courseGroup(group)
                        .dayOfWeek(DayOfWeek.valueOf(sessionDto.getDayOfWeek()))
                        .startTime(sessionDto.getStartTime())
                        .endTime(sessionDto.getEndTime())
                        .classroom(sessionDto.getClassroom())
                        .build();
                groupSessionRepository.save(session);
            }
        }

        log.info("Grupo creado exitosamente con ID: {}", group.getId());
        CourseGroupDto dto = courseGroupMapper.toDto(group);
        return ApiResponseDto.success(dto, "Grupo creado exitosamente");
    }

    /**
     * Cambia el estado de un grupo.
     *
     * @param groupId ID del grupo
     * @param updateDto nuevo estado
     * @return grupo actualizado
     */
    public ApiResponseDto<CourseGroupDto> updateGroupStatus(Long groupId, UpdateGroupStatusDto updateDto) {
        log.info("Actualizando estado del grupo {} a {}", groupId, updateDto.getStatus());

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        // Validar transiciones de estado
        CourseGroupStatus currentStatus = group.getStatus();
        CourseGroupStatus newStatus = updateDto.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        // Validaciones adicionales para activar un grupo
        if (newStatus == CourseGroupStatus.ACTIVE) {
            if (group.getTeacher() == null) {
                throw new ValidationException("No se puede activar un grupo sin profesor asignado");
            }
            if (group.getGroupSessions().isEmpty()) {
                throw new ValidationException("No se puede activar un grupo sin sesiones programadas");
            }
        }

        group.setStatus(newStatus);
        group = courseGroupRepository.save(group);

        log.info("Estado del grupo actualizado exitosamente");
        CourseGroupDto dto = courseGroupMapper.toDto(group);
        return ApiResponseDto.success(dto, "Estado actualizado exitosamente");
    }

    /**
     * Asigna un profesor a un grupo.
     *
     * @param groupId ID del grupo
     * @param assignDto datos de asignación
     * @return grupo actualizado
     */
    public ApiResponseDto<CourseGroupDto> assignTeacherToGroup(Long groupId, AssignTeacherDto assignDto) {
        log.info("Asignando profesor {} al grupo {}", assignDto.getTeacherId(), groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        Teacher teacher = teacherRepository.findById(assignDto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        // Verificar que el grupo no esté cerrado
        if (group.getStatus() == CourseGroupStatus.CLOSED) {
            throw new ValidationException("No se puede asignar un profesor a un grupo cerrado");
        }

        group.setTeacher(teacher);
        group = courseGroupRepository.save(group);

        log.info("Profesor asignado exitosamente al grupo");
        CourseGroupDto dto = courseGroupMapper.toDto(group);
        return ApiResponseDto.success(dto, "Profesor asignado exitosamente");
    }

    /**
     * Elimina un grupo.
     *
     * @param groupId ID del grupo
     * @return confirmación de eliminación
     */
    public ApiResponseDto<DeleteResponseDto> deleteCourseGroup(Long groupId) {
        log.info("Eliminando grupo ID: {}", groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        // Solo se pueden eliminar grupos en estado PLANNED
        if (group.getStatus() != CourseGroupStatus.PLANNED) {
            throw new ValidationException("Solo se pueden eliminar grupos en estado PLANNED");
        }

        // Verificar que no tenga inscripciones
        if (!group.getEnrollments().isEmpty()) {
            throw new ValidationException("No se puede eliminar un grupo con inscripciones");
        }

        courseGroupRepository.delete(group);
        log.info("Grupo eliminado exitosamente");

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(groupId)
                .entityType("CourseGroup")
                .success(true)
                .message("Grupo eliminado exitosamente")
                .build();

        return ApiResponseDto.success(response, "Grupo eliminado exitosamente");
    }

    /**
     * Lista todos los grupos con filtros opcionales.
     *
     * @param status filtrar por estado (opcional)
     * @param teacherId filtrar por profesor (opcional)
     * @param subjectId filtrar por asignatura (opcional)
     * @param pageable configuración de paginación
     * @return página de grupos
     */
    @Transactional(readOnly = true)
    public Page<CourseGroupDto> listCourseGroups(
            CourseGroupStatus status,
            Long teacherId,
            Long subjectId,
            Pageable pageable) {

        log.info("Listando grupos con filtros - estado: {}, profesor: {}, asignatura: {}",
                status, teacherId, subjectId);

        Page<CourseGroup> groups;

        // Por ahora, usamos los métodos que ya existen sin paginación
        // En un proyecto real, deberíamos agregar métodos con paginación en el repositorio
        List<CourseGroup> groupList;

        if (status != null) {
            groupList = courseGroupRepository.findByStatus(status);
        } else if (teacherId != null) {
            groupList = courseGroupRepository.findByTeacherId(teacherId);
        } else if (subjectId != null) {
            groupList = courseGroupRepository.findBySubjectId(subjectId);
        } else {
            groups = courseGroupRepository.findAll(pageable);
            return groups.map(courseGroupMapper::toDto);
        }

        // Convertir lista a página manualmente
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), groupList.size());

        Page<CourseGroup> page = new org.springframework.data.domain.PageImpl<>(
                groupList.subList(start, end), pageable, groupList.size());

        return page.map(courseGroupMapper::toDto);
    }

    // ============================================
    // ESTADÍSTICAS Y REPORTES
    // ============================================

    /**
     * Obtiene estadísticas generales del sistema.
     *
     * @return estadísticas del sistema
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<SystemStatsDto> getSystemStatistics() {
        log.info("Obteniendo estadísticas del sistema");

        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalSubjects = subjectRepository.count();
        long totalGroups = courseGroupRepository.count();

        // Contar grupos activos usando stream ya que no tenemos el método countByStatus
        long activeGroups = courseGroupRepository.findByStatus(CourseGroupStatus.ACTIVE).size();
        long totalEnrollments = enrollmentRepository.count();
        long paidEnrollments = enrollmentRepository.findByPaymentStatus(
                com.acainfo.mvp.model.enums.PaymentStatus.PAID).size();
        long pendingRequests = groupRequestRepository.findByStatus(
                com.acainfo.mvp.model.enums.RequestStatus.PENDING).size();

        SystemStatsDto stats = SystemStatsDto.builder()
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalSubjects(totalSubjects)
                .totalGroups(totalGroups)
                .activeGroups(activeGroups)
                .totalEnrollments(totalEnrollments)
                .paidEnrollments(paidEnrollments)
                .pendingGroupRequests(pendingRequests)
                .build();

        return ApiResponseDto.success(stats, "Estadísticas obtenidas exitosamente");
    }

    /**
     * Obtiene las solicitudes de grupo pendientes de revisión.
     *
     * @return lista de solicitudes pendientes
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<List<GroupRequestDetailsDto>> getPendingGroupRequests() {
        log.info("Obteniendo solicitudes de grupo pendientes");

        List<GroupRequest> pendingRequests = groupRequestRepository.findByStatus(
                com.acainfo.mvp.model.enums.RequestStatus.PENDING);

        // Agrupar por asignatura y contar
        List<GroupRequestDetailsDto> details = pendingRequests.stream()
                .collect(Collectors.groupingBy(GroupRequest::getSubject))
                .entrySet().stream()
                .map(entry -> {
                    Subject subject = entry.getKey();
                    List<GroupRequest> requests = entry.getValue();

                    return GroupRequestDetailsDto.builder()
                            .subjectId(subject.getId())
                            .subjectName(subject.getName())
                            .subjectMajor(subject.getMajor())
                            .courseYear(subject.getCourseYear())
                            .requestCount(requests.size())
                            .oldestCreatedAt(requests.stream()
                                    .map(GroupRequest::getCreatedAt)
                                    .min(LocalDateTime::compareTo)
                                    .orElse(null))
                            .build();
                })
                .sorted((a, b)
                        -> Integer.compare(b.getRequestCount(), a.getRequestCount()))
                .collect(Collectors.toList());

        return ApiResponseDto.success(details,
                String.format("Se encontraron solicitudes para %d asignaturas", details.size()));
    }

    // ============================================
    // MÉTODOS AUXILIARES
    // ============================================

    /**
     * Valida las transiciones de estado permitidas para un grupo.
     *
     * @param currentStatus estado actual
     * @param newStatus nuevo estado
     * @throws ValidationException si la transición no es válida
     */
    private void validateStatusTransition(CourseGroupStatus currentStatus, CourseGroupStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new ValidationException("El grupo ya está en el estado " + currentStatus);
        }

        switch (currentStatus) {
            case PLANNED:
                if (newStatus != CourseGroupStatus.ACTIVE) {
                    throw new ValidationException("Un grupo PLANNED solo puede cambiar a ACTIVE");
                }
                break;
            case ACTIVE:
                if (newStatus != CourseGroupStatus.CLOSED) {
                    throw new ValidationException("Un grupo ACTIVE solo puede cambiar a CLOSED");
                }
                break;
            case CLOSED:
                throw new ValidationException("Un grupo CLOSED no puede cambiar de estado");
            default:
                throw new ValidationException("Estado no reconocido: " + currentStatus);
        }
    }

    /**
     * DTO para estadísticas del sistema
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SystemStatsDto {
        private long totalStudents;
        private long totalTeachers;
        private long totalSubjects;
        private long totalGroups;
        private long activeGroups;
        private long totalEnrollments;
        private long paidEnrollments;
        private long pendingGroupRequests;
    }

    /**
     * DTO para detalles de solicitudes de grupo
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GroupRequestDetailsDto {
        private Long subjectId;
        private String subjectName;
        private String subjectMajor;
        private Integer courseYear;
        private int requestCount;
        private LocalDateTime oldestCreatedAt;
    }
}
