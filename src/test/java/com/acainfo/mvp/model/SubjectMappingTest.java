package com.acainfo.mvp.model;

import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
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
 * Test de mapeo JPA para la entidad Subject.
 * Valida constraints, atributos, relaciones y comportamiento de persistencia.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Subject Entity Mapping Tests")
@Transactional
class SubjectMappingTest {

    @Autowired
    private EntityManager em;

    private Subject validSubject;
    private Teacher testTeacher;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Crear un subject válido
        validSubject = Subject.builder()
                .name("Cálculo I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .courseGroups(new HashSet<>())
                .groupRequests(new HashSet<>())
                .build();

        // Crear entidades relacionadas para tests
        testTeacher = Teacher.builder()
                .name("Dr. Martínez")
                .email("martinez@universidad.edu")
                .password("password123")
                .build();

        testStudent = Student.builder()
                .name("Ana García")
                .email("ana.garcia@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();
    }

    @Test
    @DisplayName("Debe persistir un Subject válido con todos los campos")
    void shouldPersistValidSubject() {
        // When
        em.persist(validSubject);
        em.flush();

        // Then
        assertThat(validSubject.getId()).isNotNull();
        assertThat(validSubject.getCreatedAt()).isNotNull();
        assertThat(validSubject.getUpdatedAt()).isNull();

        // Verificar que se puede recuperar
        Subject found = em.find(Subject.class, validSubject.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Cálculo I");
        assertThat(found.getMajor()).isEqualTo("Ingeniería Informática");
        assertThat(found.getCourseYear()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe asignar automáticamente ID y createdAt")
    void shouldAutoAssignIdAndCreatedAt() {
        // Given
        assertThat(validSubject.getId()).isNull();
        assertThat(validSubject.getCreatedAt()).isNull();

        // When
        em.persist(validSubject);
        em.flush();

        // Then
        assertThat(validSubject.getId()).isNotNull();
        assertThat(validSubject.getCreatedAt()).isNotNull();
        assertThat(validSubject.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Debe actualizar updatedAt en modificaciones")
    void shouldUpdateUpdatedAtOnModification() {
        // Given
        em.persist(validSubject);
        em.flush();
        assertThat(validSubject.getUpdatedAt()).isNull();

        // When
        validSubject.setName("Cálculo I - Actualizado");
        em.flush();

        // Then
        assertThat(validSubject.getUpdatedAt()).isNotNull();
        assertThat(validSubject.getUpdatedAt()).isAfter(validSubject.getCreatedAt());
    }

    @Test
    @DisplayName("Debe fallar cuando name es null")
    void shouldFailWhenNameIsNull() {
        // Given
        validSubject.setName(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name is required");
    }

    @Test
    @DisplayName("Debe fallar cuando name está vacío")
    void shouldFailWhenNameIsBlank() {
        // Given
        validSubject.setName("   ");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name is required");
    }

    @Test
    @DisplayName("Debe fallar cuando name excede 150 caracteres")
    void shouldFailWhenNameExceedsMaxLength() {
        // Given
        String longName = "A".repeat(151);
        validSubject.setName(longName);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name must not exceed 150 characters");
    }

    @Test
    @DisplayName("Debe fallar cuando major es null")
    void shouldFailWhenMajorIsNull() {
        // Given
        validSubject.setMajor(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Major is required");
    }

    @Test
    @DisplayName("Debe fallar cuando major está vacío")
    void shouldFailWhenMajorIsBlank() {
        // Given
        validSubject.setMajor("   ");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Major is required");
    }

    @Test
    @DisplayName("Debe fallar cuando major excede 100 caracteres")
    void shouldFailWhenMajorExceedsMaxLength() {
        // Given
        String longMajor = "A".repeat(101);
        validSubject.setMajor(longMajor);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Major must not exceed 100 characters");
    }

    @Test
    @DisplayName("Debe fallar cuando courseYear es null")
    void shouldFailWhenCourseYearIsNull() {
        // Given
        validSubject.setCourseYear(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Course year is required");
    }

    @Test
    @DisplayName("Debe fallar cuando courseYear es menor a 1")
    void shouldFailWhenCourseYearIsLessThanOne() {
        // Given
        validSubject.setCourseYear(0);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Course year must be at least 1");
    }

    @Test
    @DisplayName("Debe fallar cuando courseYear es mayor a 6")
    void shouldFailWhenCourseYearIsGreaterThanSix() {
        // Given
        validSubject.setCourseYear(7);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validSubject);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Course year must not exceed 6");
    }

    @Test
    @DisplayName("Debe aceptar courseYear entre 1 y 6")
    void shouldAcceptValidCourseYears() {
        // Test para cada año válido
        for (int year = 1; year <= 6; year++) {
            Subject subject = Subject.builder()
                    .name("Asignatura Año " + year)
                    .major("Carrera Test")
                    .courseYear(year)
                    .build();

            em.persist(subject);
            em.flush();

            assertThat(subject.getId()).isNotNull();
            assertThat(subject.getCourseYear()).isEqualTo(year);

            // Limpiar para el siguiente test
            em.clear();
        }
    }

    @Test
    @DisplayName("Debe respetar constraint único de name + major")
    void shouldEnforceUniqueNameMajorConstraint() {
        // Given
        em.persist(validSubject);
        em.flush();

        Subject duplicateSubject = Subject.builder()
                .name("Cálculo I") // mismo nombre
                .major("Ingeniería Informática") // misma carrera
                .courseYear(2) // diferente año (no importa)
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(duplicateSubject);
            em.flush();
        }).satisfies(throwable -> {
            String message = throwable.getMessage();
            assertThat(message.toLowerCase()).contains("unique");
        });
    }

    @Test
    @DisplayName("Debe permitir mismo nombre en diferentes carreras")
    void shouldAllowSameNameInDifferentMajors() {
        // Given
        Subject subject1 = Subject.builder()
                .name("Cálculo I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();

        Subject subject2 = Subject.builder()
                .name("Cálculo I") // mismo nombre
                .major("Ingeniería Civil") // diferente carrera
                .courseYear(1)
                .build();

        // When
        em.persist(subject1);
        em.persist(subject2);
        em.flush();

        // Then
        assertThat(subject1.getId()).isNotNull();
        assertThat(subject2.getId()).isNotNull();
        assertThat(subject1.getId()).isNotEqualTo(subject2.getId());
    }

    @Test
    @DisplayName("Debe permitir diferentes nombres en la misma carrera")
    void shouldAllowDifferentNamesInSameMajor() {
        // Given
        Subject subject1 = Subject.builder()
                .name("Cálculo I")
                .major("Ingeniería Informática")
                .courseYear(1)
                .build();

        Subject subject2 = Subject.builder()
                .name("Álgebra Lineal") // diferente nombre
                .major("Ingeniería Informática") // misma carrera
                .courseYear(1)
                .build();

        // When
        em.persist(subject1);
        em.persist(subject2);
        em.flush();

        // Then
        assertThat(subject1.getId()).isNotNull();
        assertThat(subject2.getId()).isNotNull();
        assertThat(subject1.getId()).isNotEqualTo(subject2.getId());
    }

    @Test
    @DisplayName("Debe inicializar courseGroups y groupRequests como conjuntos vacíos")
    void shouldInitializeCollectionsAsEmptySets() {
        // When
        em.persist(validSubject);
        em.flush();
        em.clear();

        // Then
        Subject found = em.find(Subject.class, validSubject.getId());
        assertThat(found.getCourseGroups()).isNotNull();
        assertThat(found.getCourseGroups()).isEmpty();
        assertThat(found.getGroupRequests()).isNotNull();
        assertThat(found.getGroupRequests()).isEmpty();
    }

    @Test
    @DisplayName("Debe gestionar relación bidireccional con CourseGroup")
    void shouldManageBidirectionalRelationshipWithCourseGroup() {
        // Given
        em.persist(validSubject);
        em.persist(testTeacher);
        em.flush();

        // When - Crear course group usando helper method
        CourseGroup courseGroup = CourseGroup.builder()
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .maxCapacity(30)
                .build();

        validSubject.addCourseGroup(courseGroup);
        em.persist(courseGroup);
        em.flush();
        em.clear();

        // Then
        Subject foundSubject = em.find(Subject.class, validSubject.getId());
        assertThat(foundSubject.getCourseGroups()).hasSize(1);

        CourseGroup foundGroup = foundSubject.getCourseGroups().iterator().next();
        assertThat(foundGroup.getSubject()).isEqualTo(foundSubject);
        assertThat(foundGroup.getTeacher().getId()).isEqualTo(testTeacher.getId());
    }

    @Test
    @DisplayName("Debe gestionar relación bidireccional con GroupRequest")
    void shouldManageBidirectionalRelationshipWithGroupRequest() {
        // Given
        em.persist(validSubject);
        em.persist(testStudent);
        em.flush();

        // When - Crear group request usando helper method
        GroupRequest request = GroupRequest.builder()
                .student(testStudent)
                .status(RequestStatus.PENDING)
                .build();

        validSubject.addGroupRequest(request);
        em.persist(request);
        em.flush();
        em.clear();

        // Then
        Subject foundSubject = em.find(Subject.class, validSubject.getId());
        assertThat(foundSubject.getGroupRequests()).hasSize(1);

        GroupRequest foundRequest = foundSubject.getGroupRequests().iterator().next();
        assertThat(foundRequest.getSubject()).isEqualTo(foundSubject);
        assertThat(foundRequest.getStudent().getId()).isEqualTo(testStudent.getId());
    }

    @Test
    @DisplayName("Debe eliminar courseGroups en cascada")
    void shouldCascadeDeleteCourseGroups() {
        // Given
        em.persist(validSubject);
        em.persist(testTeacher);

        CourseGroup courseGroup = CourseGroup.builder()
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .build();

        validSubject.addCourseGroup(courseGroup);
        em.persist(courseGroup);
        em.flush();

        Long groupId = courseGroup.getId();
        assertThat(em.find(CourseGroup.class, groupId)).isNotNull();

        // When
        em.remove(validSubject);
        em.flush();

        // Then
        assertThat(em.find(Subject.class, validSubject.getId())).isNull();
        assertThat(em.find(CourseGroup.class, groupId)).isNull();
        // El profesor debe seguir existiendo
        assertThat(em.find(Teacher.class, testTeacher.getId())).isNotNull();
    }

    @Test
    @DisplayName("Debe eliminar groupRequests en cascada")
    void shouldCascadeDeleteGroupRequests() {
        // Given
        em.persist(validSubject);
        em.persist(testStudent);

        GroupRequest request = GroupRequest.builder()
                .student(testStudent)
                .build();

        validSubject.addGroupRequest(request);
        em.persist(request);
        em.flush();

        Long requestId = request.getId();
        assertThat(em.find(GroupRequest.class, requestId)).isNotNull();

        // When
        em.remove(validSubject);
        em.flush();

        // Then
        assertThat(em.find(Subject.class, validSubject.getId())).isNull();
        assertThat(em.find(GroupRequest.class, requestId)).isNull();
        // El estudiante debe seguir existiendo
        assertThat(em.find(Student.class, testStudent.getId())).isNotNull();
    }

    @Test
    @DisplayName("Debe verificar equals y hashCode basados en ID")
    void shouldImplementEqualsAndHashCodeBasedOnId() {
        // Given
        Subject subject1 = Subject.builder()
                .name("Cálculo I")
                .major("Ingeniería")
                .courseYear(1)
                .build();

        Subject subject2 = Subject.builder()
                .name("Álgebra")
                .major("Matemáticas")
                .courseYear(2)
                .build();

        // Before persistence
        assertThat(subject1).isNotEqualTo(subject2);

        // Persist both
        em.persist(subject1);
        em.persist(subject2);
        em.flush();

        // Con IDs asignados
        assertThat(subject1).isNotEqualTo(subject2);
        assertThat(subject1.getId()).isNotEqualTo(subject2.getId());

        // Buscar el mismo subject
        Subject sameSubject = em.find(Subject.class, subject1.getId());
        assertThat(subject1).isEqualTo(sameSubject);

        // Verificar comportamiento con null
        assertThat(subject1).isNotEqualTo(null);
        assertThat(subject1).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("Debe verificar que courseGroups y groupRequests no se incluyen en toString")
    void shouldNotIncludeCollectionsInToString() {
        // When
        String toString = validSubject.toString();

        // Then
        assertThat(toString).doesNotContain("courseGroups");
        assertThat(toString).doesNotContain("groupRequests");
        assertThat(toString).contains("Subject");
        assertThat(toString).contains("name=");
        assertThat(toString).contains("major=");
        assertThat(toString).contains("courseYear=");
    }

    @Test
    @DisplayName("Debe permitir caracteres especiales válidos en campos de texto")
    void shouldAllowSpecialCharactersInTextFields() {
        // Given
        validSubject.setName("Matemáticas Básicas: Álgebra y Cálculo");
        validSubject.setMajor("Ingeniería en Computación y Sistemas");

        // When
        em.persist(validSubject);
        em.flush();

        // Then
        Subject found = em.find(Subject.class, validSubject.getId());
        assertThat(found.getName()).isEqualTo("Matemáticas Básicas: Álgebra y Cálculo");
        assertThat(found.getMajor()).isEqualTo("Ingeniería en Computación y Sistemas");
    }

    @Test
    @DisplayName("Debe gestionar correctamente removeCourseGroup")
    void shouldCorrectlyRemoveCourseGroup() {
        // Given
        em.persist(validSubject);
        em.persist(testTeacher);

        CourseGroup courseGroup = CourseGroup.builder()
                .teacher(testTeacher)
                .status(CourseGroupStatus.PLANNED)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(200.00))
                .build();

        validSubject.addCourseGroup(courseGroup);
        em.persist(courseGroup);
        em.flush();

        assertThat(validSubject.getCourseGroups()).hasSize(1);
        assertThat(courseGroup.getSubject()).isEqualTo(validSubject);

        // When
        validSubject.removeCourseGroup(courseGroup);
        em.remove(courseGroup);
        em.flush();

        // Then
        assertThat(validSubject.getCourseGroups()).isEmpty();
        assertThat(courseGroup.getSubject()).isNull();
    }

    @Test
    @DisplayName("Debe gestionar correctamente removeGroupRequest")
    void shouldCorrectlyRemoveGroupRequest() {
        // Given
        em.persist(validSubject);
        em.persist(testStudent);

        GroupRequest request = GroupRequest.builder()
                .student(testStudent)
                .build();

        validSubject.addGroupRequest(request);
        em.persist(request);
        em.flush();

        assertThat(validSubject.getGroupRequests()).hasSize(1);
        assertThat(request.getSubject()).isEqualTo(validSubject);

        // When
        validSubject.removeGroupRequest(request);
        em.remove(request);
        em.flush();

        // Then
        assertThat(validSubject.getGroupRequests()).isEmpty();
        assertThat(request.getSubject()).isNull();
    }
}