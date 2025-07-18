package com.acainfo.mvp.model;

import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import com.acainfo.mvp.model.enums.DayOfWeek;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de mapeo JPA para la entidad GroupSession.
 * Valida constraints, atributos, relaciones y comportamiento de persistencia.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("GroupSession Entity Mapping Tests")
@Transactional
class GroupSessionMappingTest {

    @Autowired
    private EntityManager em;

    private GroupSession validGroupSession;
    private CourseGroup testCourseGroup;
    private Subject testSubject;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() {
        // Crear entidades relacionadas
        testSubject = Subject.builder()
                .name("Redes de Computadores")
                .major("Ingeniería Informática")
                .courseYear(3)
                .build();

        testTeacher = Teacher.builder()
                .name("Ing. Morales")
                .email("morales@universidad.edu")
                .password("password123")
                .build();

        testCourseGroup = CourseGroup.builder()
                .subject(testSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(280.00))
                .maxCapacity(35)
                .build();

        // Persistir las entidades relacionadas
        em.persist(testSubject);
        em.persist(testTeacher);
        em.persist(testCourseGroup);
        em.flush();

        // Crear un GroupSession válido
        validGroupSession = GroupSession.builder()
                .courseGroup(testCourseGroup)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 301")
                .build();
    }

    @Test
    @DisplayName("Debe persistir un GroupSession válido con todos los campos")
    void shouldPersistValidGroupSession() {
        // When
        em.persist(validGroupSession);
        em.flush();

        // Then
        assertThat(validGroupSession.getId()).isNotNull();
        assertThat(validGroupSession.getCreatedAt()).isNotNull();
        assertThat(validGroupSession.getUpdatedAt()).isNull();

        // Verificar que se puede recuperar
        GroupSession found = em.find(GroupSession.class, validGroupSession.getId());
        assertThat(found).isNotNull();
        assertThat(found.getCourseGroup().getId()).isEqualTo(testCourseGroup.getId());
        assertThat(found.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(found.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(found.getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(found.getClassroom()).isEqualTo("Aula 301");
    }

    @Test
    @DisplayName("Debe asignar automáticamente ID y createdAt")
    void shouldAutoAssignIdAndCreatedAt() {
        // Given
        assertThat(validGroupSession.getId()).isNull();
        assertThat(validGroupSession.getCreatedAt()).isNull();

        // When
        em.persist(validGroupSession);
        em.flush();

        // Then
        assertThat(validGroupSession.getId()).isNotNull();
        assertThat(validGroupSession.getCreatedAt()).isNotNull();
        assertThat(validGroupSession.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Debe actualizar updatedAt en modificaciones")
    void shouldUpdateUpdatedAtOnModification() {
        // Given
        em.persist(validGroupSession);
        em.flush();
        assertThat(validGroupSession.getUpdatedAt()).isNull();

        // When
        validGroupSession.setClassroom("Aula 302");
        em.flush();

        // Then
        assertThat(validGroupSession.getUpdatedAt()).isNotNull();
        assertThat(validGroupSession.getUpdatedAt()).isAfter(validGroupSession.getCreatedAt());
    }

    @Test
    @DisplayName("Debe fallar cuando courseGroup es null")
    void shouldFailWhenCourseGroupIsNull() {
        // Given
        validGroupSession.setCourseGroup(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupSession);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Course group is required");
    }

    @Test
    @DisplayName("Debe fallar cuando dayOfWeek es null")
    void shouldFailWhenDayOfWeekIsNull() {
        // Given
        validGroupSession.setDayOfWeek(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupSession);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Day of week is required");
    }

    @Test
    @DisplayName("Debe fallar cuando startTime es null")
    void shouldFailWhenStartTimeIsNull() {
        // Given
        validGroupSession.setStartTime(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupSession);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Start time is required");
    }

    @Test
    @DisplayName("Debe fallar cuando endTime es null")
    void shouldFailWhenEndTimeIsNull() {
        // Given
        validGroupSession.setEndTime(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupSession);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("End time is required");
    }

    @Test
    @DisplayName("Debe permitir classroom null")
    void shouldAllowNullClassroom() {
        // Given
        validGroupSession.setClassroom(null);

        // When
        em.persist(validGroupSession);
        em.flush();

        // Then
        GroupSession found = em.find(GroupSession.class, validGroupSession.getId());
        assertThat(found.getClassroom()).isNull();
    }

    @Test
    @DisplayName("Debe fallar cuando classroom excede 50 caracteres")
    void shouldFailWhenClassroomExceedsMaxLength() {
        // Given
        String longClassroom = "A".repeat(51);
        validGroupSession.setClassroom(longClassroom);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupSession);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Classroom must not exceed 50 characters");
    }

    @Test
    @DisplayName("Debe aceptar todos los valores de DayOfWeek")
    void shouldAcceptAllDayOfWeekValues() {
        int hour = 8;
        for (DayOfWeek day : DayOfWeek.values()) {
            GroupSession session = GroupSession.builder()
                    .courseGroup(testCourseGroup)
                    .dayOfWeek(day)
                    .startTime(LocalTime.of(hour, 0))
                    .endTime(LocalTime.of(hour + 2, 0))
                    .classroom("Aula Test")
                    .build();

            em.persist(session);
            em.flush();

            assertThat(session.getDayOfWeek()).isEqualTo(day);
            em.clear();
            hour++; // Cambiar hora para evitar conflictos con el índice único
        }
    }

    @Test
    @DisplayName("Debe fallar cuando endTime es anterior a startTime")
    void shouldFailWhenEndTimeIsBeforeStartTime() {
        // Given
        validGroupSession.setStartTime(LocalTime.of(14, 0));
        validGroupSession.setEndTime(LocalTime.of(12, 0)); // Anterior a startTime

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupSession);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    @DisplayName("Debe fallar cuando endTime es igual a startTime")
    void shouldFailWhenEndTimeEqualsStartTime() {
        // Given
        LocalTime sameTime = LocalTime.of(10, 0);
        validGroupSession.setStartTime(sameTime);
        validGroupSession.setEndTime(sameTime);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupSession);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    @DisplayName("Debe permitir sesiones con diferentes duraciones")
    void shouldAllowSessionsWithDifferentDurations() {
        // Test sesión de 1 hora
        GroupSession oneHourSession = GroupSession.builder()
                .courseGroup(testCourseGroup)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();

        // Test sesión de 3 horas
        GroupSession threeHourSession = GroupSession.builder()
                .courseGroup(testCourseGroup)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        // Test sesión con minutos
        GroupSession withMinutesSession = GroupSession.builder()
                .courseGroup(testCourseGroup)
                .dayOfWeek(DayOfWeek.THURSDAY)
                .startTime(LocalTime.of(10, 30))
                .endTime(LocalTime.of(12, 15))
                .build();

        em.persist(oneHourSession);
        em.persist(threeHourSession);
        em.persist(withMinutesSession);
        em.flush();

        assertThat(oneHourSession.getId()).isNotNull();
        assertThat(threeHourSession.getId()).isNotNull();
        assertThat(withMinutesSession.getId()).isNotNull();
    }

    @Test
    @DisplayName("Debe respetar constraint único de courseGroup + dayOfWeek + startTime")
    void shouldEnforceUniqueConstraint() {
        // Given
        em.persist(validGroupSession);
        em.flush();

        // Intentar crear otra sesión con la misma combinación
        GroupSession duplicateSession = GroupSession.builder()
                .courseGroup(testCourseGroup) // mismo grupo
                .dayOfWeek(DayOfWeek.MONDAY) // mismo día
                .startTime(LocalTime.of(10, 0)) // misma hora de inicio
                .endTime(LocalTime.of(13, 0)) // diferente hora de fin (no importa)
                .classroom("Aula 999") // diferente aula (no importa)
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(duplicateSession);
            em.flush();
        }).satisfies(throwable -> {
            String message = throwable.getMessage();
            assertThat(message.toLowerCase()).contains("unique");
        });
    }

    @Test
    @DisplayName("Debe permitir misma hora en diferentes días para el mismo grupo")
    void shouldAllowSameTimeOnDifferentDays() {
        // Given - Sesión el lunes
        em.persist(validGroupSession);

        // When - Misma hora el miércoles
        GroupSession wednesdaySession = GroupSession.builder()
                .courseGroup(testCourseGroup) // mismo grupo
                .dayOfWeek(DayOfWeek.WEDNESDAY) // diferente día
                .startTime(LocalTime.of(10, 0)) // misma hora
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 301")
                .build();

        em.persist(wednesdaySession);
        em.flush();

        // Then
        assertThat(validGroupSession.getId()).isNotNull();
        assertThat(wednesdaySession.getId()).isNotNull();
        assertThat(validGroupSession.getId()).isNotEqualTo(wednesdaySession.getId());
    }

    @Test
    @DisplayName("Debe permitir mismo día y hora para diferentes grupos")
    void shouldAllowSameDayTimeForDifferentGroups() {
        // Given - Crear otro grupo
        Subject anotherSubject = Subject.builder()
                .name("Compiladores")
                .major("Ingeniería Informática")
                .courseYear(4)
                .build();
        em.persist(anotherSubject);

        CourseGroup anotherGroup = CourseGroup.builder()
                .subject(anotherSubject)
                .teacher(testTeacher)
                .status(CourseGroupStatus.ACTIVE)
                .type(CourseGroupType.REGULAR)
                .price(BigDecimal.valueOf(300.00))
                .build();
        em.persist(anotherGroup);

        // Primera sesión
        em.persist(validGroupSession);

        // When - Mismo día y hora para otro grupo
        GroupSession anotherGroupSession = GroupSession.builder()
                .courseGroup(anotherGroup) // diferente grupo
                .dayOfWeek(DayOfWeek.MONDAY) // mismo día
                .startTime(LocalTime.of(10, 0)) // misma hora
                .endTime(LocalTime.of(12, 0))
                .classroom("Aula 302") // diferente aula (recomendado)
                .build();

        em.persist(anotherGroupSession);
        em.flush();

        // Then
        assertThat(validGroupSession.getId()).isNotNull();
        assertThat(anotherGroupSession.getId()).isNotNull();
    }

    @Test
    @DisplayName("Debe manejar correctamente la relación con CourseGroup")
    void shouldCorrectlyHandleCourseGroupRelation() {
        // When
        em.persist(validGroupSession);
        em.flush();
        em.clear();

        // Then
        GroupSession found = em.find(GroupSession.class, validGroupSession.getId());
        assertThat(found.getCourseGroup()).isNotNull();
        assertThat(found.getCourseGroup().getId()).isEqualTo(testCourseGroup.getId());
        assertThat(found.getCourseGroup().getSubject().getName()).isEqualTo("Redes de Computadores");
    }

    @Test
    @DisplayName("Debe verificar cascada al eliminar CourseGroup")
    void shouldCascadeDeleteWhenCourseGroupIsDeleted() {
        // Given
        testCourseGroup.addGroupSession(validGroupSession);
        em.persist(validGroupSession);
        em.flush();

        Long sessionId = validGroupSession.getId();
        Long groupId = testCourseGroup.getId();

        // When - Eliminar el grupo
        CourseGroup group = em.find(CourseGroup.class, groupId);
        em.remove(group);
        em.flush();

        // Then
        assertThat(em.find(CourseGroup.class, groupId)).isNull();
        assertThat(em.find(GroupSession.class, sessionId)).isNull(); // Session eliminada por cascada
    }

    @Test
    @DisplayName("Debe verificar equals y hashCode basados en ID")
    void shouldImplementEqualsAndHashCodeBasedOnId() {
        // Given
        GroupSession session1 = GroupSession.builder()
                .courseGroup(testCourseGroup)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        GroupSession session2 = GroupSession.builder()
                .courseGroup(testCourseGroup)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        // Before persistence
        assertThat(session1).isNotEqualTo(session2);

        // Persist both
        em.persist(session1);
        em.persist(session2);
        em.flush();

        // Con IDs asignados
        assertThat(session1).isNotEqualTo(session2);
        assertThat(session1.getId()).isNotEqualTo(session2.getId());

        // Buscar la misma sesión
        GroupSession sameSession = em.find(GroupSession.class, session1.getId());
        assertThat(session1).isEqualTo(sameSession);
    }

    @Test
    @DisplayName("Debe verificar toString no incluye courseGroup")
    void shouldNotIncludeCourseGroupInToString() {
        // When
        String toString = validGroupSession.toString();

        // Then
        assertThat(toString).doesNotContain("courseGroup");
        assertThat(toString).contains("GroupSession");
        assertThat(toString).contains("dayOfWeek=");
        assertThat(toString).contains("startTime=");
        assertThat(toString).contains("endTime=");
        assertThat(toString).contains("classroom=");
    }

    @Test
    @DisplayName("Debe gestionar correctamente el uso con métodos helper de CourseGroup")
    void shouldWorkCorrectlyWithCourseGroupHelperMethods() {
        // Given
        GroupSession session = GroupSession.builder()
                .dayOfWeek(DayOfWeek.FRIDAY)
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(18, 0))
                .classroom("Lab 101")
                .build();

        // When
        testCourseGroup.addGroupSession(session);
        em.persist(session);
        em.flush();
        em.clear();

        // Then
        CourseGroup foundGroup = em.find(CourseGroup.class, testCourseGroup.getId());
        assertThat(foundGroup.getGroupSessions()).hasSize(1);

        GroupSession foundSession = foundGroup.getGroupSessions().iterator().next();
        assertThat(foundSession.getCourseGroup()).isEqualTo(foundGroup);
        assertThat(foundSession.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
    }

    @Test
    @DisplayName("Debe permitir sesiones en horarios extremos")
    void shouldAllowExtremeTimes() {
        // Sesión muy temprano
        GroupSession earlySession = GroupSession.builder()
                .courseGroup(testCourseGroup)
                .dayOfWeek(DayOfWeek.SATURDAY)
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .build();

        // Sesión muy tarde
        GroupSession lateSession = GroupSession.builder()
                .courseGroup(testCourseGroup)
                .dayOfWeek(DayOfWeek.SUNDAY)
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(22, 0))
                .build();

        em.persist(earlySession);
        em.persist(lateSession);
        em.flush();

        assertThat(earlySession.getId()).isNotNull();
        assertThat(lateSession.getId()).isNotNull();
    }
}
