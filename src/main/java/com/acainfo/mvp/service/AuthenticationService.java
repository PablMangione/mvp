package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.auth.*;
import com.acainfo.mvp.dto.student.StudentRegistrationDto;
import com.acainfo.mvp.exception.auth.EmailAlreadyExistsException;
import com.acainfo.mvp.exception.auth.InvalidCredentialsException;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.mapper.TeacherMapper;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.TeacherRepository;
import com.acainfo.mvp.security.CustomUserDetails;
import com.acainfo.mvp.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio de autenticación y gestión de sesiones.
 * Maneja login, logout y registro de usuarios.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final SessionUtils sessionUtils;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 StudentRepository studentRepository,
                                 TeacherRepository teacherRepository,
                                 StudentMapper studentMapper,
                                 TeacherMapper teacherMapper,
                                 SessionUtils sessionUtils) {
        this.authenticationManager = authenticationManager;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.sessionUtils = sessionUtils;
    }

    /**
     * Realiza el login de un usuario (estudiante o profesor).
     * Crea una sesión HTTP tras autenticación exitosa.
     */
    public LoginResponseDto login(LoginRequestDto loginDto, HttpSession session) {
        log.info("Intento de login para: {}", loginDto.getEmail());

        try {
            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getEmail(),
                            loginDto.getPassword()
                    )
            );

            // Establecer autenticación en el contexto
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Obtener detalles del usuario autenticado
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Configurar atributos de sesión
            sessionUtils.setupSessionAttributes(session, userDetails);

            // Buscar el usuario completo para el response
            LoginResponseDto response = createLoginResponse(userDetails);

            log.info("Login exitoso para: {} con rol: {}", userDetails.getEmail(), userDetails.getRole());
            return response;

        } catch (AuthenticationException e) {
            log.error("Error de autenticación para: {}", loginDto.getEmail());
            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }
    }

    /**
     * Registra un nuevo estudiante en el sistema.
     * Solo los estudiantes pueden auto-registrarse.
     */
    @Transactional
    public LoginResponseDto registerStudent(StudentRegistrationDto registrationDto, HttpSession session) {
        log.info("Registro de nuevo estudiante: {}", registrationDto.getEmail());

        // Verificar si el email ya existe
        if (studentRepository.existsByEmail(registrationDto.getEmail()) ||
                teacherRepository.existsByEmail(registrationDto.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }

        // Crear nuevo estudiante
        Student student = studentMapper.toEntity(registrationDto);
        student = studentRepository.save(student);

        log.info("Estudiante registrado con ID: {}", student.getId());

        // Auto-login después del registro
        LoginRequestDto loginDto = LoginRequestDto.builder()
                .email(registrationDto.getEmail())
                .password(registrationDto.getPassword())
                .build();

        return login(loginDto, session);
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    public LogoutResponseDto logout(HttpSession session) {
        String userEmail = sessionUtils.getCurrentUserEmail();
        log.info("Logout solicitado para: {}", userEmail);

        // Invalidar sesión y limpiar contexto de seguridad
        sessionUtils.invalidateSession(session);

        log.info("Logout exitoso para: {}", userEmail);
        return LogoutResponseDto.builder()
                .success(true)
                .message("Sesión cerrada correctamente")
                .build();
    }

    /**
     * Obtiene información del usuario actual.
     */
    public CurrentUserDto getCurrentUser() {
        if (!sessionUtils.isAuthenticated()) {
            return CurrentUserDto.builder()
                    .authenticated(false)
                    .build();
        }

        CustomUserDetails userDetails = sessionUtils.getCurrentUserDetails();
        return CurrentUserDto.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .name(userDetails.getName())
                .role(userDetails.getRole())
                .authenticated(true)
                .build();
    }

    /**
     * Crea el response de login basado en el tipo de usuario.
     */
    private LoginResponseDto createLoginResponse(CustomUserDetails userDetails) {
        return LoginResponseDto.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .name(userDetails.getName())
                .role(userDetails.getRole())
                .authenticated(true)
                .build();
    }

    /**
     * Verifica si la sesión actual es válida.
     */
    public boolean isSessionValid(HttpSession session) {
        try {
            return session != null &&
                    !session.isNew() &&
                    sessionUtils.isAuthenticated();
        } catch (IllegalStateException e) {
            // La sesión ya fue invalidada
            return false;
        }
    }

    /**
     * Obtiene información de la sesión actual.
     */
    public SessionInfoDto getSessionInfo(HttpSession session) {
        if (!isSessionValid(session)) {
            return null;
        }

        return SessionInfoDto.builder()
                .sessionId(session.getId())
                .createdAt(java.time.Instant.ofEpochMilli(session.getCreationTime())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime())
                .lastAccessedAt(java.time.Instant.ofEpochMilli(session.getLastAccessedTime())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime())
                .maxInactiveInterval(session.getMaxInactiveInterval())
                .build();
    }
}
