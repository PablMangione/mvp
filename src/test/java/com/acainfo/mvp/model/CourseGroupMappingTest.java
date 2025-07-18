package com.acainfo.mvp.model;

import com.acainfo.mvp.model.enums.*;
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
import java.time.LocalTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de mapeo JPA para la entidad CourseGroup.
 * Valida constraints, atributos, relaciones y comportamiento de persistencia.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CourseGroup Entity Mapping Tests")
@Transactional
class CourseGroupMappingTest {

    @Autowired
    private EntityManager em;

    private CourseGroup validCourseGroup;
    private Subject testSubject;
    private Teacher testTeacher;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Crear entidades relacionadas
        testSubject = Subject.builder()
                .name("Programación Avanzada")
                .major("Ingeniería Informática")
                .courseYear(2)
                .build();

        testTeacher = Teacher.builder()
                .name("Dr. Rodríguez")
                .email("rodriguez@universidad.edu")
                .password("password123")
                .build();

        testStudent = Student.builder()
                .name("Carlos López")
                .email("carlos.lopez@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();

        // Persistir las entidades relacionadas
        em.persist(testSubject);
        em.persist(testTeacher);
        em.persist(testStudent);
        em.flush();

        // Crear un CourseGroup válido
        validCourseGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(250.50))
                .maxCapacity(30)
                .enrollments(new HashSet<>())
                .groupSessions(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Debe persistir un CourseGroup válido con todos los campos")
    void shouldPersistValidCourseGroup() {
        // When
        em.persist(validCourseGroup);
        em.flush();

        // Then
        assertThat(validCourseGroup.getId()).isNotNull();
        assertThat(validCourseGroup.getCreatedAt()).isNotNull();
        assertThat(validCourseGroup.getUpdatedAt()).isNull();

        // Verificar que se puede recuperar
        CourseGroup found = em.find(CourseGroup.class, validCourseGroup.getId());
        assertThat(found).isNotNull();
        assertThat(found.getSubject().getId()).isEqualTo(testSubject.getId());
        assertThat(found.getTeacher().getId()).isEqualTo(testTeacher.getId());
        assertThat(found.getStatus()).isEqualTo(CourseGroupStatus.PLANNED);
        assertThat(found.getType()).isEqualTo(CourseGroupType.REGULAR);
        assertThat(found.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(250.50));
        assertThat(found.getMaxCapacity()).isEqualTo(30);
    }

    @Test
    @DisplayName("Debe asignar automáticamente ID y createdAt")
    void shouldAutoAssignIdAndCreatedAt() {
        // Given
        assertThat(validCourseGroup.getId()).isNull();
        assertThat(validCourseGroup.getCreatedAt()).isNull();

        // When
        em.persist(validCourseGroup);
        em.flush();

        // Then
        assertThat(validCourseGroup.getId()).isNotNull();
        assertThat(validCourseGroup.getCreatedAt()).isNotNull();
        assertThat(validCourseGroup.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Debe actualizar updatedAt en modificaciones")
    void shouldUpdateUpdatedAtOnModification() {
        // Given
        em.persist(validCourseGroup);
        em.flush();
        assertThat(validCourseGroup.getUpdatedAt()).isNull();

        // When
        validCourseGroup.setPrice(BigDecimal.valueOf(300.00));
        em.flush();

        // Then
        assertThat(validCourseGroup.getUpdatedAt()).isNotNull();
        assertThat(validCourseGroup.getUpdatedAt()).isAfter(validCourseGroup.getCreatedAt());
    }

    @Test
    @DisplayName("Debe fallar cuando subject es null")
    void shouldFailWhenSubjectIsNull() {
        // Given
        validCourseGroup.setSubject(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validCourseGroup);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Subject is required");
    }

    @Test
    @DisplayName("Debe permitir teacher null")
    void shouldAllowNullTeacher() {
        // Given
        validCourseGroup.setTeacher(null);

        // When
        em.persist(validCourseGroup);
        em.flush();

        // Then
        CourseGroup found = em.find(CourseGroup.class, validCourseGroup.getId());
        assertThat(found.getTeacher()).isNull();
    }

    @Test
    @DisplayName("Debe establecer status PLANNED por defecto en @PrePersist")
    void shouldSetDefaultStatusInPrePersist() {
        // Given
        CourseGroup groupWithoutStatus = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .maxCapacity(25)
                .build();
        // No establecemos status explícitamente

        // When
        em.persist(groupWithoutStatus);
        em.flush();

        // Then
        assertThat(groupWithoutStatus.getStatus()).isEqualTo(CourseGroupStatus.PLANNED);
    }

    @Test
    @DisplayName("Debe aceptar todos los valores de CourseGroupStatus")
    void shouldAcceptAllStatusValues() {
        for (CourseGroupStatus status : CourseGroupStatus.values()) {
            CourseGroup group = CourseGroup.builder()
                    .subject(testSubject)
                    .teacher(testTeacher)
                    .status(status)
                    .type(CourseGroupType.REGULAR)
                    .price(BigDecimal.valueOf(200.00))
                    .maxCapacity(20)
                    .build();

            em.persist(group);
            em.flush();

            assertThat(group.getStatus()).isEqualTo(status);
            em.clear();
        }
    }

    @Test
    @DisplayName("Debe establecer type REGULAR por defecto en @PrePersist")
    void shouldSetDefaultTypeInPrePersist() {
        // Given
        CourseGroup groupWithoutType = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .price(BigDecimal.valueOf(200.00))
                .maxCapacity(25)
                .build();
        // No establecemos type explícitamente

        // When
        em.persist(groupWithoutType);
        em.flush();

        // Then
        assertThat(groupWithoutType.getType()).isEqualTo(CourseGroupType.REGULAR);
    }

    @Test
    @DisplayName("Debe aceptar todos los valores de CourseGroupType")
    void shouldAcceptAllTypeValues() {
        for (CourseGroupType type : CourseGroupType.values()) {
            CourseGroup group = CourseGroup.builder()
                    .subject(testSubject)
                    .teacher(testTeacher)
                    .status(CourseGroupStatus.PLANNED)
                    .type(type)
                    .price(BigDecimal.valueOf(200.00))
                    .maxCapacity(20)
                    .build();

            em.persist(group);
            em.flush();

            assertThat(group.getType()).isEqualTo(type);
            em.clear();
        }
    }

    @Test
    @DisplayName("Debe fallar cuando price es null")
    void shouldFailWhenPriceIsNull() {
        // Given
        validCourseGroup.setPrice(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validCourseGroup);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Price is required");
    }

    @Test
    @DisplayName("Debe fallar cuando price es cero")
    void shouldFailWhenPriceIsZero() {
        // Given
        validCourseGroup.setPrice(BigDecimal.ZERO);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validCourseGroup);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Price must be greater than 0");
    }

    @Test
    @DisplayName("Debe fallar cuando price es negativo")
    void shouldFailWhenPriceIsNegative() {
        // Given
        validCourseGroup.setPrice(BigDecimal.valueOf(-10.00));

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validCourseGroup);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Price must be greater than 0");
    }

    @Test
    @DisplayName("Debe aceptar precios con hasta 2 decimales")
    void shouldAcceptPricesWithTwoDecimals() {
        // Given
        validCourseGroup.setPrice(BigDecimal.valueOf(199.99));

        // When
        em.persist(validCourseGroup);
        em.flush();

        // Then
        CourseGroup found = em.find(CourseGroup.class, validCourseGroup.getId());
        assertThat(found.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(199.99));
    }

    @Test
    @DisplayName("Debe fallar cuando price tiene más de 8 dígitos enteros")
    void shouldFailWhenPriceExceedsIntegerDigits() {
        // Given - 9 dígitos enteros
        validCourseGroup.setPrice(BigDecimal.valueOf(123456789.00));

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validCourseGroup);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Price must have at most 8 integer digits and 2 decimal places");
    }



    @Test
    @DisplayName("Debe fallar cuando maxCapacity es menor a 1")
    void shouldFailWhenMaxCapacityIsLessThanOne() {
        // Given
        validCourseGroup.setMaxCapacity(0);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validCourseGroup);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Max capacity must be at least 1");
    }

    @Test
    @DisplayName("Debe establecer maxCapacity 30 por defecto en @PrePersist")
    void shouldSetDefaultMaxCapacityInPrePersist() {
        // Given
        CourseGroup groupWithoutCapacity = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .build();
        // No establecemos maxCapacity

        // When
        em.persist(groupWithoutCapacity);
        em.flush();

        // Then
        assertThat(groupWithoutCapacity.getMaxCapacity()).isEqualTo(30);
    }

    @Test
    @DisplayName("Debe inicializar enrollments y groupSessions como conjuntos vacíos")
    void shouldInitializeCollectionsAsEmptySets() {
        // When
        em.persist(validCourseGroup);
        em.flush();
        em.clear();

        // Then
        CourseGroup found = em.find(CourseGroup.class, validCourseGroup.getId());
        assertThat(found.getEnrollments()).isNotNull();
        assertThat(found.getEnrollments()).isEmpty();
        assertThat(found.getGroupSessions()).isNotNull();
        assertThat(found.getGroupSessions()).isEmpty();
    }

    @Test
    @DisplayName("Debe gestionar relación bidireccional con Enrollment")
    void shouldManageBidirectionalRelationshipWithEnrollment() {
        // Given
        em.persist(validCourseGroup);
        em.flush();

        // When - Crear enrollment usando helper method
        Enrollment enrollment = Enrollment.builder()
                .student(testStudent)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        validCourseGroup.addEnrollment(enrollment);
        em.persist(enrollment);
        em.flush();
        em.clear();

        // Then
        CourseGroup foundGroup = em.find(CourseGroup.class, validCourseGroup.getId());
        assertThat(foundGroup.getEnrollments()).hasSize(1);

        Enrollment foundEnrollment = foundGroup.getEnrollments().iterator().next();
        assertThat(foundEnrollment.getCourseGroup()).isEqualTo(foundGroup);
        assertThat(foundEnrollment.getStudent().getId()).isEqualTo(testStudent.getId());
    }

    @Test
    @DisplayName("Debe gestionar relación bidireccional con GroupSession")
    void shouldManageBidirectionalRelationshipWithGroupSession() {
        // Given
        em.persist(validCourseGroup);
        em.flush();

        // When - Crear group session usando helper method
        GroupSession session = GroupSession.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 101")
                .build();

        validCourseGroup.addGroupSession(session);
        em.persist(session);
        em.flush();
        em.clear();

        // Then
        CourseGroup foundGroup = em.find(CourseGroup.class, validCourseGroup.getId());
        assertThat(foundGroup.getGroupSessions()).hasSize(1);

        GroupSession foundSession = foundGroup.getGroupSessions().iterator().next();
        assertThat(foundSession.getCourseGroup()).isEqualTo(foundGroup);
        assertThat(foundSession.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("Debe eliminar enrollments en cascada")
    void shouldCascadeDeleteEnrollments() {
        // Given
        em.persist(validCourseGroup);

        Enrollment enrollment = Enrollment.builder()
                .student(testStudent)
                .build();

        validCourseGroup.addEnrollment(enrollment);
        em.persist(enrollment);
        em.flush();

        Long enrollmentId = enrollment.getId();
        assertThat(em.find(Enrollment.class, enrollmentId)).isNotNull();

        // When
        em.remove(validCourseGroup);
        em.flush();

        // Then
        assertThat(em.find(CourseGroup.class, validCourseGroup.getId())).isNull();
        assertThat(em.find(Enrollment.class, enrollmentId)).isNull();
        // El estudiante debe seguir existiendo
        assertThat(em.find(Student.class, testStudent.getId())).isNotNull();
    }

    @Test
    @DisplayName("Debe eliminar groupSessions en cascada")
    void shouldCascadeDeleteGroupSessions() {
        // Given
        em.persist(validCourseGroup);

        GroupSession session = GroupSession.builder()
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .build();

        validCourseGroup.addGroupSession(session);
        em.persist(session);
        em.flush();

        Long sessionId = session.getId();
        assertThat(em.find(GroupSession.class, sessionId)).isNotNull();

        // When
        em.remove(validCourseGroup);
        em.flush();

        // Then
        assertThat(em.find(CourseGroup.class, validCourseGroup.getId())).isNull();
        assertThat(em.find(GroupSession.class, sessionId)).isNull();
    }

    @Test
    @DisplayName("Debe verificar método hasCapacity")
    void shouldVerifyHasCapacityMethod() {
        // Given
        em.persist(validCourseGroup);
        em.flush();

        // Initially should have capacity
        assertThat(validCourseGroup.hasCapacity()).isTrue();
        assertThat(validCourseGroup.getAvailableSpots()).isEqualTo(30);

        // Add enrollments up to capacity
        for (int i = 0; i < 30; i++) {
            Student student = Student.builder()
                    .name("Student " + i)
                    .email("student" + i + "@test.com")
                    .password("password123")
                    .major("Ingeniería Informática")
                    .build();
            em.persist(student);

            Enrollment enrollment = Enrollment.builder()
                    .student(student)
                    .build();
            validCourseGroup.addEnrollment(enrollment);
            em.persist(enrollment);
        }
        em.flush();

        // Then should not have capacity
        assertThat(validCourseGroup.hasCapacity()).isFalse();
        assertThat(validCourseGroup.getAvailableSpots()).isEqualTo(0);
    }

    @Test
    @DisplayName("Debe verificar método getAvailableSpots")
    void shouldVerifyGetAvailableSpotsMethod() {
        // Given
        validCourseGroup.setMaxCapacity(5);
        em.persist(validCourseGroup);

        // Add 3 enrollments
        for (int i = 0; i < 3; i++) {
            Student student = Student.builder()
                    .name("Student " + i)
                    .email("student" + i + "@test.com")
                    .password("password123")
                    .major("Ingeniería Informática")
                    .build();
            em.persist(student);

            Enrollment enrollment = Enrollment.builder()
                    .student(student)
                    .build();
            validCourseGroup.addEnrollment(enrollment);
            em.persist(enrollment);
        }
        em.flush();

        // Then
        assertThat(validCourseGroup.getAvailableSpots()).isEqualTo(2);
        assertThat(validCourseGroup.hasCapacity()).isTrue();
    }

    @Test
    @DisplayName("Debe verificar equals y hashCode basados en ID")
    void shouldImplementEqualsAndHashCodeBasedOnId() {
        // Given
        CourseGroup group1 = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(100.00))
                .build();

        CourseGroup group2 = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.INTENSIVE)
                .price(BigDecimal.valueOf(200.00))
                .build();

        // Before persistence
        assertThat(group1).isNotEqualTo(group2);

        // Persist both
        em.persist(group1);
        em.persist(group2);
        em.flush();

        // Con IDs asignados
        assertThat(group1).isNotEqualTo(group2);
        assertThat(group1.getId()).isNotEqualTo(group2.getId());

        // Buscar el mismo group
        CourseGroup sameGroup = em.find(CourseGroup.class, group1.getId());
        assertThat(group1).isEqualTo(sameGroup);
    }

    @Test
    @DisplayName("Debe verificar que enrollments, groupSessions, subject y teacher no se incluyen en toString")
    void shouldNotIncludeRelationsInToString() {
        // When
        String toString = validCourseGroup.toString();

        // Then
        assertThat(toString).doesNotContain("enrollments");
        assertThat(toString).doesNotContain("groupSessions");
        assertThat(toString).doesNotContain("subject");
        assertThat(toString).doesNotContain("teacher");
        assertThat(toString).contains("CourseGroup");
        assertThat(toString).contains("status=");
        assertThat(toString).contains("type=");
        assertThat(toString).contains("price=");
    }

    @Test
    @DisplayName("Debe gestionar correctamente removeEnrollment")
    void shouldCorrectlyRemoveEnrollment() {
        // Given
        em.persist(validCourseGroup);

        Enrollment enrollment = Enrollment.builder()
                .student(testStudent)
                .build();

        validCourseGroup.addEnrollment(enrollment);
        em.persist(enrollment);
        em.flush();

        assertThat(validCourseGroup.getEnrollments()).hasSize(1);
        assertThat(enrollment.getCourseGroup()).isEqualTo(validCourseGroup);

        // When
        validCourseGroup.removeEnrollment(enrollment);
        em.remove(enrollment);
        em.flush();

        // Then
        assertThat(validCourseGroup.getEnrollments()).isEmpty();
    }

    @Test
    @DisplayName("Debe gestionar correctamente removeGroupSession")
    void shouldCorrectlyRemoveGroupSession() {
        // Given
        em.persist(validCourseGroup);

        GroupSession session = GroupSession.builder()
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        validCourseGroup.addGroupSession(session);
        em.persist(session);
        em.flush();

        assertThat(validCourseGroup.getGroupSessions()).hasSize(1);
        assertThat(session.getCourseGroup()).isEqualTo(validCourseGroup);

        // When
        validCourseGroup.removeGroupSession(session);
        em.remove(session);
        em.flush();

        // Then
        assertThat(validCourseGroup.getGroupSessions()).isEmpty();
    }

    @Test
    @DisplayName("Debe manejar correctamente la relación con Subject")
    void shouldCorrectlyHandleSubjectRelation() {
        // When
        em.persist(validCourseGroup);
        em.flush();
        em.clear();

        // Then
        CourseGroup found = em.find(CourseGroup.class, validCourseGroup.getId());
        assertThat(found.getSubject()).isNotNull();
        assertThat(found.getSubject().getId()).isEqualTo(testSubject.getId());
        assertThat(found.getSubject().getName()).isEqualTo("Programación Avanzada");
    }

    @Test
    @DisplayName("Debe manejar correctamente la relación con Teacher nullable")
    void shouldCorrectlyHandleNullableTeacherRelation() {
        // Given - grupo sin profesor
        CourseGroup groupWithoutTeacher = CourseGroup.builder()
                .subject(testSubject)
                .teacher(null)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(150.00))
                .build();

        // When
        em.persist(groupWithoutTeacher);
        em.flush();
        em.clear();

        // Then
        CourseGroup found = em.find(CourseGroup.class, groupWithoutTeacher.getId());
        assertThat(found.getTeacher()).isNull();
    }

    @Test
    @DisplayName("Debe validar que status no puede ser null después de persistir")
    void shouldNotAllowNullStatusAfterPersist() {
        // Given
        em.persist(validCourseGroup);
        em.flush();

        // When - intentar setear null después de persistir
        validCourseGroup.setStatus(null);

        // Then
        assertThatThrownBy(() -> {
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Status is required");
    }

    @Test
    @DisplayName("Debe validar que type no puede ser null después de persistir")
    void shouldNotAllowNullTypeAfterPersist() {
        // Given
        em.persist(validCourseGroup);
        em.flush();

        // When - intentar setear null después de persistir
        validCourseGroup.setType(null);

        // Then
        assertThatThrownBy(() -> {
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Type is required");
    }

    @Test
    @DisplayName("Debe validar que maxcapacity no puede ser null después de persistir")
    void shouldNotAllowNullMaxCapacityAfterPersist() {
        // Given
        em.persist(validCourseGroup);
        em.flush();

        // When - intentar setear null después de persistir
        validCourseGroup.setMaxCapacity(null);

        // Then
        assertThatThrownBy(() -> {
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Max capacity is required");
    }
}
