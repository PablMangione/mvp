package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.auth.*;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.exception.auth.*;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private StudentRegistrationDto validRegistrationDto;
    private LoginRequestDto validLoginDto;
    private ChangePasswordDto validChangePasswordDto;
    private Student testStudent;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() {
        // Preparar datos de prueba
        validRegistrationDto = StudentRegistrationDto.builder()
                .name("Juan Pérez")
                .email("juan.perez@example.com")
                .password("password123")
                .major("Ingeniería Informática")
                .build();

        validLoginDto = LoginRequestDto.builder()
                .email("juan.perez@example.com")
                .password("password123")
                .build();

        validChangePasswordDto = ChangePasswordDto.builder()
                .currentPassword("password123")
                .newPassword("newPassword456")
                .confirmPassword("newPassword456")
                .build();

        testStudent = Student.builder()
                .name("Juan Pérez")
                .email("juan.perez@example.com")
                .password("encodedPassword")
                .major("Ingeniería Informática")
                .build();
        ReflectionTestUtils.setField(testStudent, "id", 1L);

        testTeacher = Teacher.builder()
                .name("María García")
                .email("maria.garcia@example.com")
                .password("encodedPassword")
                .build();
        ReflectionTestUtils.setField(testTeacher, "id", 1L);
    }

    @Test
    @DisplayName("Registro exitoso de estudiante")
    void registerStudent_Success() {
        // Given
        when(studentRepository.existsByEmail(anyString())).thenReturn(false);
        when(teacherRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // Configurar el mock para devolver el estudiante con ID cuando se guarde
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student savedStudent = invocation.getArgument(0);
            // Simular que la BD asigna un ID usando ReflectionTestUtils
            ReflectionTestUtils.setField(savedStudent, "id", 1L);
            return savedStudent;
        });

        // When
        ApiResponseDto<LoginResponseDto> result = authService.registerStudent(validRegistrationDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(1L); // Verificar que el ID no es null
        assertThat(result.getData().getEmail()).isEqualTo(validRegistrationDto.getEmail());
        assertThat(result.getData().getName()).isEqualTo(validRegistrationDto.getName());
        assertThat(result.getData().getRole()).isEqualTo("STUDENT");

        verify(studentRepository).save(any(Student.class));
        verify(passwordEncoder).encode(validRegistrationDto.getPassword());
    }

    @Test
    @DisplayName("Registro falla cuando email ya existe")
    void registerStudent_EmailAlreadyExists() {
        // Given
        when(studentRepository.existsByEmail(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.registerStudent(validRegistrationDto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("El email ya está registrado en el sistema");

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Login exitoso de estudiante")
    void loginStudent_Success() {
        // Given
        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(testStudent));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When
        ApiResponseDto<LoginResponseDto> result = authService.loginStudent(validLoginDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getEmail()).isEqualTo(testStudent.getEmail());
        assertThat(result.getData().getRole()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("Login falla con credenciales inválidas")
    void loginStudent_InvalidCredentials() {
        // Given
        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.of(testStudent));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.loginStudent(validLoginDto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    @DisplayName("Login falla cuando estudiante no existe")
    void loginStudent_UserNotFound() {
        // Given
        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.loginStudent(validLoginDto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    @DisplayName("Login exitoso de profesor")
    void loginTeacher_Success() {
        // Given
        when(teacherRepository.findByEmail(anyString())).thenReturn(Optional.of(testTeacher));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When
        ApiResponseDto<LoginResponseDto> result = authService.loginTeacher(validLoginDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getRole()).isEqualTo("TEACHER");
    }

    @Test
    @DisplayName("Cambio de contraseña exitoso de estudiante")
    void changeStudentPassword_Success() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(passwordEncoder.matches("password123", testStudent.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword456")).thenReturn("newEncodedPassword");
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        // When
        ApiResponseDto<Void> result = authService.changeStudentPassword(1L, validChangePasswordDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Contraseña actualizada exitosamente");

        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("Cambio de contraseña falla cuando las contraseñas no coinciden")
    void changeStudentPassword_PasswordMismatch() {
        // Given
        ChangePasswordDto mismatchDto = ChangePasswordDto.builder()
                .currentPassword("password123")
                .newPassword("newPassword456")
                .confirmPassword("differentPassword789")
                .build();

        // When/Then
        assertThatThrownBy(() -> authService.changeStudentPassword(1L, mismatchDto))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessage("Las contraseñas nuevas no coinciden");

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Cambio de contraseña falla con contraseña actual incorrecta")
    void changeStudentPassword_InvalidCurrentPassword() {
        // Given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.changeStudentPassword(1L, validChangePasswordDto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("La contraseña actual es incorrecta");

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Verificar disponibilidad de email - disponible")
    void isEmailAvailable_True() {
        // Given
        when(studentRepository.existsByEmail(anyString())).thenReturn(false);
        when(teacherRepository.existsByEmail(anyString())).thenReturn(false);

        // When
        boolean result = authService.isEmailAvailable("nuevo@example.com");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Verificar disponibilidad de email - no disponible")
    void isEmailAvailable_False() {
        // Given
        when(studentRepository.existsByEmail(anyString())).thenReturn(true);

        // When
        boolean result = authService.isEmailAvailable("existente@example.com");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Verificar existencia de estudiante")
    void studentExists() {
        // Given
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(studentRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThat(authService.studentExists(1L)).isTrue();
        assertThat(authService.studentExists(999L)).isFalse();
    }

    @Test
    @DisplayName("Verificar existencia de profesor")
    void teacherExists() {
        // Given
        when(teacherRepository.existsById(1L)).thenReturn(true);
        when(teacherRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThat(authService.teacherExists(1L)).isTrue();
        assertThat(authService.teacherExists(999L)).isFalse();
    }
}