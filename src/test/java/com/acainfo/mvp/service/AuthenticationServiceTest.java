package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.auth.*;
import com.acainfo.mvp.dto.student.StudentRegistrationDto;
import com.acainfo.mvp.exception.auth.EmailAlreadyExistsException;
import com.acainfo.mvp.exception.auth.InvalidCredentialsException;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.mapper.TeacherMapper;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.TeacherRepository;
import com.acainfo.mvp.security.CustomUserDetails;
import com.acainfo.mvp.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthenticationService.
 * Verifica login, registro, logout y gestión de sesiones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private SessionUtils sessionUtils;
    @Mock
    private HttpSession httpSession;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    private AuthenticationService authenticationService;

    private LoginRequestDto validLoginDto;
    private StudentRegistrationDto validRegistrationDto;
    private Student testStudent;
    private Teacher testTeacher;
    private CustomUserDetails studentUserDetails;
    private CustomUserDetails teacherUserDetails;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                authenticationManager,
                studentRepository,
                teacherRepository,
                studentMapper,
                teacherMapper,
                sessionUtils
        );

        // Configurar DTOs de prueba
        validLoginDto = LoginRequestDto.builder()
                .email("student@test.com")
                .password("password123")
                .build();

        validRegistrationDto = StudentRegistrationDto.builder()
                .name("Test Student")
                .email("newstudent@test.com")
                .password("password123")
                .major("Ingeniería Informática")
                .build();

        // Configurar entidades de prueba
        testStudent = Student.builder()
                .name("Test Student")
                .email("student@test.com")
                .password("$2a$10$encoded_password")
                .major("Ingeniería Informática")
                .build();

        testTeacher = Teacher.builder()
                .name("Test Teacher")
                .email("teacher@test.com")
                .password("$2a$10$encoded_password")
                .build();

        // Configurar UserDetails
        studentUserDetails = new CustomUserDetails(
                testStudent.getId(),
                testStudent.getEmail(),
                testStudent.getPassword(),
                testStudent.getName(),
                "STUDENT"
        );

        teacherUserDetails = new CustomUserDetails(
                testTeacher.getId(),
                testTeacher.getEmail(),
                testTeacher.getPassword(),
                testTeacher.getName(),
                "TEACHER"
        );

        // Configurar SecurityContext mock
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Debe realizar login exitoso de estudiante")
    void shouldLoginStudentSuccessfully() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(studentUserDetails);

        // When
        LoginResponseDto response = authenticationService.login(validLoginDto, httpSession);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testStudent.getId());
        assertThat(response.getEmail()).isEqualTo(testStudent.getEmail());
        assertThat(response.getName()).isEqualTo(testStudent.getName());
        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(response.isAuthenticated()).isTrue();

        // Verificar interacciones
        verify(securityContext).setAuthentication(authentication);
        verify(sessionUtils).setupSessionAttributes(httpSession, studentUserDetails);
    }

    @Test
    @DisplayName("Debe realizar login exitoso de profesor")
    void shouldLoginTeacherSuccessfully() {
        // Given
        LoginRequestDto teacherLoginDto = LoginRequestDto.builder()
                .email("teacher@test.com")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(teacherUserDetails);

        // When
        LoginResponseDto response = authenticationService.login(teacherLoginDto, httpSession);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testTeacher.getId());
        assertThat(response.getEmail()).isEqualTo(testTeacher.getEmail());
        assertThat(response.getName()).isEqualTo(testTeacher.getName());
        assertThat(response.getRole()).isEqualTo("TEACHER");
        assertThat(response.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Debe fallar login con credenciales incorrectas")
    void shouldFailLoginWithInvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When/Then
        assertThatThrownBy(() -> authenticationService.login(validLoginDto, httpSession))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email o contraseña incorrectos");

        // Verificar que no se configuró la sesión
        verify(sessionUtils, never()).setupSessionAttributes(any(), any());
        verify(securityContext, never()).setAuthentication(any());
    }

    @Test
    @DisplayName("Debe registrar nuevo estudiante exitosamente")
    void shouldRegisterStudentSuccessfully() {
        // Given
        when(studentRepository.existsByEmail(validRegistrationDto.getEmail())).thenReturn(false);
        when(teacherRepository.existsByEmail(validRegistrationDto.getEmail())).thenReturn(false);
        when(studentMapper.toEntity(validRegistrationDto)).thenReturn(testStudent);
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        // Configurar el login automático después del registro
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(studentUserDetails);

        // When
        LoginResponseDto response = authenticationService.registerStudent(validRegistrationDto, httpSession);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getRole()).isEqualTo("STUDENT");

        // Verificar guardado
        verify(studentRepository).save(any(Student.class));

        // Verificar login automático
        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());

        UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();
        assertThat(capturedAuth.getPrincipal()).isEqualTo(validRegistrationDto.getEmail());
        assertThat(capturedAuth.getCredentials()).isEqualTo(validRegistrationDto.getPassword());
    }

    @Test
    @DisplayName("Debe fallar registro con email ya existente en estudiantes")
    void shouldFailRegistrationWithExistingStudentEmail() {
        // Given
        when(studentRepository.existsByEmail(validRegistrationDto.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() ->
                authenticationService.registerStudent(validRegistrationDto, httpSession))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("El email ya está registrado");

        // Verificar que no se intentó guardar
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar registro con email ya existente en profesores")
    void shouldFailRegistrationWithExistingTeacherEmail() {
        // Given
        when(studentRepository.existsByEmail(validRegistrationDto.getEmail())).thenReturn(false);
        when(teacherRepository.existsByEmail(validRegistrationDto.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() ->
                authenticationService.registerStudent(validRegistrationDto, httpSession))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("El email ya está registrado");

        // Verificar que no se intentó guardar
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe realizar logout exitosamente")
    void shouldLogoutSuccessfully() {
        // Given
        when(sessionUtils.getCurrentUserEmail()).thenReturn("test@test.com");

        // When
        LogoutResponseDto response = authenticationService.logout(httpSession);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Sesión cerrada correctamente");

        // Verificar invalidación de sesión
        verify(sessionUtils).invalidateSession(httpSession);
    }

    @Test
    @DisplayName("Debe obtener usuario actual autenticado")
    void shouldGetCurrentAuthenticatedUser() {
        // Given
        when(sessionUtils.isAuthenticated()).thenReturn(true);
        when(sessionUtils.getCurrentUserDetails()).thenReturn(studentUserDetails);

        // When
        CurrentUserDto currentUser = authenticationService.getCurrentUser();

        // Then
        assertThat(currentUser).isNotNull();
        assertThat(currentUser.isAuthenticated()).isTrue();
        assertThat(currentUser.getId()).isEqualTo(studentUserDetails.getId());
        assertThat(currentUser.getEmail()).isEqualTo(studentUserDetails.getEmail());
        assertThat(currentUser.getName()).isEqualTo(studentUserDetails.getName());
        assertThat(currentUser.getRole()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("Debe retornar usuario no autenticado cuando no hay sesión")
    void shouldReturnUnauthenticatedUserWhenNoSession() {
        // Given
        when(sessionUtils.isAuthenticated()).thenReturn(false);

        // When
        CurrentUserDto currentUser = authenticationService.getCurrentUser();

        // Then
        assertThat(currentUser).isNotNull();
        assertThat(currentUser.isAuthenticated()).isFalse();
        assertThat(currentUser.getId()).isNull();
        assertThat(currentUser.getEmail()).isNull();
    }

    @Test
    @DisplayName("Debe validar sesión válida correctamente")
    void shouldValidateValidSession() {
        // Given
        when(httpSession.isNew()).thenReturn(false);
        when(sessionUtils.isAuthenticated()).thenReturn(true);

        // When
        boolean isValid = authenticationService.isSessionValid(httpSession);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Debe detectar sesión inválida - sesión nueva")
    void shouldDetectInvalidSession_NewSession() {
        // Given
        when(httpSession.isNew()).thenReturn(true);

        // When
        boolean isValid = authenticationService.isSessionValid(httpSession);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Debe detectar sesión inválida - usuario no autenticado")
    void shouldDetectInvalidSession_NotAuthenticated() {
        // Given
        when(httpSession.isNew()).thenReturn(false);
        when(sessionUtils.isAuthenticated()).thenReturn(false);

        // When
        boolean isValid = authenticationService.isSessionValid(httpSession);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Debe detectar sesión inválida - sesión null")
    void shouldDetectInvalidSession_NullSession() {
        // When
        boolean isValid = authenticationService.isSessionValid(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Debe manejar IllegalStateException en validación de sesión")
    void shouldHandleIllegalStateExceptionInSessionValidation() {
        // Given
        when(httpSession.isNew()).thenThrow(new IllegalStateException("Session already invalidated"));

        // When
        boolean isValid = authenticationService.isSessionValid(httpSession);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Debe obtener información de sesión válida")
    void shouldGetSessionInfo() {
        // Given
        long creationTime = System.currentTimeMillis() - 10000; // 10 segundos atrás
        long lastAccessedTime = System.currentTimeMillis() - 1000; // 1 segundo atrás
        int maxInactiveInterval = 1800; // 30 minutos

        when(httpSession.isNew()).thenReturn(false);
        when(sessionUtils.isAuthenticated()).thenReturn(true);
        when(httpSession.getId()).thenReturn("test-session-id");
        when(httpSession.getCreationTime()).thenReturn(creationTime);
        when(httpSession.getLastAccessedTime()).thenReturn(lastAccessedTime);
        when(httpSession.getMaxInactiveInterval()).thenReturn(maxInactiveInterval);

        // When
        SessionInfoDto sessionInfo = authenticationService.getSessionInfo(httpSession);

        // Then
        assertThat(sessionInfo).isNotNull();
        assertThat(sessionInfo.getSessionId()).isEqualTo("test-session-id");
        assertThat(sessionInfo.getMaxInactiveInterval()).isEqualTo(maxInactiveInterval);

        // Verificar timestamps
        LocalDateTime expectedCreatedAt = Instant.ofEpochMilli(creationTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        LocalDateTime expectedLastAccessedAt = Instant.ofEpochMilli(lastAccessedTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        assertThat(sessionInfo.getCreatedAt()).isEqualTo(expectedCreatedAt);
        assertThat(sessionInfo.getLastAccessedAt()).isEqualTo(expectedLastAccessedAt);
    }

    @Test
    @DisplayName("Debe retornar null para información de sesión inválida")
    void shouldReturnNullForInvalidSessionInfo() {
        // Given
        when(httpSession.isNew()).thenReturn(true);

        // When
        SessionInfoDto sessionInfo = authenticationService.getSessionInfo(httpSession);

        // Then
        assertThat(sessionInfo).isNull();
    }
}