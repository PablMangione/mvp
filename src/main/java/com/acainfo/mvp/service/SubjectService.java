package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.subject.*;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.SubjectMapper;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.RequestStatus;
import com.acainfo.mvp.repository.SubjectRepository;
import com.acainfo.mvp.util.SessionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de asignaturas.
 * Maneja consultas públicas y operaciones administrativas sobre asignaturas.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final SessionUtils sessionUtils;

    public SubjectService(SubjectRepository subjectRepository,
                          SubjectMapper subjectMapper,
                          SessionUtils sessionUtils) {
        this.subjectRepository = subjectRepository;
        this.subjectMapper = subjectMapper;
        this.sessionUtils = sessionUtils;
    }

    /**
     * Obtiene todas las asignaturas del sistema.
     * Accesible para todos los usuarios autenticados.
     */
    public List<SubjectDto> getAllSubjects() {
        log.debug("Obteniendo todas las asignaturas");

        List<Subject> subjects = subjectRepository.findAll();
        return subjectMapper.toDtoList(subjects);
    }

    /**
     * Obtiene una asignatura por su ID.
     * Accesible para todos los usuarios autenticados.
     */
    public SubjectDto getSubjectById(Long id) {
        log.debug("Obteniendo asignatura con ID: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignatura no encontrada con ID: " + id));

        return subjectMapper.toDto(subject);
    }

    /**
     * Obtiene asignaturas por carrera.
     * Usado principalmente por estudiantes para ver asignaturas de su carrera.
     */
    public List<SubjectDto> getSubjectsByMajor(String major) {
        log.debug("Obteniendo asignaturas de la carrera: {}", major);

        if (major == null || major.trim().isEmpty()) {
            throw new ValidationException("La carrera no puede estar vacía");
        }

        List<Subject> subjects = subjectRepository.findByMajor(major);
        return subjectMapper.toDtoList(subjects);
    }

    /**
     * Obtiene asignaturas por año de curso.
     * Permite filtrar asignaturas de un año específico.
     */
    public List<SubjectDto> getSubjectsByCourseYear(Integer courseYear) {
        log.debug("Obteniendo asignaturas del año: {}", courseYear);

        if (courseYear == null || courseYear < 1 || courseYear > 6) {
            throw new ValidationException("El año de curso debe estar entre 1 y 6");
        }

        List<Subject> subjects = subjectRepository.findByCourseYear(courseYear);
        return subjectMapper.toDtoList(subjects);
    }

    /**
     * Obtiene asignaturas con grupos activos.
     * Útil para mostrar solo asignaturas donde hay inscripción disponible.
     */
    public List<SubjectWithGroupsDto> getSubjectsWithActiveGroups() {
        log.debug("Obteniendo asignaturas con grupos activos");

        List<Subject> subjects = subjectRepository.findSubjectsWithActiveGroups();
        return subjectMapper.toWithGroupsDtoList(subjects);
    }

    /**
     * Obtiene asignaturas filtradas según criterios.
     * Permite búsqueda avanzada con múltiples filtros.
     */
    public List<SubjectDto> getSubjectsFiltered(SubjectFilterDto filter) {
        log.debug("Obteniendo asignaturas con filtros: {}", filter);

        List<Subject> allSubjects = subjectRepository.findAll();

        // Aplicar filtros
        List<Subject> filtered = allSubjects.stream()
                .filter(subject -> filter.getMajor() == null ||
                        subject.getMajor().equalsIgnoreCase(filter.getMajor()))
                .filter(subject -> filter.getCourseYear() == null ||
                        subject.getCourseYear().equals(filter.getCourseYear()))
                .filter(subject -> !Boolean.TRUE.equals(filter.getOnlyWithActiveGroups()) ||
                        subject.getCourseGroups().stream()
                                .anyMatch(group -> group.getStatus().name().equals("ACTIVE")))
                .collect(Collectors.toList());

        return subjectMapper.toDtoList(filtered);
    }

    /**
     * Obtiene una asignatura con información detallada de sus grupos.
     * Incluye información sobre grupos disponibles para inscripción.
     */
    public SubjectWithGroupsDto getSubjectWithGroups(Long id) {
        log.debug("Obteniendo asignatura con grupos, ID: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignatura no encontrada con ID: " + id));

        return subjectMapper.toWithGroupsDto(subject);
    }

    // ========== OPERACIONES DE ADMINISTRADOR ==========

    /**
     * Crea una nueva asignatura.
     * Solo accesible para administradores.
     */
    @Transactional
    public SubjectDto createSubject(CreateSubjectDto createDto) {
        validateAdminRole();
        log.info("Creando nueva asignatura: {}", createDto.getName());

        // Verificar si ya existe una asignatura con el mismo nombre y carrera
        boolean exists = subjectRepository.findAll().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(createDto.getName()) &&
                        s.getMajor().equalsIgnoreCase(createDto.getMajor()));

        if (exists) {
            throw new ValidationException(
                    "Ya existe una asignatura con ese nombre en la carrera especificada");
        }

        Subject subject = subjectMapper.toEntity(createDto);

        try {
            subject = subjectRepository.save(subject);
            log.info("Asignatura creada con ID: {}", subject.getId());
            return subjectMapper.toDto(subject);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al crear asignatura", e);
            throw new ValidationException(
                    "Error al crear la asignatura. Verifique que no exista una con el mismo nombre y carrera");
        }
    }

    /**
     * Actualiza una asignatura existente.
     * Solo accesible para administradores.
     */
    @Transactional
    public SubjectDto updateSubject(Long id, UpdateSubjectDto updateDto) {
        validateAdminRole();
        log.info("Actualizando asignatura ID: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignatura no encontrada con ID: " + id));

        // Verificar si el nuevo nombre/carrera ya existe (si se está cambiando)
        if (updateDto.getName() != null || updateDto.getMajor() != null) {
            String newName = updateDto.getName() != null ? updateDto.getName() : subject.getName();
            String newMajor = updateDto.getMajor() != null ? updateDto.getMajor() : subject.getMajor();

            boolean exists = subjectRepository.findAll().stream()
                    .anyMatch(s -> !s.getId().equals(id) &&
                            s.getName().equalsIgnoreCase(newName) &&
                            s.getMajor().equalsIgnoreCase(newMajor));

            if (exists) {
                throw new ValidationException(
                        "Ya existe otra asignatura con ese nombre en la carrera especificada");
            }
        }

        subjectMapper.updateFromDto(subject, updateDto);

        try {
            subject = subjectRepository.save(subject);
            log.info("Asignatura actualizada: {}", subject.getId());
            return subjectMapper.toDto(subject);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al actualizar asignatura", e);
            throw new ValidationException(
                    "Error al actualizar la asignatura. Verifique los datos ingresados");
        }
    }

    /**
     * Elimina una asignatura.
     * Solo accesible para administradores.
     * No se puede eliminar si tiene grupos o solicitudes asociadas.
     */
    @Transactional
    public void deleteSubject(Long id) {
        validateAdminRole();
        log.info("Eliminando asignatura ID: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignatura no encontrada con ID: " + id));

        // Verificar si tiene grupos asociados
        if (!subject.getCourseGroups().isEmpty()) {
            throw new ValidationException(
                    "No se puede eliminar la asignatura porque tiene grupos asociados");
        }

        // Verificar si tiene solicitudes de grupo
        if (!subject.getGroupRequests().isEmpty()) {
            throw new ValidationException(
                    "No se puede eliminar la asignatura porque tiene solicitudes de grupo pendientes");
        }

        try {
            subjectRepository.delete(subject);
            log.info("Asignatura eliminada: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error("Error al eliminar asignatura", e);
            throw new ValidationException(
                    "No se puede eliminar la asignatura debido a dependencias existentes");
        }
    }

    /**
     * Obtiene estadísticas de una asignatura.
     * Información útil para administradores.
     */
    public SubjectStatsDto getSubjectStats(Long id) {
        validateAdminRole();
        log.debug("Obteniendo estadísticas de asignatura ID: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignatura no encontrada con ID: " + id));

        int totalGroups = subject.getCourseGroups().size();
        long activeGroups = subject.getCourseGroups().stream()
                .filter(g -> "ACTIVE".equals(g.getStatus().name()))
                .count();
        long plannedGroups = subject.getCourseGroups().stream()
                .filter(g -> "PLANNED".equals(g.getStatus().name()))
                .count();
        long closedGroups = subject.getCourseGroups().stream()
                .filter(g -> "CLOSED".equals(g.getStatus().name()))
                .count();

        int totalEnrollments = subject.getCourseGroups().stream()
                .mapToInt(g -> g.getEnrollments().size())
                .sum();

        long pendingRequests = subject.getGroupRequests().stream()
                .filter(r -> "PENDING".equals(r.getStatus().name()))
                .count();

        return SubjectStatsDto.builder()
                .subjectId(id)
                .subjectName(subject.getName())
                .totalGroups(totalGroups)
                .activeGroups((int) activeGroups)
                .plannedGroups((int) plannedGroups)
                .closedGroups((int) closedGroups)
                .totalEnrollments(totalEnrollments)
                .pendingGroupRequests((int) pendingRequests)
                .build();
    }

    /**
     * Verifica si una asignatura puede ser eliminada.
     * No puede eliminarse si tiene grupos o solicitudes pendientes.
     */
    public boolean canDeleteSubject(Long subjectId) {
        validateAdminRole();
        log.debug("Verificando si se puede eliminar asignatura ID: {}", subjectId);

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignatura no encontrada con ID: " + subjectId));

        // Verificar si tiene grupos
        boolean hasGroups = !subject.getCourseGroups().isEmpty();

        // Verificar si tiene solicitudes pendientes
        boolean hasPendingRequests = subject.getGroupRequests().stream()
                .anyMatch(request -> RequestStatus.PENDING.equals(request.getStatus()));

        return !hasGroups && !hasPendingRequests;
    }

    /**
     * Valida que el usuario actual sea administrador.
     * Lanza excepción si no tiene permisos.
     */
    private void validateAdminRole() {
        if (!sessionUtils.isAdmin()) {
            log.warn("Intento de acceso a función de admin por usuario: {}",
                    sessionUtils.getCurrentUserEmail());
            throw new ValidationException("No tiene permisos para realizar esta operación");
        }
    }
}

/**
 * DTO para estadísticas de asignatura (usado internamente).
 */
@lombok.Data
@lombok.Builder
class SubjectStatsDto {
    private Long subjectId;
    private String subjectName;
    private int totalGroups;
    private int activeGroups;
    private int plannedGroups;
    private int closedGroups;
    private int totalEnrollments;
    private int pendingGroupRequests;
}