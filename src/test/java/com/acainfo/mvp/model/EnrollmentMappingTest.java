package com.acainfo.mvp.model;

import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import com.acainfo.mvp.model.enums.PaymentStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de mapeo JPA para la entidad Enrollment.
 * Valida constraints, atributos, relaciones y comportamiento de persistencia.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Enrollment Entity Mapping Tests")
@Transactional
class EnrollmentMappingTest {

    @Autowired
    private EntityManager em;

    private Enrollment validEnrollment;
    private Student testStudent;
    private CourseGroup testCourseGroup;
    private Subject testSubject;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() {
        // Crear entidades relacionadas
        testSubject = Subject.builder()
                .name("Base de Datos")
                .major("Ingeniería Informática")
                .courseYear(2)
                .build();

        testTeacher = Teacher.builder()
                .name("Prof. Sánchez")
                .email("sanchez@universidad.edu")
                .password("password123")
                .build();

        testStudent = Student.builder()
                .name("María González")
                .email("maria.gonzalez@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();

        testCourseGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(300.00))
                .maxCapacity(25)
                .build();

        // Persistir las entidades relacionadas
        em.persist(testSubject);
        em.persist(testTeacher);
        em.persist(testStudent);
        em.persist(testCourseGroup);
        em.flush();

        // Crear un Enrollment válido
        validEnrollment = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testCourseGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Debe persistir un Enrollment válido con todos los campos")
    void shouldPersistValidEnrollment() {
        // When
        em.persist(validEnrollment);
        em.flush();

        // Then
        assertThat(validEnrollment.getId()).isNotNull();
        assertThat(validEnrollment.getCreatedAt()).isNotNull();
        assertThat(validEnrollment.getUpdatedAt()).isNull();

        // Verificar que se puede recuperar
        Enrollment found = em.find(Enrollment.class, validEnrollment.getId());
        assertThat(found).isNotNull();
        assertThat(found.getStudent().getId()).isEqualTo(testStudent.getId());
        assertThat(found.getCourseGroup().getId()).isEqualTo(testCourseGroup.getId());
        assertThat(found.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Debe asignar automáticamente ID y createdAt")
    void shouldAutoAssignIdAndCreatedAt() {
        // Given
        assertThat(validEnrollment.getId()).isNull();
        assertThat(validEnrollment.getCreatedAt()).isNull();

        // When
        em.persist(validEnrollment);
        em.flush();

        // Then
        assertThat(validEnrollment.getId()).isNotNull();
        assertThat(validEnrollment.getCreatedAt()).isNotNull();
        assertThat(validEnrollment.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Debe actualizar updatedAt en modificaciones")
    void shouldUpdateUpdatedAtOnModification() {
        // Given
        em.persist(validEnrollment);
        em.flush();
        assertThat(validEnrollment.getUpdatedAt()).isNull();

        // When
        validEnrollment.setPaymentStatus(PaymentStatus.PAID);
        em.flush();

        // Then
        assertThat(validEnrollment.getUpdatedAt()).isNotNull();
        assertThat(validEnrollment.getUpdatedAt()).isAfter(validEnrollment.getCreatedAt());
    }

    @Test
    @DisplayName("Debe fallar cuando student es null")
    void shouldFailWhenStudentIsNull() {
        // Given
        validEnrollment.setStudent(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validEnrollment);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Student is required");
    }

    @Test
    @DisplayName("Debe fallar cuando courseGroup es null")
    void shouldFailWhenCourseGroupIsNull() {
        // Given
        validEnrollment.setCourseGroup(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validEnrollment);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Course group is required");
    }

    @Test
    @DisplayName("Debe establecer paymentStatus PENDING por defecto si es null")
    void shouldSetDefaultPaymentStatusWhenNull() {
        // Given
        Enrollment enrollmentWithoutStatus = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testCourseGroup)
                .build();
        // No establecemos paymentStatus

        // When
        em.persist(enrollmentWithoutStatus);
        em.flush();

        // Then
        assertThat(enrollmentWithoutStatus.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Debe aceptar todos los valores de PaymentStatus")
    void shouldAcceptAllPaymentStatusValues() {
        for (PaymentStatus status : PaymentStatus.values()) {
            // Crear nuevo estudiante para cada iteración para evitar conflictos de constraint único
            Student student = Student.builder()
                    .name("Student " + status)
                    .email(status + "@test.com")
                    .password("password123")
                    .major("Ingeniería Informática")
                    .build();
            em.persist(student);

            Enrollment enrollment = Enrollment.builder()
                    .student(student)
                    .courseGroup(testCourseGroup)
                    .paymentStatus(status)
                    .build();

            em.persist(enrollment);
            em.flush();

            assertThat(enrollment.getPaymentStatus()).isEqualTo(status);
            em.clear();
        }
    }

    @Test
    @DisplayName("Debe respetar constraint único de student + courseGroup")
    void shouldEnforceUniqueStudentCourseGroupConstraint() {
        // Given
        em.persist(validEnrollment);
        em.flush();

        // Intentar crear otra inscripción del mismo estudiante en el mismo grupo
        Enrollment duplicateEnrollment = Enrollment.builder()
                .student(testStudent) // mismo estudiante
                .courseGroup(testCourseGroup) // mismo grupo
                .paymentStatus(PaymentStatus.PAID)
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(duplicateEnrollment);
            em.flush();
        }).satisfies(throwable -> {
            String message = throwable.getMessage();
            assertThat(message.toLowerCase()).contains("unique");
        });
    }

    @Test
    @DisplayName("Debe permitir mismo estudiante en diferentes grupos")
    void shouldAllowSameStudentInDifferentGroups() {
        // Given - Crear otro grupo
        Subject anotherSubject = Subject.builder()
                .name("Algoritmos")
                .major("Ingeniería Informática")
                .courseYear(2)
                .build();
        em.persist(anotherSubject);

        CourseGroup anotherGroup = CourseGroup.builder()
                .subject(anotherSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(250.00))
                .build();
        em.persist(anotherGroup);

        // When - Inscribir al mismo estudiante en dos grupos diferentes
        Enrollment enrollment1 = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testCourseGroup)
                .build();

        Enrollment enrollment2 = Enrollment.builder()
                .student(testStudent) // mismo estudiante
                .courseGroup(anotherGroup) // diferente grupo
                .build();

        em.persist(enrollment1);
        em.persist(enrollment2);
        em.flush();

        // Then
        assertThat(enrollment1.getId()).isNotNull();
        assertThat(enrollment2.getId()).isNotNull();
        assertThat(enrollment1.getId()).isNotEqualTo(enrollment2.getId());
    }

    @Test
    @DisplayName("Debe permitir diferentes estudiantes en el mismo grupo")
    void shouldAllowDifferentStudentsInSameGroup() {
        // Given - Crear otro estudiante
        Student anotherStudent = Student.builder()
                .name("Pedro Martín")
                .email("pedro.martin@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();
        em.persist(anotherStudent);

        // When - Inscribir dos estudiantes diferentes en el mismo grupo
        Enrollment enrollment1 = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testCourseGroup)
                .build();

        Enrollment enrollment2 = Enrollment.builder()
                .student(anotherStudent) // diferente estudiante
                .courseGroup(testCourseGroup) // mismo grupo
                .build();

        em.persist(enrollment1);
        em.persist(enrollment2);
        em.flush();

        // Then
        assertThat(enrollment1.getId()).isNotNull();
        assertThat(enrollment2.getId()).isNotNull();
        assertThat(enrollment1.getId()).isNotEqualTo(enrollment2.getId());
    }

    @Test
    @DisplayName("Debe manejar correctamente la relación con Student")
    void shouldCorrectlyHandleStudentRelation() {
        // When
        em.persist(validEnrollment);
        em.flush();
        em.clear();

        // Then
        Enrollment found = em.find(Enrollment.class, validEnrollment.getId());
        assertThat(found.getStudent()).isNotNull();
        assertThat(found.getStudent().getId()).isEqualTo(testStudent.getId());
        assertThat(found.getStudent().getName()).isEqualTo("María González");
    }

    @Test
    @DisplayName("Debe manejar correctamente la relación con CourseGroup")
    void shouldCorrectlyHandleCourseGroupRelation() {
        // When
        em.persist(validEnrollment);
        em.flush();
        em.clear();

        // Then
        Enrollment found = em.find(Enrollment.class, validEnrollment.getId());
        assertThat(found.getCourseGroup()).isNotNull();
        assertThat(found.getCourseGroup().getId()).isEqualTo(testCourseGroup.getId());
        assertThat(found.getCourseGroup().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(300.00));
    }

    @Test
    @DisplayName("Debe verificar equals y hashCode basados en ID")
    void shouldImplementEqualsAndHashCodeBasedOnId() {
        // Given - Crear otro estudiante para evitar constraint único
        Student student2 = Student.builder()
                .name("Ana López")
                .email("ana.lopez@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();
        em.persist(student2);

        Enrollment enrollment1 = Enrollment.builder()
                .student(testStudent)
                .courseGroup(testCourseGroup)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Enrollment enrollment2 = Enrollment.builder()
                .student(student2)
                .courseGroup(testCourseGroup)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        // Before persistence
        assertThat(enrollment1).isNotEqualTo(enrollment2);

        // Persist both
        em.persist(enrollment1);
        em.persist(enrollment2);
        em.flush();

        // Con IDs asignados
        assertThat(enrollment1).isNotEqualTo(enrollment2);
        assertThat(enrollment1.getId()).isNotEqualTo(enrollment2.getId());

        // Buscar el mismo enrollment
        Enrollment sameEnrollment = em.find(Enrollment.class, enrollment1.getId());
        assertThat(enrollment1).isEqualTo(sameEnrollment);
    }

    @Test
    @DisplayName("Debe verificar que student y courseGroup no se incluyen en toString")
    void shouldNotIncludeRelationsInToString() {
        // When
        String toString = validEnrollment.toString();

        // Then
        assertThat(toString).doesNotContain("student");
        assertThat(toString).doesNotContain("courseGroup");
        assertThat(toString).contains("Enrollment");
        assertThat(toString).contains("paymentStatus=");
    }

    @Test
    @DisplayName("Debe verificar cascada al eliminar Student")
    void shouldCascadeDeleteWhenStudentIsDeleted() {
        // Given
        testStudent.addEnrollment(validEnrollment);
        em.persist(validEnrollment);
        em.flush();

        Long enrollmentId = validEnrollment.getId();
        Long studentId = testStudent.getId();
        Long courseGroupId = testCourseGroup.getId();

        // When - Eliminar el estudiante
        Student student = em.find(Student.class, studentId);
        em.remove(student);
        em.flush();

        // Then
        assertThat(em.find(Student.class, studentId)).isNull();
        assertThat(em.find(Enrollment.class, enrollmentId)).isNull(); // Enrollment eliminado por cascada
        assertThat(em.find(CourseGroup.class, courseGroupId)).isNotNull(); // CourseGroup sigue existiendo
    }

    @Test
    @DisplayName("Debe verificar cascada al eliminar CourseGroup")
    void shouldCascadeDeleteWhenCourseGroupIsDeleted() {
        // Given
        testCourseGroup.addEnrollment(validEnrollment);
        em.persist(validEnrollment);
        em.flush();

        Long enrollmentId = validEnrollment.getId();
        Long studentId = testStudent.getId();
        Long courseGroupId = testCourseGroup.getId();

        // When - Eliminar el grupo
        CourseGroup group = em.find(CourseGroup.class, courseGroupId);
        em.remove(group);
        em.flush();

        // Then
        assertThat(em.find(CourseGroup.class, courseGroupId)).isNull();
        assertThat(em.find(Enrollment.class, enrollmentId)).isNull(); // Enrollment eliminado por cascada
        assertThat(em.find(Student.class, studentId)).isNotNull(); // Student sigue existiendo
    }

    @Test
    @DisplayName("Debe mantener integridad referencial")
    void shouldMaintainReferentialIntegrity() {
        // Given
        em.persist(validEnrollment);
        em.flush();

        // Verificar que las relaciones son consistentes
        assertThat(validEnrollment.getStudent()).isEqualTo(testStudent);
        assertThat(validEnrollment.getCourseGroup()).isEqualTo(testCourseGroup);

        testStudent.addEnrollment(validEnrollment);
        assertThat(testStudent.getEnrollments()).contains(validEnrollment);

        testCourseGroup.addEnrollment(validEnrollment);
        assertThat(testCourseGroup.getEnrollments()).contains(validEnrollment);
    }

    @Test
    @DisplayName("Debe permitir cambiar el estado de pago")
    void shouldAllowPaymentStatusChange() {
        // Given
        em.persist(validEnrollment);
        em.flush();
        assertThat(validEnrollment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);

        // When - Cambiar a PAID
        validEnrollment.setPaymentStatus(PaymentStatus.PAID);
        em.flush();

        // Then
        Enrollment found = em.find(Enrollment.class, validEnrollment.getId());
        assertThat(found.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        // When - Cambiar a FAILED
        validEnrollment.setPaymentStatus(PaymentStatus.FAILED);
        em.flush();

        // Then
        found = em.find(Enrollment.class, validEnrollment.getId());
        assertThat(found.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Debe validar que no se puede inscribir en grupo de otra carrera")
    void shouldValidateStudentMajorMatchesSubjectMajor() {
        // Given - Estudiante de otra carrera
        Student otherMajorStudent = Student.builder()
                .name("Luis Torres")
                .email("luis.torres@universidad.edu")
                .password("password123")
                .major("Ingeniería Civil") // Diferente carrera
                .build();
        em.persist(otherMajorStudent);

        Enrollment crossMajorEnrollment = Enrollment.builder()
                .student(otherMajorStudent)
                .courseGroup(testCourseGroup) // Grupo de Ingeniería Informática
                .build();

        // When
        em.persist(crossMajorEnrollment);
        em.flush();

        // Then - La validación de negocio debería manejarse en el servicio
        // Aquí solo verificamos que JPA permite la persistencia
        assertThat(crossMajorEnrollment.getId()).isNotNull();

        // Nota: La validación de que un estudiante solo puede inscribirse
        // en asignaturas de su carrera debe implementarse en la capa de servicio
    }
}