package com.acainfo.mvp.integration;

import com.acainfo.mvp.dto.enrollment.CreateEnrollmentDto;
import com.acainfo.mvp.dto.student.StudentRegistrationDto;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import com.acainfo.mvp.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración para el flujo completo de inscripción de estudiantes.
 * Verifica el proceso end-to-end desde el registro hasta la inscripción en grupos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("Tests de Integración - Inscripción de Estudiantes")
class StudentEnrollmentIntegrationTest {

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
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseGroupRepository courseGroupRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String studentSessionCookie;
    private Subject testSubject;
    private CourseGroup activeGroup;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() throws Exception {
        // Crear datos de prueba en la BD
        setupTestData();

        // Registrar un estudiante y obtener su sesión
        StudentRegistrationDto registrationDto = StudentRegistrationDto.builder()
                .name("Ana García")
                .email("ana.garcia@universidad.edu")
                .password("password123")
                .major("Ingeniería en Sistemas")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andReturn();

        studentSessionCookie = result.getResponse().getHeader("Set-Cookie");
    }

    private void setupTestData() {
        // Crear profesor
        testTeacher = Teacher.builder()
                .name("Dr. Roberto Martínez")
                .email("roberto.martinez@universidad.edu")
                .password(passwordEncoder.encode("password123"))
                .build();
        testTeacher = teacherRepository.save(testTeacher);

        // Crear asignatura
        testSubject = Subject.builder()
                .name("Programación Orientada a Objetos")
                .major("Ingeniería en Sistemas")
                .courseYear(2)
                .build();
        testSubject = subjectRepository.save(testSubject);

        // Crear grupo activo
        activeGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("150.00"))
                .maxCapacity(30)
                .build();
        activeGroup = courseGroupRepository.save(activeGroup);
    }

    @Test
    @DisplayName("Consultar asignaturas de mi carrera")
    void testGetSubjectsFromMyMajor() throws Exception {
        mockMvc.perform(get("/api/students/subjects")
                        .header("Cookie", studentSessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.name == 'Programación Orientada a Objetos')]").exists())
                .andExpect(jsonPath("$[?(@.major == 'Ingeniería en Sistemas')]").exists());
    }

    @Test
    @DisplayName("Inscripción exitosa en grupo activo")
    void testSuccessfulEnrollment() throws Exception {
        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .courseGroupId(activeGroup.getId())
                .build();

        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.enrollmentId").exists())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.message").value(containsString("exitosamente")));
    }

    @Test
    @DisplayName("Inscripción fallida - Grupo no activo")
    void testEnrollmentFailsWhenGroupNotActive() throws Exception {
        // Crear grupo planificado
        CourseGroup plannedGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("150.00"))
                .maxCapacity(30)
                .build();
        plannedGroup = courseGroupRepository.save(plannedGroup);

        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .courseGroupId(plannedGroup.getId())
                .build();

        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("grupos activos")));
    }

    @Test
    @DisplayName("Inscripción fallida - Asignatura de otra carrera")
    void testEnrollmentFailsWhenSubjectFromDifferentMajor() throws Exception {
        // Crear asignatura de otra carrera
        Subject otherMajorSubject = Subject.builder()
                .name("Anatomía Humana")
                .major("Medicina")
                .courseYear(1)
                .build();
        otherMajorSubject = subjectRepository.save(otherMajorSubject);

        CourseGroup otherMajorGroup = CourseGroup.builder()
                .subject(otherMajorSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("200.00"))
                .maxCapacity(25)
                .build();
        otherMajorGroup = courseGroupRepository.save(otherMajorGroup);

        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .courseGroupId(otherMajorGroup.getId())
                .build();

        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("asignaturas de su carrera")));
    }

    @Test
    @DisplayName("Inscripción fallida - Inscripción duplicada")
    void testEnrollmentFailsWhenAlreadyEnrolled() throws Exception {
        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .courseGroupId(activeGroup.getId())
                .build();

        // Primera inscripción exitosa
        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isCreated());

        // Segunda inscripción debe fallar
        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Ya existe una inscripción")));
    }

    @Test
    @DisplayName("Consultar mis inscripciones")
    void testGetMyEnrollments() throws Exception {
        // Primero inscribirse
        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .courseGroupId(activeGroup.getId())
                .build();

        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isCreated());

        // Consultar inscripciones
        mockMvc.perform(get("/api/students/enrollments")
                        .header("Cookie", studentSessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subjectName").value(testSubject.getName()))
                .andExpect(jsonPath("$[0].teacherName").value(testTeacher.getName()))
                .andExpect(jsonPath("$[0].paymentStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Cancelar inscripción exitosamente")
    void testCancelEnrollment() throws Exception {
        // Primero inscribirse
        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .courseGroupId(activeGroup.getId())
                .build();

        MvcResult enrollResult = mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = enrollResult.getResponse().getContentAsString();
        Long enrollmentId = objectMapper.readTree(responseBody).get("enrollmentId").asLong();

        // Cancelar inscripción
        mockMvc.perform(delete("/api/students/enrollments/" + enrollmentId)
                        .header("Cookie", studentSessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("cancelada exitosamente")));

        // Verificar que ya no aparece en la lista
        mockMvc.perform(get("/api/students/enrollments")
                        .header("Cookie", studentSessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Inscripción fallida - Grupo lleno")
    void testEnrollmentFailsWhenGroupIsFull() throws Exception {
        // Crear grupo con capacidad mínima
        CourseGroup fullGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("150.00"))
                .maxCapacity(1) // Solo 1 espacio
                .build();
        fullGroup = courseGroupRepository.save(fullGroup);

        // Registrar otro estudiante
        StudentRegistrationDto otherStudent = StudentRegistrationDto.builder()
                .name("Carlos López")
                .email("carlos.lopez@universidad.edu")
                .password("password123")
                .major("Ingeniería en Sistemas")
                .build();

        MvcResult otherResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherStudent)))
                .andExpect(status().isCreated())
                .andReturn();

        String otherSessionCookie = otherResult.getResponse().getHeader("Set-Cookie");

        // Primera inscripción exitosa
        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .courseGroupId(fullGroup.getId())
                .build();

        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isCreated());

        // Segunda inscripción debe fallar por capacidad
        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", otherSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("capacidad máxima")));
    }

    @Test
    @DisplayName("Consultar estadísticas del estudiante")
    void testGetStudentStats() throws Exception {
        // Inscribirse en un grupo
        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .courseGroupId(activeGroup.getId())
                .build();

        mockMvc.perform(post("/api/students/enroll")
                        .header("Cookie", studentSessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentDto)))
                .andExpect(status().isCreated());

        // Consultar estadísticas
        mockMvc.perform(get("/api/students/stats")
                        .header("Cookie", studentSessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnrollments").value(1))
                .andExpect(jsonPath("$.activeEnrollments").value(1))
                .andExpect(jsonPath("$.pendingPayments").value(1))
                .andExpect(jsonPath("$.major").value("Ingeniería en Sistemas"));
    }
}