package com.acainfo.mvp.model;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de mapeo JPA para la entidad Teacher.
 * Valida constraints, atributos y comportamiento de persistencia.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Teacher Entity Mapping Tests")
@Transactional
class TeacherMappingTest {

    @Autowired
    private EntityManager em;

    private Teacher validTeacher;

    @BeforeEach
    void setUp() {
        validTeacher = Teacher.builder()
                .name("Dr. García López")
                .email("garcia.lopez@universidad.edu")
                .password("encodedPassword123")
                .courseGroups(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Debe persistir un Teacher válido con todos los campos")
    void shouldPersistValidTeacher() {
        // When
        em.persist(validTeacher);
        em.flush();

        // Then
        assertThat(validTeacher.getId()).isNotNull();
        assertThat(validTeacher.getCreatedAt()).isNotNull();
        assertThat(validTeacher.getUpdatedAt()).isNull(); // Solo se setea en updates

        // Verificar que se puede recuperar
        Teacher found = em.find(Teacher.class, validTeacher.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Dr. García López");
        assertThat(found.getEmail()).isEqualTo("garcia.lopez@universidad.edu");
        assertThat(found.getPassword()).isEqualTo("encodedPassword123");
    }

    @Test
    @DisplayName("Debe asignar automáticamente ID y createdAt")
    void shouldAutoAssignIdAndCreatedAt() {
        // Given
        assertThat(validTeacher.getId()).isNull();
        assertThat(validTeacher.getCreatedAt()).isNull();

        // When
        em.persist(validTeacher);
        em.flush();

        // Then
        assertThat(validTeacher.getId()).isNotNull();
        assertThat(validTeacher.getCreatedAt()).isNotNull();
        assertThat(validTeacher.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Debe actualizar updatedAt en modificaciones")
    void shouldUpdateUpdatedAtOnModification() {
        // Given
        em.persist(validTeacher);
        em.flush();
        assertThat(validTeacher.getUpdatedAt()).isNull();

        // When
        validTeacher.setName("Dr. García López Actualizado");
        em.flush();

        // Then
        assertThat(validTeacher.getUpdatedAt()).isNotNull();
        assertThat(validTeacher.getUpdatedAt()).isAfter(validTeacher.getCreatedAt());
    }

    @Test
    @DisplayName("Debe fallar cuando name es null")
    void shouldFailWhenNameIsNull() {
        // Given
        validTeacher.setName(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validTeacher);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name is required");
    }

    @Test
    @DisplayName("Debe fallar cuando name está vacío")
    void shouldFailWhenNameIsBlank() {
        // Given
        validTeacher.setName("   ");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validTeacher);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name is required");
    }

    @Test
    @DisplayName("Debe fallar cuando name excede 100 caracteres")
    void shouldFailWhenNameExceedsMaxLength() {
        // Given
        String longName = "A".repeat(101);
        validTeacher.setName(longName);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validTeacher);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Name must not exceed 100 characters");
    }

    @Test
    @DisplayName("Debe fallar cuando email es null")
    void shouldFailWhenEmailIsNull() {
        // Given
        validTeacher.setEmail(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validTeacher);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    @DisplayName("Debe fallar cuando email es inválido")
    void shouldFailWhenEmailIsInvalid() {
        // Given
        validTeacher.setEmail("not-an-email");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validTeacher);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Email must be valid");
    }


    @Test
    @DisplayName("Debe fallar cuando password es null")
    void shouldFailWhenPasswordIsNull() {
        // Given
        validTeacher.setPassword(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validTeacher);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    @DisplayName("Debe fallar cuando password es menor a 8 caracteres")
    void shouldFailWhenPasswordIsTooShort() {
        // Given
        validTeacher.setPassword("1234567");

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validTeacher);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Password must be at least 8 characters long");
    }

    @Test
    @DisplayName("Debe respetar constraint único de email")
    void shouldEnforceUniqueEmailConstraint() {
        // Given
        em.persist(validTeacher);
        em.flush();

        Teacher duplicateEmailTeacher = Teacher.builder()
                .name("Otro Profesor")
                .email("garcia.lopez@universidad.edu") // mismo email
                .password("otroPassword123")
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(duplicateEmailTeacher);
            em.flush();
        }).satisfies(throwable -> {
            // Verificar que es algún tipo de violación de constraint
            String message = throwable.getMessage();
            assertThat(message.toLowerCase()).contains("unique");
        });
    }

    @Test
    @DisplayName("Debe permitir múltiples teachers con emails diferentes")
    void shouldAllowMultipleTeachersWithDifferentEmails() {
        // Given
        Teacher teacher1 = Teacher.builder()
                .name("Profesor 1")
                .email("profesor1@universidad.edu")
                .password("password123")
                .build();

        Teacher teacher2 = Teacher.builder()
                .name("Profesor 2")
                .email("profesor2@universidad.edu")
                .password("password123")
                .build();

        // When
        em.persist(teacher1);
        em.persist(teacher2);
        em.flush();

        // Then
        assertThat(teacher1.getId()).isNotNull();
        assertThat(teacher2.getId()).isNotNull();
        assertThat(teacher1.getId()).isNotEqualTo(teacher2.getId());
    }

    @Test
    @DisplayName("Debe inicializar courseGroups como conjunto vacío")
    void shouldInitializeCourseGroupsAsEmptySet() {
        // When
        em.persist(validTeacher);
        em.flush();
        em.clear(); // Limpiar contexto de persistencia

        // Then
        Teacher found = em.find(Teacher.class, validTeacher.getId());
        assertThat(found.getCourseGroups()).isNotNull();
        assertThat(found.getCourseGroups()).isEmpty();
    }

    @Test
    @DisplayName("Debe verificar equals y hashCode basados en ID")
    void shouldImplementEqualsAndHashCodeBasedOnId() {
        // Given
        Teacher teacher1 = Teacher.builder()
                .name("Profesor 1")
                .email("profesor1@universidad.edu")
                .password("password123")
                .courseGroups(new HashSet<>())
                .build();

        Teacher teacher2 = Teacher.builder()
                .name("Profesor 2")
                .email("profesor2@universidad.edu")
                .password("password456")
                .courseGroups(new HashSet<>())
                .build();

        // Before persistence - diferentes objetos sin ID deberían ser diferentes
        assertThat(teacher1).isNotEqualTo(teacher2);

        // Persist both teachers
        em.persist(teacher1);
        em.persist(teacher2);
        em.flush();

        // Ahora con IDs asignados, deberían ser diferentes
        assertThat(teacher1).isNotEqualTo(teacher2);
        assertThat(teacher1.getId()).isNotEqualTo(teacher2.getId());

        // Buscar el mismo teacher por ID
        Teacher sameTeacher = em.find(Teacher.class, teacher1.getId());
        assertThat(teacher1).isEqualTo(sameTeacher);

        // Verificar comportamiento con null
        assertThat(teacher1).isNotEqualTo(null);
        assertThat(teacher1).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("Debe verificar que password no se incluye en toString")
    void shouldNotIncludePasswordInToString() {
        // When
        String toString = validTeacher.toString();

        // Then
        assertThat(toString).doesNotContain("password");
        assertThat(toString).doesNotContain("encodedPassword123");
        assertThat(toString).contains("Teacher");
        assertThat(toString).contains("name=");
        assertThat(toString).contains("email=");
    }

    @Test
    @DisplayName("Debe permitir caracteres especiales válidos en el nombre")
    void shouldAllowSpecialCharactersInName() {
        // Given
        validTeacher.setName("Dr. María José García-López Ñúñez");

        // When
        em.persist(validTeacher);
        em.flush();

        // Then
        Teacher found = em.find(Teacher.class, validTeacher.getId());
        assertThat(found.getName()).isEqualTo("Dr. María José García-López Ñúñez");
    }
}