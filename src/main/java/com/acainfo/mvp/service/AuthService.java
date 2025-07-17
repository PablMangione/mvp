package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.auth.*;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.student.StudentRegistrationDto;
import com.acainfo.mvp.exception.auth.AuthenticationException;
import com.acainfo.mvp.exception.auth.EmailAlreadyExistsException;
import com.acainfo.mvp.exception.auth.InvalidCredentialsException;
import com.acainfo.mvp.exception.auth.PasswordMismatchException;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de autenticación y gestión de usuarios.
 * Maneja registro, login y cambio de contraseña para estudiantes y profesores.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo estudiante en el sistema.
     *
     * @param registrationDto datos de registro del estudiante
     * @return respuesta con los datos del estudiante registrado
     * @throws EmailAlreadyExistsException si el email ya está registrado
     */
    public ApiResponseDto<LoginResponseDto> registerStudent(StudentRegistrationDto registrationDto) {
        log.info("Registrando nuevo estudiante con email: {}", registrationDto.getEmail());

        // Verificar si el email ya existe
        if (studentRepository.existsByEmail(registrationDto.getEmail()) ||
                teacherRepository.existsByEmail(registrationDto.getEmail())) {
            log.warn("Intento de registro con email duplicado: {}", registrationDto.getEmail());
            throw new EmailAlreadyExistsException("El email ya está registrado en el sistema");
        }

        // Crear nuevo estudiante
        Student student = Student.builder()
                .name(registrationDto.getName())
                .email(registrationDto.getEmail())
                .password(passwordEncoder.encode(registrationDto.getPassword()))
                .major(registrationDto.getMajor())
                .build();

        // Guardar en base de datos
        student = studentRepository.save(student);
        log.info("Estudiante registrado exitosamente con ID: {}", student.getId());

        // Construir respuesta
        LoginResponseDto response = LoginResponseDto.builder()
                .id(student.getId())
                .email(student.getEmail())
                .name(student.getName())
                .role("STUDENT")
                .token(generateToken(student.getId(), "STUDENT")) // TODO: Implementar JWT
                .build();

        return ApiResponseDto.success(response, "Estudiante registrado exitosamente");
    }

    /**
     * Autentica un estudiante en el sistema.
     *
     * @param loginDto credenciales de login
     * @return respuesta con los datos del estudiante autenticado
     * @throws InvalidCredentialsException si las credenciales son inválidasacainfodb
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<LoginResponseDto> loginStudent(LoginRequestDto loginDto) {
        log.info("Intento de login de estudiante: {}", loginDto.getEmail());

        // Buscar estudiante por email
        Student student = studentRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login fallido - estudiante no encontrado: {}", loginDto.getEmail());
                    return new InvalidCredentialsException("Credenciales inválidas");
                });

        // Verificar contraseña
        if (!passwordEncoder.matches(loginDto.getPassword(), student.getPassword())) {
            log.warn("Login fallido - contraseña incorrecta para: {}", loginDto.getEmail());
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        log.info("Login exitoso para estudiante ID: {}", student.getId());

        // Construir respuesta
        LoginResponseDto response = LoginResponseDto.builder()
                .id(student.getId())
                .email(student.getEmail())
                .name(student.getName())
                .role("STUDENT")
                .token(generateToken(student.getId(), "STUDENT")) // TODO: Implementar JWT
                .build();

        return ApiResponseDto.success(response, "Login exitoso");
    }

    /**
     * Autentica un profesor en el sistema.
     *
     * @param loginDto credenciales de login
     * @return respuesta con los datos del profesor autenticado
     * @throws InvalidCredentialsException si las credenciales son inválidas
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<LoginResponseDto> loginTeacher(LoginRequestDto loginDto) {
        log.info("Intento de login de profesor: {}", loginDto.getEmail());

        // Buscar profesor por email
        Teacher teacher = teacherRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login fallido - profesor no encontrado: {}", loginDto.getEmail());
                    return new InvalidCredentialsException("Credenciales inválidas");
                });

        // Verificar contraseña
        if (!passwordEncoder.matches(loginDto.getPassword(), teacher.getPassword())) {
            log.warn("Login fallido - contraseña incorrecta para: {}", loginDto.getEmail());
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        log.info("Login exitoso para profesor ID: {}", teacher.getId());

        // Construir respuesta
        LoginResponseDto response = LoginResponseDto.builder()
                .id(teacher.getId())
                .email(teacher.getEmail())
                .name(teacher.getName())
                .role("TEACHER")
                .token(generateToken(teacher.getId(), "TEACHER")) // TODO: Implementar JWT
                .build();

        return ApiResponseDto.success(response, "Login exitoso");
    }

    /**
     * Cambia la contraseña de un estudiante.
     *
     * @param studentId ID del estudiante
     * @param changePasswordDto datos para el cambio de contraseña
     * @return respuesta indicando éxito
     * @throws AuthenticationException si el estudiante no existe
     * @throws InvalidCredentialsException si la contraseña actual es incorrecta
     * @throws PasswordMismatchException si las contraseñas nuevas no coinciden
     */
    public ApiResponseDto<Void> changeStudentPassword(Long studentId, ChangePasswordDto changePasswordDto) {
        log.info("Cambio de contraseña solicitado para estudiante ID: {}", studentId);

        // Validar que las contraseñas nuevas coincidan
        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
            log.warn("Cambio de contraseña fallido - las contraseñas no coinciden");
            throw new PasswordMismatchException("Las contraseñas nuevas no coinciden");
        }

        // Buscar estudiante
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> {
                    log.error("Estudiante no encontrado para cambio de contraseña: {}", studentId);
                    return new AuthenticationException("Usuario no encontrado");
                });

        // Verificar contraseña actual
        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), student.getPassword())) {
            log.warn("Cambio de contraseña fallido - contraseña actual incorrecta para estudiante: {}", studentId);
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        // Actualizar contraseña
        student.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.save(student);

        log.info("Contraseña cambiada exitosamente para estudiante ID: {}", studentId);
        return ApiResponseDto.success(null, "Contraseña actualizada exitosamente");
    }

    /**
     * Cambia la contraseña de un profesor.
     *
     * @param teacherId ID del profesor
     * @param changePasswordDto datos para el cambio de contraseña
     * @return respuesta indicando éxito
     * @throws AuthenticationException si el profesor no existe
     * @throws InvalidCredentialsException si la contraseña actual es incorrecta
     * @throws PasswordMismatchException si las contraseñas nuevas no coinciden
     */
    public ApiResponseDto<Void> changeTeacherPassword(Long teacherId, ChangePasswordDto changePasswordDto) {
        log.info("Cambio de contraseña solicitado para profesor ID: {}", teacherId);

        // Validar que las contraseñas nuevas coincidan
        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
            log.warn("Cambio de contraseña fallido - las contraseñas no coinciden");
            throw new PasswordMismatchException("Las contraseñas nuevas no coinciden");
        }

        // Buscar profesor
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> {
                    log.error("Profesor no encontrado para cambio de contraseña: {}", teacherId);
                    return new AuthenticationException("Usuario no encontrado");
                });

        // Verificar contraseña actual
        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), teacher.getPassword())) {
            log.warn("Cambio de contraseña fallido - contraseña actual incorrecta para profesor: {}", teacherId);
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        // Actualizar contraseña
        teacher.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        teacher.setUpdatedAt(LocalDateTime.now());
        teacherRepository.save(teacher);

        log.info("Contraseña cambiada exitosamente para profesor ID: {}", teacherId);
        return ApiResponseDto.success(null, "Contraseña actualizada exitosamente");
    }

    /**
     * Verifica si un email está disponible para registro.
     *
     * @param email email a verificar
     * @return true si está disponible, false si ya existe
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        boolean exists = studentRepository.existsByEmail(email) ||
                teacherRepository.existsByEmail(email);
        log.debug("Verificación de email {}: {}", email, exists ? "ocupado" : "disponible");
        return !exists;
    }

    /**
     * Genera un token temporal para el usuario.
     * TODO: Implementar generación real de JWT
     *
     * @param userId ID del usuario
     * @param role rol del usuario
     * @return token generado
     */
    private String generateToken(Long userId, String role) {
        // Implementación temporal - reemplazar con JWT real
        return String.format("temp-token-%s-%d-%d", role, userId, System.currentTimeMillis());
    }

    /**
     * Valida si un estudiante existe por ID.
     *
     * @param studentId ID del estudiante
     * @return true si existe
     */
    @Transactional(readOnly = true)
    public boolean studentExists(Long studentId) {
        return studentRepository.existsById(studentId);
    }

    /**
     * Valida si un profesor existe por ID.
     *
     * @param teacherId ID del profesor
     * @return true si existe
     */
    @Transactional(readOnly = true)
    public boolean teacherExists(Long teacherId) {
        return teacherRepository.existsById(teacherId);
    }
}
