package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.auth.ChangePasswordDto;
import com.acainfo.mvp.dto.grouprequest.CreateGroupRequestDto;
import com.acainfo.mvp.dto.student.StudentRegistrationDto;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import com.acainfo.mvp.model.enums.PaymentStatus;
import com.acainfo.mvp.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración para el controlador de estudiantes.
 * Verifica todos los endpoints disponibles para estudiantes en la iteración 1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("Tests de Integración - StudentController")
class StudentControllerIntegrationTest {

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

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseGroupRepository courseGroupRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private GroupRequestRepository groupRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession studentSession;
    private Student testStudent;
    private Subject testSubject1;
    private Subject testSubject2;
    private CourseGroup activeGroup;
    private CourseGroup inactiveGroup;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() throws Exception {
        // Limpiar datos previos
        groupRequestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseGroupRepository.deleteAll();
        subjectRepository.deleteAll();
        teacherRepository.deleteAll();
        studentRepository.deleteAll();

        // Crear datos de prueba
        setupTestData();

        // Crear sesión mock para el estudiante
        studentSession = new MockHttpSession();

        // Registrar estudiante con la sesión
        StudentRegistrationDto registrationDto = StudentRegistrationDto.builder()
                .name("Carlos Mendoza")
                .email("carlos.mendoza@universidad.edu")
                .password("password123")
                .major("Ingeniería en Sistemas")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated());

        // Obtener el estudiante creado
        testStudent = studentRepository.findByEmail("carlos.mendoza@universidad.edu")
                .orElseThrow();
    }

    private void setupTestData() {
        // Crear profesor
        testTeacher = Teacher.builder()
                .name("Dr. Ana López")
                .email("ana.lopez@universidad.edu")
                .password(passwordEncoder.encode("password123"))
                .build();
        testTeacher = teacherRepository.save(testTeacher);

        // Crear asignaturas
        testSubject1 = Subject.builder()
                .name("Programación I")
                .major("Ingeniería en Sistemas")
                .courseYear(1)
                .build();
        testSubject1 = subjectRepository.save(testSubject1);

        testSubject2 = Subject.builder()
                .name("Base de Datos")
                .major("Ingeniería en Sistemas")
                .courseYear(2)
                .build();
        testSubject2 = subjectRepository.save(testSubject2);

        // Crear grupo activo
        activeGroup = CourseGroup.builder()
                .subject(testSubject1)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("200.00"))
                .maxCapacity(30)
                .build();
        activeGroup = courseGroupRepository.save(activeGroup);

        // Crear grupo inactivo
        inactiveGroup = CourseGroup.builder()
                .subject(testSubject2)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("180.00"))
                .maxCapacity(25)
                .build();
        inactiveGroup = courseGroupRepository.save(inactiveGroup);
    }

    // ========== TESTS DE PERFIL ==========

    @Test
    @DisplayName("Obtener perfil del estudiante autenticado")
    void testGetMyProfile() throws Exception {
        mockMvc.perform(get("/api/students/profile")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carlos Mendoza"))
                .andExpect(jsonPath("$.email").value("carlos.mendoza@universidad.edu"))
                .andExpect(jsonPath("$.major").value("Ingeniería en Sistemas"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Obtener perfil sin autenticación - debe fallar")
    void testGetMyProfileWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/students/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Actualizar perfil del estudiante")
    void testUpdateProfile() throws Exception {
        StudentRegistrationDto updateDto = StudentRegistrationDto.builder()
                .name("Carlos Mendoza Actualizado")
                .build();

        mockMvc.perform(put("/api/students/profile")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carlos Mendoza Actualizado"))
                .andExpect(jsonPath("$.email").value("carlos.mendoza@universidad.edu")); // Email no cambia
    }

    @Test
    @DisplayName("Cambiar contraseña exitosamente")
    void testChangePassword() throws Exception {
        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("password123")
                .newPassword("newPassword456")
                .build();

        mockMvc.perform(post("/api/students/change-password")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contraseña actualizada exitosamente"));
    }

    @Test
    @DisplayName("Cambiar contraseña con contraseña actual incorrecta - debe fallar")
    void testChangePasswordWithWrongCurrent() throws Exception {
        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword456")
                .build();

        mockMvc.perform(post("/api/students/change-password")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación"));
    }

    // ========== TESTS DE CONSULTAS ACADÉMICAS ==========

    @Test
    @DisplayName("Obtener todas las asignaturas de mi carrera")
    void testGetMyMajorSubjects() throws Exception {
        mockMvc.perform(get("/api/students/subjects")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.name == 'Programación I')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Base de Datos')]").exists())
                .andExpect(jsonPath("$[*].major", everyItem(equalTo("Ingeniería en Sistemas"))));
    }

    @Test
    @DisplayName("Obtener asignaturas por año específico")
    void testGetSubjectsByYear() throws Exception {
        mockMvc.perform(get("/api/students/subjects/year/1")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Programación I"))
                .andExpect(jsonPath("$[0].courseYear").value(1));
    }

    @Test
    @DisplayName("Obtener asignaturas con año inválido - debe fallar")
    void testGetSubjectsByInvalidYear() throws Exception {
        mockMvc.perform(get("/api/students/subjects/year/7")
                        .session(studentSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El año de curso debe estar entre 1 y 6"));
    }

    @Test
    @DisplayName("Obtener grupos activos para una asignatura")
    void testGetActiveGroupsForSubject() throws Exception {
        mockMvc.perform(get("/api/students/subjects/{subjectId}/active-groups", testSubject1.getId())
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].teacherName").value("Dr. Ana López"));
    }

    // ========== TESTS DE INSCRIPCIONES ==========

    @Test
    @DisplayName("Obtener mis inscripciones - inicialmente vacío")
    void testGetMyEnrollmentsEmpty() throws Exception {
        mockMvc.perform(get("/api/students/enrollments")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Inscribirse en un grupo y verificar inscripción")
    void testEnrollAndVerify() throws Exception {
        // Inscribirse
        mockMvc.perform(post("/api/students/subjects/{subjectId}/groups/{groupId}/enroll",
                        testSubject1.getId(), activeGroup.getId())
                        .session(studentSession))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.enrollmentId").exists())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        // Verificar inscripción
        mockMvc.perform(get("/api/students/enrollments")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subjectName").value("Programación I"))
                .andExpect(jsonPath("$[0].teacherName").value("Dr. Ana López"))
                .andExpect(jsonPath("$[0].paymentStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Inscribirse dos veces en el mismo grupo - debe fallar")
    void testDuplicateEnrollment() throws Exception {
        // Primera inscripción
        mockMvc.perform(post("/api/students/subjects/{subjectId}/groups/{groupId}/enroll",
                        testSubject1.getId(), activeGroup.getId())
                        .session(studentSession))
                .andExpect(status().isCreated());

        // Segunda inscripción - debe fallar
        mockMvc.perform(post("/api/students/subjects/{subjectId}/groups/{groupId}/enroll",
                        testSubject1.getId(), activeGroup.getId())
                        .session(studentSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("El estudiante ya está inscrito en este grupo")));
    }

    // ========== TESTS DE SOLICITUDES DE GRUPO ==========

    @Test
    @DisplayName("Crear solicitud de grupo cuando no hay grupos activos")
    void testCreateGroupRequest() throws Exception {
        CreateGroupRequestDto requestDto = CreateGroupRequestDto.builder()
                .subjectId(testSubject2.getId())
                .build();

        mockMvc.perform(post("/api/students/group-requests")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.message").
                        value(containsString(
                                "Solicitud creada exitosamente. " +
                                        "El administrador revisará tu solicitud.")));
    }

    @Test
    @DisplayName("Obtener mis solicitudes de grupo")
    void testGetMyGroupRequests() throws Exception {
        // Crear una solicitud primero
        CreateGroupRequestDto requestDto = CreateGroupRequestDto.builder()
                .subjectId(testSubject2.getId())
                .build();

        mockMvc.perform(post("/api/students/group-requests")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        // Obtener solicitudes
        mockMvc.perform(get("/api/students/group-requests")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subjectName").value("Base de Datos"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Crear solicitud duplicada - debe fallar")
    void testDuplicateGroupRequest() throws Exception {
        CreateGroupRequestDto requestDto = CreateGroupRequestDto.builder()
                .subjectId(testSubject2.getId())
                .build();

        // Primera solicitud
        mockMvc.perform(post("/api/students/group-requests")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        // Segunda solicitud - debe fallar
        mockMvc.perform(post("/api/students/group-requests")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Ya tiene una solicitud pendiente")));
    }

    // ========== TESTS DE ESTADÍSTICAS ==========

    @Test
    @DisplayName("Obtener estadísticas del estudiante")
    void testGetMyStats() throws Exception {
        // Inscribir al estudiante en un grupo
        mockMvc.perform(post("/api/students/subjects/{subjectId}/groups/{groupId}/enroll",
                        testSubject1.getId(), activeGroup.getId())
                        .session(studentSession))
                .andExpect(status().isCreated());

        // Obtener estadísticas
        mockMvc.perform(get("/api/students/stats")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(testStudent.getId()))
                .andExpect(jsonPath("$.studentName").value("Carlos Mendoza"))
                .andExpect(jsonPath("$.major").value("Ingeniería en Sistemas"))
                .andExpect(jsonPath("$.totalEnrollments").value(1))
                .andExpect(jsonPath("$.activeEnrollments").value(1))
                .andExpect(jsonPath("$.pendingPayments").value(1))
                .andExpect(jsonPath("$.totalSubjectsInMajor").value(2));
    }

    // ========== TESTS DE SEGURIDAD Y VALIDACIONES ==========

    @Test
    @DisplayName("Acceder a endpoints sin autenticación - debe fallar")
    void testEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/students/subjects"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/students/enrollments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/students/group-requests"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/students/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Validación de datos en actualización de perfil")
    void testProfileUpdateValidation() throws Exception {
        // Nombre vacío
        StudentRegistrationDto invalidDto = StudentRegistrationDto.builder()
                .name("")
                .build();

        mockMvc.perform(put("/api/students/profile")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Nombre muy largo
        invalidDto.setName("A".repeat(101));
        mockMvc.perform(put("/api/students/profile")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}