package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.auth.ChangePasswordDto;
import com.acainfo.mvp.dto.auth.LoginRequestDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import com.acainfo.mvp.model.enums.DayOfWeek;
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
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración para el controlador de profesores.
 * Verifica todos los endpoints disponibles para profesores en la iteración 1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("Tests de Integración - TeacherController")
class TeacherControllerIntegrationTest {

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
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseGroupRepository courseGroupRepository;

    @Autowired
    private GroupSessionRepository groupSessionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession teacherSession;
    private Teacher testTeacher;
    private Subject testSubject1;
    private Subject testSubject2;
    private CourseGroup activeGroup1;
    private CourseGroup activeGroup2;
    private Student testStudent1;
    private Student testStudent2;

    @BeforeEach
    void setUp() throws Exception {
        // Limpiar datos previos
        enrollmentRepository.deleteAll();
        groupSessionRepository.deleteAll();
        courseGroupRepository.deleteAll();
        subjectRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();

        // Crear datos de prueba
        setupTestData();

        // Crear sesión mock para el profesor
        teacherSession = new MockHttpSession();

        // Login del profesor
        LoginRequestDto loginDto = LoginRequestDto.builder()
                .email("profesor.martinez@universidad.edu")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk());
    }

    private void setupTestData() {
        // Crear profesor
        testTeacher = Teacher.builder()
                .name("Dr. Roberto Martínez")
                .email("profesor.martinez@universidad.edu")
                .password(passwordEncoder.encode("password123"))
                .build();
        testTeacher = teacherRepository.save(testTeacher);

        // Crear estudiantes
        testStudent1 = Student.builder()
                .name("Ana García")
                .email("ana.garcia@universidad.edu")
                .password(passwordEncoder.encode("password123"))
                .major("Ingeniería en Sistemas")
                .build();
        testStudent1 = studentRepository.save(testStudent1);

        testStudent2 = Student.builder()
                .name("Juan López")
                .email("juan.lopez@universidad.edu")
                .password(passwordEncoder.encode("password123"))
                .major("Ingeniería en Sistemas")
                .build();
        testStudent2 = studentRepository.save(testStudent2);

        // Crear asignaturas
        testSubject1 = Subject.builder()
                .name("Algoritmos y Estructuras de Datos")
                .major("Ingeniería en Sistemas")
                .courseYear(2)
                .build();
        testSubject1 = subjectRepository.save(testSubject1);

        testSubject2 = Subject.builder()
                .name("Sistemas Operativos")
                .major("Ingeniería en Sistemas")
                .courseYear(3)
                .build();
        testSubject2 = subjectRepository.save(testSubject2);

        // Crear grupos activos con sesiones
        activeGroup1 = CourseGroup.builder()
                .subject(testSubject1)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("250.00"))
                .maxCapacity(30)
                .build();
        activeGroup1 = courseGroupRepository.save(activeGroup1);

        // Agregar sesiones al grupo 1
        GroupSession session1 = GroupSession.builder()
                .courseGroup(activeGroup1)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 301")
                .build();
        groupSessionRepository.save(session1);

        GroupSession session2 = GroupSession.builder()
                .courseGroup(activeGroup1)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 301")
                .build();
        groupSessionRepository.save(session2);

        activeGroup2 = CourseGroup.builder()
                .subject(testSubject2)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("280.00"))
                .maxCapacity(25)
                .build();
        activeGroup2 = courseGroupRepository.save(activeGroup2);

        // Agregar sesiones al grupo 2
        GroupSession session3 = GroupSession.builder()
                .courseGroup(activeGroup2)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .classroom("Lab 203")
                .build();
        groupSessionRepository.save(session3);

        GroupSession session4 = GroupSession.builder()
                .courseGroup(activeGroup2)
                .dayOfWeek(DayOfWeek.THURSDAY)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .classroom("Lab 203")
                .build();
        groupSessionRepository.save(session4);

        // Inscribir algunos estudiantes
        Enrollment enrollment1 = Enrollment.builder()
                .student(testStudent1)
                .courseGroup(activeGroup1)
                .paymentStatus(com.acainfo.mvp.model.enums.PaymentStatus.PAID)
                .build();
        enrollmentRepository.save(enrollment1);

        Enrollment enrollment2 = Enrollment.builder()
                .student(testStudent2)
                .courseGroup(activeGroup1)
                .paymentStatus(com.acainfo.mvp.model.enums.PaymentStatus.PENDING)
                .build();
        enrollmentRepository.save(enrollment2);

        Enrollment enrollment3 = Enrollment.builder()
                .student(testStudent1)
                .courseGroup(activeGroup2)
                .paymentStatus(com.acainfo.mvp.model.enums.PaymentStatus.PAID)
                .build();
        enrollmentRepository.save(enrollment3);
    }

    // ========== TESTS DE PERFIL ==========

    @Test
    @DisplayName("Obtener perfil del profesor autenticado")
    void testGetMyProfile() throws Exception {
        mockMvc.perform(get("/api/teachers/profile")
                        .session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Roberto Martínez"))
                .andExpect(jsonPath("$.email").value("profesor.martinez@universidad.edu"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Obtener perfil sin autenticación - debe fallar")
    void testGetMyProfileWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/teachers/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Actualizar perfil del profesor")
    void testUpdateProfile() throws Exception {
        TeacherDto updateDto = TeacherDto.builder()
                .name("Dr. Roberto Martínez González")
                .build();

        mockMvc.perform(put("/api/teachers/profile")
                        .session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Roberto Martínez González"))
                .andExpect(jsonPath("$.email").value("profesor.martinez@universidad.edu"));
    }

    @Test
    @DisplayName("Cambiar contraseña exitosamente")
    void testChangePassword() throws Exception {
        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("password123")
                .newPassword("newSecurePassword456")
                .build();

        mockMvc.perform(post("/api/teachers/change-password")
                        .session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contraseña actualizada exitosamente"));
    }

    // ========== TESTS DE HORARIO (FUNCIONALIDAD PRINCIPAL ITERACIÓN 1) ==========

    @Test
    @DisplayName("Obtener horario semanal completo")
    void testGetMySchedule() throws Exception {
        mockMvc.perform(get("/api/teachers/schedule")
                        .session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(testTeacher.getId()))
                .andExpect(jsonPath("$.teacherName").value("Dr. Roberto Martínez"))
                .andExpect(jsonPath("$.totalWeeklyHours").value(8)) // 4 sesiones de 2 horas
                .andExpect(jsonPath("$.scheduleByDay").exists())
                .andExpect(jsonPath("$.scheduleByDay.MONDAY", hasSize(1)))
                .andExpect(jsonPath("$.scheduleByDay.TUESDAY", hasSize(1)))
                .andExpect(jsonPath("$.scheduleByDay.WEDNESDAY", hasSize(1)))
                .andExpect(jsonPath("$.scheduleByDay.THURSDAY", hasSize(1)))
                .andExpect(jsonPath("$.scheduleByDay.FRIDAY", hasSize(0)));
    }

    @Test
    @DisplayName("Obtener horario de un día específico - Lunes")
    void testGetScheduleByDay() throws Exception {
        mockMvc.perform(get("/api/teachers/schedule/MONDAY")
                        .session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$[0].classroom").value("Aula 301"))
                .andExpect(jsonPath("$[0].subjectName").value("Algoritmos y Estructuras de Datos"))
                .andExpect(jsonPath("$[0].groupType").value("REGULAR"));
    }

    @Test
    @DisplayName("Obtener horario de día sin clases - Viernes")
    void testGetScheduleByDayEmpty() throws Exception {
        mockMvc.perform(get("/api/teachers/schedule/FRIDAY")
                        .session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Obtener horario con día inválido - debe fallar")
    void testGetScheduleByInvalidDay() throws Exception {
        mockMvc.perform(get("/api/teachers/schedule/INVALID_DAY")
                        .session(teacherSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== TESTS DE GRUPOS ==========

    @Test
    @DisplayName("Obtener mis grupos")
    void testGetMyGroups() throws Exception {
        mockMvc.perform(get("/api/teachers/groups")
                        .session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.subjectName == 'Algoritmos y Estructuras de Datos')]").exists())
                .andExpect(jsonPath("$[?(@.subjectName == 'Sistemas Operativos')]").exists())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].enrolledStudents").value(2))
                .andExpect(jsonPath("$[1].enrolledStudents").value(1));
    }

    // ========== TESTS DE ESTADÍSTICAS ==========

    @Test
    @DisplayName("Obtener estadísticas del profesor")
    void testGetMyStats() throws Exception {
        mockMvc.perform(get("/api/teachers/stats")
                        .session(teacherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(testTeacher.getId()))
                .andExpect(jsonPath("$.teacherName").value("Dr. Roberto Martínez"))
                .andExpect(jsonPath("$.totalGroups").value(2))
                .andExpect(jsonPath("$.activeGroups").value(2))
                .andExpect(jsonPath("$.totalStudents").value(3)) // 2 en grupo1 + 1 en grupo2
                .andExpect(jsonPath("$.uniqueStudents").value(2)) // Ana y Juan
                .andExpect(jsonPath("$.weeklyHours").value(8))
                .andExpect(jsonPath("$.groupStats", hasSize(2)));
    }

    // ========== TESTS DE SEGURIDAD Y VALIDACIONES ==========

    @Test
    @DisplayName("Acceder a endpoints sin autenticación - debe fallar")
    void testEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/teachers/schedule"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/teachers/groups"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/teachers/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Estudiante intentando acceder a endpoints de profesor - debe fallar")
    void testStudentAccessingTeacherEndpoints() throws Exception {
        // Crear sesión de estudiante
        MockHttpSession studentSession = new MockHttpSession();

        LoginRequestDto studentLogin = LoginRequestDto.builder()
                .email("ana.garcia@universidad.edu")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentLogin)))
                .andExpect(status().isOk());

        // Intentar acceder a endpoints de profesor
        mockMvc.perform(get("/api/teachers/schedule")
                        .session(studentSession))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Validación de datos en actualización de perfil")
    void testProfileUpdateValidation() throws Exception {
        // Nombre vacío
        TeacherDto invalidDto = TeacherDto.builder()
                .name("")
                .build();

        mockMvc.perform(put("/api/teachers/profile")
                        .session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Nombre muy largo
        invalidDto.setName("A".repeat(101));
        mockMvc.perform(put("/api/teachers/profile")
                        .session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Cambiar contraseña con contraseña actual incorrecta - debe fallar")
    void testChangePasswordWithWrongCurrent() throws Exception {
        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword456")
                .build();

        mockMvc.perform(post("/api/teachers/change-password")
                        .session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Contraseña actual incorrecta"));
    }

    @Test
    @DisplayName("Cambiar contraseña con nueva contraseña inválida - debe fallar")
    void testChangePasswordWithInvalidNew() throws Exception {
        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("password123")
                .newPassword("short") // Menos de 8 caracteres
                .build();

        mockMvc.perform(post("/api/teachers/change-password")
                        .session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}