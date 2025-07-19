package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.auth.LoginRequestDto;
import com.acainfo.mvp.dto.coursegroup.CreateCourseGroupDto;
import com.acainfo.mvp.dto.coursegroup.CreateGroupSessionDto;
import com.acainfo.mvp.dto.coursegroup.UpdateGroupStatusDto;
import com.acainfo.mvp.dto.grouprequest.UpdateRequestStatusDto;
import com.acainfo.mvp.dto.student.CreateStudentDto;
import com.acainfo.mvp.dto.subject.CreateSubjectDto;
import com.acainfo.mvp.dto.subject.UpdateSubjectDto;
import com.acainfo.mvp.dto.teacher.CreateTeacherDto;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.*;
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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración para el controlador de administración.
 * Verifica todos los endpoints administrativos de la iteración 1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("Tests de Integración - AdminController")
class AdminControllerIntegrationTest {

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
    private AdminRepository adminRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseGroupRepository courseGroupRepository;

    @Autowired
    private GroupRequestRepository groupRequestRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession adminSession;
    private Admin testAdmin;
    private Student existingStudent;
    private Teacher existingTeacher;
    private Subject existingSubject;
    private CourseGroup existingGroup;

    @BeforeEach
    void setUp() throws Exception {
        // Limpiar datos previos
        enrollmentRepository.deleteAll();
        groupRequestRepository.deleteAll();
        courseGroupRepository.deleteAll();
        subjectRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        adminRepository.deleteAll();

        // Crear datos de prueba
        setupTestData();

        // Crear sesión mock para el admin
        adminSession = new MockHttpSession();

        // Login del admin
        LoginRequestDto loginDto = LoginRequestDto.builder()
                .email("admin@universidad.edu")
                .password("adminPassword123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk());
    }

    private void setupTestData() {
        // Crear admin
        testAdmin = Admin.builder()
                .name("Administrador Principal")
                .email("admin@universidad.edu")
                .password(passwordEncoder.encode("adminPassword123"))
                .build();
        testAdmin = adminRepository.save(testAdmin);

        // Crear estudiante existente
        existingStudent = Student.builder()
                .name("María González")
                .email("maria.gonzalez@universidad.edu")
                .password(passwordEncoder.encode("password123"))
                .major("Ingeniería en Sistemas")
                .build();
        existingStudent = studentRepository.save(existingStudent);

        // Crear profesor existente
        existingTeacher = Teacher.builder()
                .name("Dr. Carlos Pérez")
                .email("carlos.perez@universidad.edu")
                .password(passwordEncoder.encode("password123"))
                .build();
        existingTeacher = teacherRepository.save(existingTeacher);

        // Crear asignatura existente
        existingSubject = Subject.builder()
                .name("Cálculo I")
                .major("Ingeniería en Sistemas")
                .courseYear(1)
                .build();
        existingSubject = subjectRepository.save(existingSubject);

        // Crear grupo existente
        existingGroup = CourseGroup.builder()
                .subject(existingSubject)
                .teacher(existingTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("200.00"))
                .maxCapacity(30)
                .build();
        existingGroup = courseGroupRepository.save(existingGroup);
    }

    // ========== TESTS DE GESTIÓN DE ALUMNOS ==========

    @Test
    @DisplayName("Listar estudiantes con paginación")
    void testGetAllStudents() throws Exception {
        mockMvc.perform(get("/api/admin/students")
                        .session(adminSession)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("María González"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("Alta de alumno exitosa")
    void testCreateStudent() throws Exception {
        CreateStudentDto createDto = CreateStudentDto.builder()
                .name("Pedro Ramírez")
                .email("pedro.ramirez@universidad.edu")
                .password("securePass123")
                .major("Ingeniería en Sistemas")
                .build();

        mockMvc.perform(post("/api/admin/students")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pedro Ramírez"))
                .andExpect(jsonPath("$.email").value("pedro.ramirez@universidad.edu"))
                .andExpect(jsonPath("$.major").value("Ingeniería en Sistemas"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Alta de alumno con email duplicado - debe fallar")
    void testCreateStudentDuplicateEmail() throws Exception {
        CreateStudentDto createDto = CreateStudentDto.builder()
                .name("Otro Estudiante")
                .email("maria.gonzalez@universidad.edu") // Email existente
                .password("password123")
                .major("Ingeniería en Sistemas")
                .build();

        mockMvc.perform(post("/api/admin/students")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El email ya está registrado"));
    }

    @Test
    @DisplayName("Baja de alumno exitosa")
    void testDeleteStudent() throws Exception {
        mockMvc.perform(delete("/api/admin/students/{id}", existingStudent.getId())
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("eliminado exitosamente")));
    }

    @Test
    @DisplayName("Baja de alumno con inscripciones activas - debe fallar")
    void testDeleteStudentWithEnrollments() throws Exception {
        // Crear inscripción activa
        Enrollment enrollment = Enrollment.builder()
                .student(existingStudent)
                .courseGroup(existingGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        enrollmentRepository.save(enrollment);

        mockMvc.perform(delete("/api/admin/students/{id}", existingStudent.getId())
                        .session(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("inscripciones activas")));
    }

    // ========== TESTS DE GESTIÓN DE PROFESORES ==========

    @Test
    @DisplayName("Listar profesores con paginación")
    void testGetAllTeachers() throws Exception {
        mockMvc.perform(get("/api/admin/teachers")
                        .session(adminSession)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Dr. Carlos Pérez"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Alta de profesor exitosa")
    void testCreateTeacher() throws Exception {
        CreateTeacherDto createDto = CreateTeacherDto.builder()
                .name("Dra. Laura Sánchez")
                .email("laura.sanchez@universidad.edu")
                .password("teacherPass123")
                .build();

        mockMvc.perform(post("/api/admin/teachers")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dra. Laura Sánchez"))
                .andExpect(jsonPath("$.email").value("laura.sanchez@universidad.edu"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Baja de profesor exitosa")
    void testDeleteTeacher() throws Exception {
        // Crear profesor sin grupos
        Teacher teacherToDelete = Teacher.builder()
                .name("Prof. Temporal")
                .email("temporal@universidad.edu")
                .password(passwordEncoder.encode("password123"))
                .build();
        teacherToDelete = teacherRepository.save(teacherToDelete);

        mockMvc.perform(delete("/api/admin/teachers/{id}", teacherToDelete.getId())
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("eliminado exitosamente")));
    }

    // ========== TESTS DE GESTIÓN DE ASIGNATURAS ==========

    @Test
    @DisplayName("Listar asignaturas")
    void testGetAllSubjects() throws Exception {
        mockMvc.perform(get("/api/admin/subjects")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Cálculo I"))
                .andExpect(jsonPath("$[0].major").value("Ingeniería en Sistemas"))
                .andExpect(jsonPath("$[0].courseYear").value(1));
    }

    @Test
    @DisplayName("Crear asignatura exitosamente")
    void testCreateSubject() throws Exception {
        CreateSubjectDto createDto = CreateSubjectDto.builder()
                .name("Física I")
                .major("Ingeniería en Sistemas")
                .courseYear(1)
                .build();

        mockMvc.perform(post("/api/admin/subjects")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Física I"))
                .andExpect(jsonPath("$.major").value("Ingeniería en Sistemas"))
                .andExpect(jsonPath("$.courseYear").value(1));
    }

    @Test
    @DisplayName("Actualizar asignatura exitosamente")
    void testUpdateSubject() throws Exception {
        UpdateSubjectDto updateDto = UpdateSubjectDto.builder()
                .name("Cálculo Diferencial e Integral")
                .build();

        mockMvc.perform(put("/api/admin/subjects/{id}", existingSubject.getId())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cálculo Diferencial e Integral"))
                .andExpect(jsonPath("$.major").value("Ingeniería en Sistemas"))
                .andExpect(jsonPath("$.courseYear").value(1));
    }

    @Test
    @DisplayName("Eliminar asignatura sin grupos")
    void testDeleteSubject() throws Exception {
        // Crear asignatura sin grupos
        Subject subjectToDelete = Subject.builder()
                .name("Materia Temporal")
                .major("Ingeniería en Sistemas")
                .courseYear(4)
                .build();
        subjectToDelete = subjectRepository.save(subjectToDelete);

        mockMvc.perform(delete("/api/admin/subjects/{id}", subjectToDelete.getId())
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("eliminada exitosamente")));
    }

    @Test
    @DisplayName("Eliminar asignatura con grupos - debe fallar")
    void testDeleteSubjectWithGroups() throws Exception {
        mockMvc.perform(delete("/api/admin/subjects/{id}", existingSubject.getId())
                        .session(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("tiene grupos asociados")));
    }

    // ========== TESTS DE GESTIÓN DE GRUPOS ==========

    @Test
    @DisplayName("Listar todos los grupos")
    void testGetAllGroups() throws Exception {
        mockMvc.perform(get("/api/admin/groups")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subjectName").value("Cálculo I"))
                .andExpect(jsonPath("$[0].teacherName").value("Dr. Carlos Pérez"))
                .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    @DisplayName("Crear grupo con sesiones")
    void testCreateGroupWithSessions() throws Exception {
        List<CreateGroupSessionDto> sessions = new ArrayList<>();
        sessions.add(CreateGroupSessionDto.builder()
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build());
        sessions.add(CreateGroupSessionDto.builder()
                .dayOfWeek("WEDNESDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build());

        CreateCourseGroupDto createDto = CreateCourseGroupDto.builder()
                .subjectId(existingSubject.getId())
                .teacherId(existingTeacher.getId())
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("250.00"))
                .sessions(sessions)
                .build();

        mockMvc.perform(post("/api/admin/groups")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectName").value("Cálculo I"))
                .andExpect(jsonPath("$.teacherName").value("Dr. Carlos Pérez"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.type").value("REGULAR"))
                .andExpect(jsonPath("$.price").value(250.00))
                .andExpect(jsonPath("$.maxCapacity").value(35))
                .andExpect(jsonPath("$.sessions", hasSize(2)));
    }

    @Test
    @DisplayName("Cambiar estado de grupo - PLANNED a ACTIVE")
    void testUpdateGroupStatus() throws Exception {
        UpdateGroupStatusDto statusDto = UpdateGroupStatusDto.builder()
                .status(CourseGroupStatus.ACTIVE)
                .reason("Grupo listo para iniciar clases")
                .build();

        mockMvc.perform(put("/api/admin/groups/{id}/status", existingGroup.getId())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").value(existingGroup.getId()));
    }

    @Test
    @DisplayName("Cambiar estado de grupo - ACTIVE a CLOSED")
    void testCloseActiveGroup() throws Exception {
        // Primero activar el grupo
        existingGroup.setStatus(CourseGroupStatus.ACTIVE);
        courseGroupRepository.save(existingGroup);

        UpdateGroupStatusDto statusDto = UpdateGroupStatusDto.builder()
                .status(CourseGroupStatus.CLOSED)
                .reason("Semestre finalizado")
                .build();

        mockMvc.perform(put("/api/admin/groups/{id}/status", existingGroup.getId())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("Eliminar grupo sin inscripciones")
    void testDeleteGroup() throws Exception {
        mockMvc.perform(delete("/api/admin/groups/{id}", existingGroup.getId())
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("eliminado exitosamente")));
    }

    @Test
    @DisplayName("Eliminar grupo con inscripciones - debe fallar")
    void testDeleteGroupWithEnrollments() throws Exception {
        // Crear inscripción
        Enrollment enrollment = Enrollment.builder()
                .student(existingStudent)
                .courseGroup(existingGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        enrollmentRepository.save(enrollment);

        mockMvc.perform(delete("/api/admin/groups/{id}", existingGroup.getId())
                        .session(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("tiene estudiantes inscritos")));
    }

    // ========== TESTS DE GESTIÓN DE SOLICITUDES DE GRUPOS ==========

    @Test
    @DisplayName("Listar solicitudes de grupos pendientes")
    void testGetPendingGroupRequests() throws Exception {
        // Crear solicitud de grupo
        GroupRequest request = GroupRequest.builder()
                .student(existingStudent)
                .subject(existingSubject)
                .status(RequestStatus.PENDING)
                .build();
        groupRequestRepository.save(request);

        mockMvc.perform(get("/api/admin/group-requests")
                        .session(adminSession)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentName").value("María González"))
                .andExpect(jsonPath("$[0].subjectName").value("Cálculo I"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].comments").value("Necesito cursar esta materia"));
    }

    @Test
    @DisplayName("Aprobar solicitud de grupo")
    void testApproveGroupRequest() throws Exception {
        // Crear solicitud
        GroupRequest request = GroupRequest.builder()
                .student(existingStudent)
                .subject(existingSubject)
                .status(RequestStatus.PENDING)
                .build();
        request = groupRequestRepository.save(request);

        UpdateRequestStatusDto updateDto = UpdateRequestStatusDto.builder()
                .status(RequestStatus.APPROVED)
                .adminComments("Se creará un nuevo grupo para el próximo semestre")
                .build();

        mockMvc.perform(put("/api/admin/group-requests/{id}", request.getId())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.adminComments").value("Se creará un nuevo grupo para el próximo semestre"));
    }

    @Test
    @DisplayName("Rechazar solicitud de grupo")
    void testRejectGroupRequest() throws Exception {
        // Crear solicitud
        GroupRequest request = GroupRequest.builder()
                .student(existingStudent)
                .subject(existingSubject)
                .status(RequestStatus.PENDING)
                .build();
        request = groupRequestRepository.save(request);

        UpdateRequestStatusDto updateDto = UpdateRequestStatusDto.builder()
                .status(RequestStatus.REJECTED)
                .adminComments("No hay suficientes estudiantes interesados")
                .build();

        mockMvc.perform(put("/api/admin/group-requests/{id}", request.getId())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.adminComments").value("No hay suficientes estudiantes interesados"));
    }

    // ========== TESTS DE SEGURIDAD Y AUTORIZACIÓN ==========

    @Test
    @DisplayName("Acceder a endpoints admin sin autenticación - debe fallar")
    void testAdminEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/admin/students"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/teachers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/subjects"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/groups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Estudiante intentando acceder a endpoints admin - debe fallar")
    void testStudentAccessingAdminEndpoints() throws Exception {
        // Crear sesión de estudiante
        MockHttpSession studentSession = new MockHttpSession();

        LoginRequestDto studentLogin = LoginRequestDto.builder()
                .email("maria.gonzalez@universidad.edu")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentLogin)))
                .andExpect(status().isOk());

        // Intentar acceder a endpoints admin
        mockMvc.perform(get("/api/admin/students")
                        .session(studentSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/students")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Profesor intentando acceder a endpoints admin - debe fallar")
    void testTeacherAccessingAdminEndpoints() throws Exception {
        // Crear sesión de profesor
        MockHttpSession teacherSession = new MockHttpSession();

        LoginRequestDto teacherLogin = LoginRequestDto.builder()
                .email("carlos.perez@universidad.edu")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .session(teacherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teacherLogin)))
                .andExpect(status().isOk());

        // Intentar acceder a endpoints admin
        mockMvc.perform(get("/api/admin/students")
                        .session(teacherSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/students/1")
                        .session(teacherSession))
                .andExpect(status().isForbidden());
    }

    // ========== TESTS DE VALIDACIONES ==========

    @Test
    @DisplayName("Crear estudiante con datos inválidos")
    void testCreateStudentValidation() throws Exception {
        // Email inválido
        CreateStudentDto invalidDto = CreateStudentDto.builder()
                .name("Test Student")
                .email("invalid-email")
                .password("password123")
                .major("Ingeniería")
                .build();

        mockMvc.perform(post("/api/admin/students")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Contraseña muy corta
        invalidDto.setEmail("valid@email.com");
        invalidDto.setPassword("short");

        mockMvc.perform(post("/api/admin/students")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Crear asignatura con año de curso inválido")
    void testCreateSubjectWithInvalidYear() throws Exception {
        CreateSubjectDto invalidDto = CreateSubjectDto.builder()
                .name("Materia Test")
                .major("Ingeniería")
                .courseYear(7) // Máximo es 6
                .build();

        mockMvc.perform(post("/api/admin/subjects")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Crear grupo con precio negativo")
    void testCreateGroupWithNegativePrice() throws Exception {
        CreateCourseGroupDto invalidDto = CreateCourseGroupDto.builder()
                .subjectId(existingSubject.getId())
                .teacherId(existingTeacher.getId())
                .type(CourseGroupType.REGULAR)
                .price(new BigDecimal("-100.00")) // Precio negativo
                .sessions(new ArrayList<>())
                .build();

        mockMvc.perform(post("/api/admin/groups")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
