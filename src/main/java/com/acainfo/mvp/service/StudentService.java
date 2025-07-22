package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.auth.ChangePasswordDto;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.coursegroup.CourseGroupDto;
import com.acainfo.mvp.dto.student.CreateStudentDto;
import com.acainfo.mvp.dto.student.EnrollmentSummaryDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.dto.subject.SubjectDto;
import com.acainfo.mvp.exception.auth.EmailAlreadyExistsException;
import com.acainfo.mvp.exception.auth.InvalidCredentialsException;
import com.acainfo.mvp.exception.auth.PasswordMismatchException;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.CourseGroupMapper;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.mapper.SubjectMapper;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupRequest;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.RequestStatus;
import com.acainfo.mvp.repository.*;
import com.acainfo.mvp.util.SessionUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de estudiantes.
 * Maneja operaciones sobre el perfil del estudiante y consultas relacionadas.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final GroupRequestRepository groupRequestRepository;
    private final StudentMapper studentMapper;
    private final SubjectMapper subjectMapper;
    private final CourseGroupMapper courseGroupMapper;
    private final SessionUtils sessionUtils;
    private final EnrollmentService enrollmentService;

    public StudentService(StudentRepository studentRepository,
                          TeacherRepository teacherRepository,
                          SubjectRepository subjectRepository,
                          CourseGroupRepository courseGroupRepository,
                          GroupRequestRepository groupRequestRepository,
                          StudentMapper studentMapper,
                          SubjectMapper subjectMapper,
                          CourseGroupMapper courseGroupMapper,
                          SessionUtils sessionUtils,
                          EnrollmentService enrollmentService) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.courseGroupRepository = courseGroupRepository;
        this.groupRequestRepository = groupRequestRepository;
        this.studentMapper = studentMapper;
        this.subjectMapper = subjectMapper;
        this.courseGroupMapper = courseGroupMapper;
        this.sessionUtils = sessionUtils;
        this.enrollmentService = enrollmentService;
    }

    // ========== OPERACIONES DE PERFIL DE ESTUDIANTE ==========

    /**
     * Obtiene el perfil del estudiante actual.
     * Solo puede acceder a su propio perfil.
     */
    public StudentDto getMyProfile() {
        Long studentId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo perfil del estudiante ID: {}", studentId);

        if (!sessionUtils.isStudent()) {
            throw new ValidationException("Esta operación es solo para estudiantes");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        return studentMapper.toDto(student);
    }

    /**
     * Obtiene un estudiante por ID.
     * Estudiantes solo pueden ver su propio perfil.
     * Administradores pueden ver cualquier perfil.
     */
    public StudentDto getStudentById(Long studentId) {
        log.debug("Obteniendo estudiante con ID: {}", studentId);

        // Validar acceso
        if (sessionUtils.isStudent()) {
            validateStudentAccess(studentId);
        } else if (!sessionUtils.isAdmin()) {
            throw new ValidationException("No tiene permisos para ver este perfil");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        return studentMapper.toDto(student);
    }

    /**
     * Actualiza el perfil del estudiante.
     * Solo puede actualizar nombre y carrera (no email).
     */
    @Transactional
    public StudentDto updateProfile(Long studentId, StudentDto updateDto) {
        validateStudentAccess(studentId);
        log.info("Actualizando perfil del estudiante ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        // Actualizar solo campos permitidos
        studentMapper.updateBasicInfo(student, updateDto);

        try {
            student = studentRepository.save(student);
            log.info("Perfil actualizado exitosamente para estudiante: {}", student.getEmail());
            return studentMapper.toDto(student);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al actualizar perfil", e);
            throw new ValidationException("Error al actualizar el perfil");
        }
    }

    /**
     * Cambia la contraseña del estudiante.
     * Requiere la contraseña actual para validación.
     */
    @Transactional
    public ApiResponseDto<Void> changePassword(Long studentId, ChangePasswordDto changePasswordDto) {
        validateStudentAccess(studentId);
        log.info("Cambio de contraseña solicitado para estudiante ID: {}", studentId);

        // Validar que las contraseñas nuevas coincidan
        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
            throw new PasswordMismatchException("Las contraseñas nuevas no coinciden");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        // Verificar contraseña actual
        if (!studentMapper.passwordMatches(changePasswordDto.getCurrentPassword(),
                student.getPassword())) {
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        // Actualizar contraseña
        student.setPassword(studentMapper.encodePassword(changePasswordDto.getNewPassword()));

        try {
            studentRepository.save(student);
            log.info("Contraseña actualizada exitosamente para estudiante: {}", student.getEmail());
            return ApiResponseDto.success(null, "Contraseña actualizada exitosamente");
        } catch (Exception e) {
            log.error("Error al cambiar contraseña", e);
            throw new ValidationException("Error al cambiar la contraseña");
        }
    }

    // ========== CONSULTAS ACADÉMICAS ==========

    /**
     * Obtiene los grupos disponibles de una asignatura.
     * Solo devuelve grupos ACTIVE o PLANNED.
     * Verifica que la asignatura sea de la carrera del estudiante.
     *
     * @param subjectId ID de la asignatura
     * @return Lista de grupos disponibles
     */
    public List<CourseGroupDto> getSubjectGroups(Long subjectId) throws AccessDeniedException {
        // Obtener el estudiante actual
        Long studentId = sessionUtils.getCurrentUserId();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        // Obtener la asignatura
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Asignatura no encontrada"));

        // Verificar que la asignatura sea de la carrera del estudiante
        if (!subject.getMajor().equals(student.getMajor())) {
            throw new AccessDeniedException("La asignatura no pertenece a tu carrera");
        }

        // Obtener grupos activos o planificados
        List<CourseGroup> groups = courseGroupRepository
                .findBySubjectIdAndStatusIn(subjectId,
                        Arrays.asList(CourseGroupStatus.ACTIVE, CourseGroupStatus.PLANNED));

        // Convertir a DTOs
        return courseGroupMapper.toDtoList(groups);
    }

    /**
     * Obtiene las asignaturas de la carrera del estudiante.
     * Filtradas por la carrera que cursa el estudiante.
     */
    public List<SubjectDto> getMyMajorSubjects() {
        Long studentId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo asignaturas de la carrera del estudiante ID: {}", studentId);

        if (!sessionUtils.isStudent()) {
            throw new ValidationException("Esta operación es solo para estudiantes");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        List<Subject> subjects = subjectRepository.findByMajor(student.getMajor());
        subjects.forEach(subject -> {
            log.debug("****************************************************** {}", subject.getId());
        });
        return subjectMapper.toDtoList(subjects);
    }

    /**
     * Obtiene las asignaturas de un año específico de la carrera del estudiante.
     */
    public List<SubjectDto> getMyMajorSubjectsByYear(Integer courseYear) {
        Long studentId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo asignaturas del año {} para estudiante ID: {}", courseYear, studentId);

        if (!sessionUtils.isStudent()) {
            throw new ValidationException("Esta operación es solo para estudiantes");
        }

        if (courseYear == null || courseYear < 1 || courseYear > 6) {
            throw new ValidationException("El año de curso debe estar entre 1 y 6");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        // Filtrar por carrera y año
        List<Subject> subjects = subjectRepository.findByMajor(student.getMajor()).stream()
                .filter(subject -> subject.getCourseYear().equals(courseYear))
                .collect(Collectors.toList());

        return subjectMapper.toDtoList(subjects);
    }

    /**
     * Obtiene las inscripciones actuales del estudiante.
     * Incluye información detallada de cada inscripción.
     */
    public List<EnrollmentSummaryDto> getMyEnrollments() {
        Long studentId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo inscripciones del estudiante ID: {}", studentId);

        if (!sessionUtils.isStudent()) {
            throw new ValidationException("Esta operación es solo para estudiantes");
        }

        return enrollmentService.getStudentEnrollments(studentId);
    }

    /**
     * Obtiene estadísticas del estudiante.
     * Incluye número de inscripciones, pagos pendientes, etc.
     */
    public StudentStatsDto getMyStats() {
        Long studentId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo estadísticas del estudiante ID: {}", studentId);

        if (!sessionUtils.isStudent()) {
            throw new ValidationException("Esta operación es solo para estudiantes");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        // Obtener estadísticas de inscripciones
        EnrollmentStatsDto enrollmentStats = enrollmentService.getStudentEnrollmentStats(studentId);

        // Calcular asignaturas disponibles
        long totalSubjectsInMajor = subjectRepository.findByMajor(student.getMajor()).size();
        long enrolledSubjects = student.getEnrollments().stream()
                .map(e -> e.getCourseGroup().getSubject())
                .distinct()
                .count();

        return StudentStatsDto.builder()
                .studentId(studentId)
                .studentName(student.getName())
                .major(student.getMajor())
                .totalEnrollments(enrollmentStats.getTotalEnrollments())
                .activeEnrollments(enrollmentStats.getActiveEnrollments())
                .pendingPayments(enrollmentStats.getPendingPayments())
                .totalSubjectsInMajor((int) totalSubjectsInMajor)
                .enrolledSubjects((int) enrolledSubjects)
                .remainingSubjects((int) (totalSubjectsInMajor - enrolledSubjects))
                .build();
    }

    // ========== OPERACIONES ADMINISTRATIVAS ==========

    /**
     * Obtiene todos los estudiantes con paginación.
     * Solo accesible para administradores.
     */
    public Page<StudentDto> getAllStudents(Pageable pageable) {
        validateAdminRole();
        log.debug("Obteniendo todos los estudiantes - página: {}", pageable.getPageNumber());

        Page<Student> students = studentRepository.findAll(pageable);
        return students.map(studentMapper::toDto);
    }

    /**
     * Crea un nuevo estudiante.
     * Solo accesible para administradores.
     */
    @Transactional
    public StudentDto createStudent(CreateStudentDto createDto) {
        validateAdminRole();
        log.info("Creando nuevo estudiante: {}", createDto.getEmail());

        // Verificar si el email ya existe
        if (studentRepository.existsByEmail(createDto.getEmail()) ||
                teacherRepository.existsByEmail(createDto.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }

        Student student = studentMapper.toEntity(createDto);

        try {
            student = studentRepository.save(student);
            log.info("Estudiante creado con ID: {}", student.getId());
            return studentMapper.toDto(student);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al crear estudiante", e);
            throw new ValidationException("Error al crear el estudiante. Verifique los datos");
        }
    }

    /**
     * Actualiza un estudiante como administrador.
     * Permite actualizar más campos que el propio estudiante.
     */
    @Transactional
    public StudentDto updateStudent(Long studentId, StudentDto updateDto) {
        validateAdminRole();
        log.info("Admin actualizando estudiante ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        // Admin puede actualizar más campos
        studentMapper.updateBasicInfo(student, updateDto);

        try {
            student = studentRepository.save(student);
            log.info("Estudiante actualizado por admin: {}", student.getId());
            return studentMapper.toDto(student);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al actualizar estudiante", e);
            throw new ValidationException("Error al actualizar el estudiante");
        }
    }

    /**
     * Elimina un estudiante.
     * Solo posible si no tiene inscripciones activas.
     */
    @Transactional
    public void deleteStudent(Long studentId) {
        validateAdminRole();
        log.info("Eliminando estudiante ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        // Verificar si tiene inscripciones activas
        long activeEnrollments = student.getEnrollments().stream()
                .filter(e -> "ACTIVE".equals(e.getCourseGroup().getStatus().name()))
                .count();

        if (activeEnrollments > 0) {
            throw new ValidationException(
                    "No se puede eliminar el estudiante porque tiene inscripciones activas");
        }

        try {
            studentRepository.delete(student);
            log.info("Estudiante eliminado: {}", studentId);
        } catch (DataIntegrityViolationException e) {
            log.error("Error al eliminar estudiante", e);
            throw new ValidationException(
                    "No se puede eliminar el estudiante debido a dependencias existentes");
        }
    }

    /**
     * Busca estudiantes por carrera.
     * Útil para reportes administrativos.
     */
    public List<StudentDto> getStudentsByMajor(String major) {
        validateAdminRole();
        log.debug("Buscando estudiantes de la carrera: {}", major);

        if (major == null || major.trim().isEmpty()) {
            throw new ValidationException("La carrera no puede estar vacía");
        }

        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> s.getMajor().equalsIgnoreCase(major))
                .collect(Collectors.toList());

        return students.stream()
                .map(studentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas generales de estudiantes.
     * Para dashboard administrativo.
     */
    public AdminStudentStatsDto getAdminStudentStats() {
        validateAdminRole();
        log.debug("Obteniendo estadísticas administrativas de estudiantes");

        List<Student> allStudents = studentRepository.findAll();

        // Agrupar por carrera
        var studentsByMajor = allStudents.stream()
                .collect(Collectors.groupingBy(Student::getMajor, Collectors.counting()));

        // Contar inscripciones activas
        long totalActiveEnrollments = allStudents.stream()
                .flatMap(s -> s.getEnrollments().stream())
                .filter(e -> "ACTIVE".equals(e.getCourseGroup().getStatus().name()))
                .count();

        // Contar pagos pendientes
        long totalPendingPayments = allStudents.stream()
                .flatMap(s -> s.getEnrollments().stream())
                .filter(e -> "PENDING".equals(e.getPaymentStatus().name()))
                .count();

        return AdminStudentStatsDto.builder()
                .totalStudents(allStudents.size())
                .studentsByMajor(studentsByMajor)
                .totalActiveEnrollments(totalActiveEnrollments)
                .totalPendingPayments(totalPendingPayments)
                .build();
    }

    // ========== MÉTODOS DE VALIDACIÓN PRIVADOS ==========

    /**
     * Valida acceso de estudiante a sus propios recursos.
     */
    private void validateStudentAccess(Long studentId) {
        if (!sessionUtils.isStudent()) {
            return; // Puede ser admin
        }

        if (!studentId.equals(sessionUtils.getCurrentUserId())) {
            log.warn("Estudiante {} intentó acceder a datos del estudiante {}",
                    sessionUtils.getCurrentUserId(), studentId);
            throw new ValidationException("No puede acceder a información de otros estudiantes");
        }
    }

    /**
     * Valida que el usuario actual sea administrador.
     */
    private void validateAdminRole() {
        if (!sessionUtils.isAdmin()) {
            log.warn("Intento de acceso a función de admin por usuario: {}",
                    sessionUtils.getCurrentUserEmail());
            throw new ValidationException("No tiene permisos para realizar esta operación");
        }
    }

    /**
     * Verifica si un email ya está registrado.
     * Solo accesible para administradores.
     */
    public boolean emailExists(String email) {
        validateAdminRole();
        log.debug("Verificando si existe el email: {}", email);
        return studentRepository.existsByEmail(email);
    }


}

/**
 * DTO para estadísticas del estudiante.
 */
@lombok.Data
@lombok.Builder
class StudentStatsDto {
    private Long studentId;
    private String studentName;
    private String major;
    private int totalEnrollments;
    private int activeEnrollments;
    private int pendingPayments;
    private int totalSubjectsInMajor;
    private int enrolledSubjects;
    private int remainingSubjects;
}

/**
 * DTO para estadísticas administrativas.
 */
@lombok.Data
@lombok.Builder
class AdminStudentStatsDto {
    private int totalStudents;
    private java.util.Map<String, Long> studentsByMajor;
    private long totalActiveEnrollments;
    private long totalPendingPayments;
}