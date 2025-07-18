package com.acainfo.mvp.model;

import com.acainfo.mvp.model.enums.PaymentStatus;
import com.acainfo.mvp.model.enums.RequestStatus;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de mapeo JPA para la entidad Student.
 * Valida constraints, atributos, relaciones y comportamiento de persistencia.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Student Entity Mapping Tests")
@Transactional
class StudentMappingTest {

    @Autowired
    private EntityManager em;

    private Student validStudent;
    private Subject testSubject;
    private Teacher testTeacher;
    private CourseGroup testCourseGroup;

    @BeforeEach
    void setUp() {
        // Crear un student válido
        validStudent = Student.builder()
                .name("Juan Pérez García")
                .email("juan.perez@universidad.edu")
                .password("encodedPassword123")
                .major("Ingeniería Informática")
                .enrollments(new HashSet<>())
                .groupRequests(new HashSet<>())
                .build();

        // Crear entidades relacionadas para tests de relaciones
        testSubject = Subject.builder()
                .name("Programación I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();

        testTeacher = Teacher.builder()
                .name("Dr. López")
                .email("lopez@universidad.edu")
                .password("password123")
                .build();

        testCourseGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .price(BigDecimal.valueOf(150.00))
                .maxCapacity(30)
                .build();
    }

    @Test
    @DisplayName("Debe persistir un Student válido con todos los campos")
    void shouldPersistValidStudent() {
        // When
        em.persist(validStudent);
        em.flush();

        // Then
        assertThat(validStudent.getId()).isNotNull();
        assertThat(validStudent.getCreatedAt()).isNotNull();
        assertThat(validStudent.getUpdatedAt()).isNull();

        // Verificar que se puede recuperar
        Student found = em.find(Student.class, validStudent.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Juan Pérez García");
        assertThat(found.getEmail()).isEqualTo("juan.perez@universidad.edu");
        assertThat(found.getPassword()).isEqualTo("encodedPassword123");
        assertThat(found.getMajor()).isEqualTo("Ingeniería Informática");
    }

    @Test
    @DisplayName("Debe asignar automáticamente ID y createdAt")
    void shouldAutoAssignIdAndCreatedAt() {
        // Given
        assertThat(validStudent.getId()).isNull();
        assertThat(validStudent.getCreatedAt()).isNull();

        // When
        em.persist(validStudent);
        em.flush();

        // Then
        assertThat(validStudent.getId()).isNotNull();
        assertThat(validStudent.getCreatedAt()).isNotNull();
        assertThat(validStudent.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Debe actualizar updatedAt en modificaciones")
    void shouldUpdateUpdatedAtOnModification() {
        // Given
        em.persist(validStudent);
        em.flush();
        assertThat(validStudent.getUpdatedAt()).isNull();

        // When
        validStudent.setName("Juan Pérez García Actualizado");
        em.flush();

        // Then
        assertThat(validStudent.getUpdatedAt()).isNotNull();
        assertThat(validStudent.getUpdatedAt()).isAfter(validStudent.getCreatedAt());
    }

    @Test
    @DisplayName("Debe fallar cuando name es null")
    void shouldFailWhenNameIsNull() {
        // Given
        validStudent.setName(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name is required");
    }

    @Test
    @DisplayName("Debe fallar cuando name está vacío")
    void shouldFailWhenNameIsBlank() {
        // Given
        validStudent.setName("   ");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name is required");
    }

    @Test
    @DisplayName("Debe fallar cuando name excede 100 caracteres")
    void shouldFailWhenNameExceedsMaxLength() {
        // Given
        String longName = "A".repeat(101);
        validStudent.setName(longName);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name must not exceed 100 characters");
    }

    @Test
    @DisplayName("Debe fallar cuando email es null")
    void shouldFailWhenEmailIsNull() {
        // Given
        validStudent.setEmail(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    @DisplayName("Debe fallar cuando email es inválido")
    void shouldFailWhenEmailIsInvalid() {
        // Given
        validStudent.setEmail("not-an-email");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Email must be valid");
    }

    @Test
    @DisplayName("Debe fallar cuando password es null")
    void shouldFailWhenPasswordIsNull() {
        // Given
        validStudent.setPassword(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    @DisplayName("Debe fallar cuando password es menor a 8 caracteres")
    void shouldFailWhenPasswordIsTooShort() {
        // Given
        validStudent.setPassword("1234567");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Password must be at least 8 characters long");
    }

    @Test
    @DisplayName("Debe fallar cuando major es null")
    void shouldFailWhenMajorIsNull() {
        // Given
        validStudent.setMajor(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Major is required");
    }

    @Test
    @DisplayName("Debe fallar cuando major está vacío")
    void shouldFailWhenMajorIsBlank() {
        // Given
        validStudent.setMajor("   ");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Major is required");
    }

    @Test
    @DisplayName("Debe fallar cuando major excede 100 caracteres")
    void shouldFailWhenMajorExceedsMaxLength() {
        // Given
        String longMajor = "A".repeat(101);
        validStudent.setMajor(longMajor);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validStudent);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Major must not exceed 100 characters");
    }

    @Test
    @DisplayName("Debe respetar constraint único de email")
    void shouldEnforceUniqueEmailConstraint() {
        // Given
        em.persist(validStudent);
        em.flush();

        Student duplicateEmailStudent = Student.builder()
                .name("Otro Estudiante")
                .email("juan.perez@universidad.edu") // mismo email
                .password("otroPassword123")
                .major("Otra Carrera")
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(duplicateEmailStudent);
            em.flush();
        }).satisfies(throwable -> {
            String message = throwable.getMessage();
            assertThat(message.toLowerCase()).contains("unique");
        });
    }

    @Test
    @DisplayName("Debe permitir múltiples students con emails diferentes")
    void shouldAllowMultipleStudentsWithDifferentEmails() {
        // Given
        Student student1 = Student.builder()
                .name("Estudiante 1")
                .email("estudiante1@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();

        Student student2 = Student.builder()
                .name("Estudiante 2")
                .email("estudiante2@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();

        // When
        em.persist(student1);
        em.persist(student2);
        em.flush();

        // Then
        assertThat(student1.getId()).isNotNull();
        assertThat(student2.getId()).isNotNull();
        assertThat(student1.getId()).isNotEqualTo(student2.getId());
    }

    @Test
    @DisplayName("Debe inicializar enrollments y groupRequests como conjuntos vacíos")
    void shouldInitializeCollectionsAsEmptySets() {
        // When
        em.persist(validStudent);
        em.flush();
        em.clear();

        // Then
        Student found = em.find(Student.class, validStudent.getId());
        assertThat(found.getEnrollments()).isNotNull();
        assertThat(found.getEnrollments()).isEmpty();
        assertThat(found.getGroupRequests()).isNotNull();
        assertThat(found.getGroupRequests()).isEmpty();
    }

    @Test
    @DisplayName("Debe gestionar relación bidireccional con Enrollment")
    void shouldManageBidirectionalRelationshipWithEnrollment() {
        // Given
        em.persist(validStudent);
        em.persist(testSubject);
        em.persist(testTeacher);
        em.persist(testCourseGroup);
        em.flush();

        // When - Crear enrollment usando helper method
        Enrollment enrollment = Enrollment.builder()
                .courseGroup(testCourseGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        validStudent.addEnrollment(enrollment);
        em.persist(enrollment);
        em.flush();
        em.clear();

        // Then
        Student foundStudent = em.find(Student.class, validStudent.getId());
        assertThat(foundStudent.getEnrollments()).hasSize(1);

        Enrollment foundEnrollment = foundStudent.getEnrollments().iterator().next();
        assertThat(foundEnrollment.getStudent()).isEqualTo(foundStudent);
        assertThat(foundEnrollment.getCourseGroup().getId()).isEqualTo(testCourseGroup.getId());
    }

    @Test
    @DisplayName("Debe gestionar relación bidireccional con GroupRequest")
    void shouldManageBidirectionalRelationshipWithGroupRequest() {
        // Given
        em.persist(validStudent);
        em.persist(testSubject);
        em.flush();

        // When - Crear group request usando helper method
        GroupRequest request = GroupRequest.builder()
                .subject(testSubject)
                .status(RequestStatus.PENDING)
                .build();

        validStudent.addGroupRequest(request);
        em.persist(request);
        em.flush();
        em.clear();

        // Then
        Student foundStudent = em.find(Student.class, validStudent.getId());
        assertThat(foundStudent.getGroupRequests()).hasSize(1);

        GroupRequest foundRequest = foundStudent.getGroupRequests().iterator().next();
        assertThat(foundRequest.getStudent()).isEqualTo(foundStudent);
        assertThat(foundRequest.getSubject().getId()).isEqualTo(testSubject.getId());
    }

    @Test
    @DisplayName("Debe eliminar enrollments en cascada")
    @Transactional
    void shouldCascadeDeleteEnrollments() {
        // Given
        em.persist(validStudent);
        em.persist(testSubject);
        em.persist(testTeacher);
        em.persist(testCourseGroup);

        Enrollment enrollment = Enrollment.builder()
                .courseGroup(testCourseGroup)
                .build();
        validStudent.addEnrollment(enrollment);
        em.persist(enrollment);
        em.flush();

        Long enrollmentId = enrollment.getId();
        assertThat(em.find(Enrollment.class, enrollmentId)).isNotNull();

        // When
        em.remove(validStudent);
        em.flush();

        // Then
        assertThat(em.find(Student.class, validStudent.getId())).isNull();
        assertThat(em.find(Enrollment.class, enrollmentId)).isNull();
        // El grupo debe seguir existiendo
        assertThat(em.find(CourseGroup.class, testCourseGroup.getId())).isNotNull();
    }

    @Test
    @DisplayName("Debe eliminar groupRequests en cascada")
    @Transactional
    void shouldCascadeDeleteGroupRequests() {
        // Given
        em.persist(validStudent);
        em.persist(testSubject);

        GroupRequest request = GroupRequest.builder()
                .subject(testSubject)
                .build();
        validStudent.addGroupRequest(request);
        em.persist(request);
        em.flush();

        Long requestId = request.getId();
        assertThat(em.find(GroupRequest.class, requestId)).isNotNull();

        // When
        em.remove(validStudent);
        em.flush();

        // Then
        assertThat(em.find(Student.class, validStudent.getId())).isNull();
        assertThat(em.find(GroupRequest.class, requestId)).isNull();
        // La asignatura debe seguir existiendo
        assertThat(em.find(Subject.class, testSubject.getId())).isNotNull();
    }

    @Test
    @DisplayName("Debe verificar equals y hashCode basados en ID")
    @Transactional
    void shouldImplementEqualsAndHashCodeBasedOnId() {
        // Given
        Student student1 = Student.builder()
                .name("Estudiante 1")
                .email("estudiante1@universidad.edu")
                .password("password123")
                .major("Ingeniería")
                .build();

        Student student2 = Student.builder()
                .name("Estudiante 2")
                .email("estudiante2@universidad.edu")
                .password("password456")
                .major("Medicina")
                .build();

        // Before persistence
        assertThat(student1).isNotEqualTo(student2);

        // Persist both
        em.persist(student1);
        em.persist(student2);
        em.flush();

        // Con IDs asignados
        assertThat(student1).isNotEqualTo(student2);
        assertThat(student1.getId()).isNotEqualTo(student2.getId());

        // Buscar el mismo student
        Student sameStudent = em.find(Student.class, student1.getId());
        assertThat(student1).isEqualTo(sameStudent);

        // Verificar comportamiento con null
        assertThat(student1).isNotEqualTo(null);
        assertThat(student1).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("Debe verificar que password no se incluye en toString")
    void shouldNotIncludePasswordInToString() {
        // When
        String toString = validStudent.toString();

        // Then
        assertThat(toString).doesNotContain("password");
        assertThat(toString).doesNotContain("encodedPassword123");
        assertThat(toString).contains("Student");
        assertThat(toString).contains("name=");
        assertThat(toString).contains("email=");
        assertThat(toString).contains("major=");
    }

    @Test
    @DisplayName("Debe permitir caracteres especiales válidos en campos de texto")
    void shouldAllowSpecialCharactersInTextFields() {
        // Given
        validStudent.setName("María José García-López Ñúñez");
        validStudent.setMajor("Ingeniería en Computación y Sistemas");

        // When
        em.persist(validStudent);
        em.flush();

        // Then
        Student found = em.find(Student.class, validStudent.getId());
        assertThat(found.getName()).isEqualTo("María José García-López Ñúñez");
        assertThat(found.getMajor()).isEqualTo("Ingeniería en Computación y Sistemas");
    }

    @Test
    @DisplayName("Debe gestionar correctamente removeEnrollment")
    void shouldCorrectlyRemoveEnrollment() {
        // Given
        em.persist(validStudent);
        em.persist(testSubject);
        em.persist(testTeacher);
        em.persist(testCourseGroup);

        Enrollment enrollment = Enrollment.builder()
                .courseGroup(testCourseGroup)
                .build();

        validStudent.addEnrollment(enrollment);
        em.persist(enrollment);
        em.flush();

        assertThat(validStudent.getEnrollments()).hasSize(1);
        assertThat(enrollment.getStudent()).isEqualTo(validStudent);

        // When
        validStudent.removeEnrollment(enrollment);
        em.remove(enrollment);
        em.flush();

        // Then
        assertThat(validStudent.getEnrollments()).isEmpty();
        assertThat(enrollment.getStudent()).isNull();
    }

    @Test
    @DisplayName("Debe gestionar correctamente removeGroupRequest")
    @Transactional
    void shouldCorrectlyRemoveGroupRequest() {
        // Given
        em.persist(validStudent);
        em.persist(testSubject);

        GroupRequest request = GroupRequest.builder()
                .subject(testSubject)
                .build();

        validStudent.addGroupRequest(request);
        em.persist(request);
        em.flush();

        assertThat(validStudent.getGroupRequests()).hasSize(1);
        assertThat(request.getStudent()).isEqualTo(validStudent);

        // When
        validStudent.removeGroupRequest(request);
        em.remove(request);
        em.flush();

        // Then
        assertThat(validStudent.getGroupRequests()).isEmpty();
        assertThat(request.getStudent()).isNull();
    }
}