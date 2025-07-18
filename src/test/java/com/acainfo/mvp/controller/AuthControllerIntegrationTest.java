package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.auth.LoginRequestDto;
import com.acainfo.mvp.dto.student.StudentRegistrationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración para el controlador de autenticación.
 * Verifica el flujo completo de registro y login de estudiantes.
 *
 * Usa TestContainers para ejecutar una instancia real de MySQL,
 * garantizando que las pruebas sean lo más cercanas posible al entorno real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional // Rollback después de cada test para mantener la BD limpia
@DisplayName("Tests de Integración - Autenticación")
class AuthControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("acainfo_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private StudentRegistrationDto validRegistrationDto;
    private LoginRequestDto validLoginDto;

    @BeforeEach
    void setUp() {
        // Preparar datos de prueba válidos
        validRegistrationDto = StudentRegistrationDto.builder()
                .name("Juan Pérez")
                .email("juan.perez@universidad.edu")
                .password("password123")
                .major("Ingeniería en Sistemas")
                .build();

        validLoginDto = LoginRequestDto.builder()
                .email("juan.perez@universidad.edu")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("Registro exitoso de nuevo estudiante")
    void testSuccessfulStudentRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(validRegistrationDto.getEmail()))
                .andExpect(jsonPath("$.name").value(validRegistrationDto.getName()))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.password").doesNotExist()); // No debe devolver la contraseña
    }

    @Test
    @DisplayName("Registro fallido - Email duplicado")
    void testRegistrationFailsWithDuplicateEmail() throws Exception {
        // Primer registro exitoso
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated());

        // Segundo registro con el mismo email debe fallar
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("email ya está registrado")));
    }

    @Test
    @DisplayName("Registro fallido - Datos inválidos")
    void testRegistrationFailsWithInvalidData() throws Exception {
        StudentRegistrationDto invalidDto = StudentRegistrationDto.builder()
                .name("") // Nombre vacío
                .email("email-invalido") // Email sin formato correcto
                .password("123") // Contraseña muy corta
                .major("") // Carrera vacía
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("validación")));
    }

    @Test
    @DisplayName("Login exitoso después de registro")
    void testSuccessfulLoginAfterRegistration() throws Exception {
        // Primero registrar al estudiante
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated());

        // Luego hacer login con las mismas credenciales
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(validLoginDto.getEmail()))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(header().exists("Set-Cookie")); // Debe establecer cookie de sesión
    }

    @Test
    @DisplayName("Login fallido - Credenciales incorrectas")
    void testLoginFailsWithInvalidCredentials() throws Exception {
        // Primero registrar al estudiante
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated());

        // Intentar login con contraseña incorrecta
        LoginRequestDto invalidLoginDto = LoginRequestDto.builder()
                .email(validLoginDto.getEmail())
                .password("contraseña-incorrecta")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Credenciales inválidas")));
    }

    @Test
    @DisplayName("Login fallido - Usuario no existe")
    void testLoginFailsWithNonExistentUser() throws Exception {
        LoginRequestDto nonExistentUserDto = LoginRequestDto.builder()
                .email("no.existe@universidad.edu")
                .password("cualquier-password")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentUserDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Obtener usuario actual - Sin autenticación")
    void testGetCurrentUserWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("Obtener usuario actual - Con autenticación")
    void testGetCurrentUserWithAuthentication() throws Exception {
        // Registrar y obtener la cookie de sesión
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionCookie = result.getResponse().getHeader("Set-Cookie");

        // Usar la cookie para obtener el usuario actual
        mockMvc.perform(get("/api/auth/me")
                        .header("Cookie", sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.email").value(validRegistrationDto.getEmail()))
                .andExpect(jsonPath("$.name").value(validRegistrationDto.getName()))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("Logout exitoso")
    void testSuccessfulLogout() throws Exception {
        // Registrar y obtener la cookie de sesión
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionCookie = result.getResponse().getHeader("Set-Cookie");

        // Hacer logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Cookie", sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("cerrada correctamente")));

        // Verificar que la sesión ya no es válida
        mockMvc.perform(get("/api/auth/me")
                        .header("Cookie", sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("Acceso a endpoint protegido sin autenticación")
    void testAccessProtectedEndpointWithoutAuth() throws Exception {
        // Intentar acceder al perfil del estudiante sin autenticación
        mockMvc.perform(get("/api/students/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Acceso a endpoint protegido con autenticación")
    void testAccessProtectedEndpointWithAuth() throws Exception {
        // Registrar y obtener la cookie de sesión
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionCookie = result.getResponse().getHeader("Set-Cookie");

        // Acceder al perfil con autenticación
        mockMvc.perform(get("/api/students/profile")
                        .header("Cookie", sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(validRegistrationDto.getEmail()))
                .andExpect(jsonPath("$.name").value(validRegistrationDto.getName()))
                .andExpect(jsonPath("$.major").value(validRegistrationDto.getMajor()));
    }

    @Test
    @DisplayName("Registro con todos los campos opcionales")
    void testRegistrationWithAllFields() throws Exception {
        StudentRegistrationDto completeDto = StudentRegistrationDto.builder()
                .name("María González López")
                .email("maria.gonzalez@universidad.edu")
                .password("superSecurePassword123!")
                .major("Medicina")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(completeDto.getName()))
                .andExpect(jsonPath("$.email").value(completeDto.getEmail()))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.authenticated").value(true));
    }
}