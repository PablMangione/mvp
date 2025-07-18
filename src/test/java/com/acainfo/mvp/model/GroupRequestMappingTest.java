package com.acainfo.mvp.model;

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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de mapeo JPA para la entidad GroupRequest.
 * Valida constraints, atributos, relaciones y comportamiento de persistencia.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("GroupRequest Entity Mapping Tests")
@Transactional
class GroupRequestMappingTest {

    @Autowired
    private EntityManager em;

    private GroupRequest validGroupRequest;
    private Student testStudent;
    private Subject testSubject;

    @BeforeEach
    void setUp() {
        // Crear entidades relacionadas
        testStudent = Student.builder()
                .name("Pablo Fernández")
                .email("pablo.fernandez@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();

        testSubject = Subject.builder()
                .name("Inteligencia Artificial")
                .major("Ingeniería Informática")
                .courseYear(3)
                .build();

        // Persistir las entidades relacionadas
        em.persist(testStudent);
        em.persist(testSubject);
        em.flush();

        // Crear un GroupRequest válido
        validGroupRequest = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .status(RequestStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Debe persistir un GroupRequest válido con todos los campos")
    void shouldPersistValidGroupRequest() {
        // When
        em.persist(validGroupRequest);
        em.flush();

        // Then
        assertThat(validGroupRequest.getId()).isNotNull();
        assertThat(validGroupRequest.getCreatedAt()).isNotNull();
        assertThat(validGroupRequest.getUpdatedAt()).isNull();

        // Verificar que se puede recuperar
        GroupRequest found = em.find(GroupRequest.class, validGroupRequest.getId());
        assertThat(found).isNotNull();
        assertThat(found.getStudent().getId()).isEqualTo(testStudent.getId());
        assertThat(found.getSubject().getId()).isEqualTo(testSubject.getId());
        assertThat(found.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("Debe asignar automáticamente ID y createdAt")
    void shouldAutoAssignIdAndCreatedAt() {
        // Given
        assertThat(validGroupRequest.getId()).isNull();
        assertThat(validGroupRequest.getCreatedAt()).isNull();

        // When
        em.persist(validGroupRequest);
        em.flush();

        // Then
        assertThat(validGroupRequest.getId()).isNotNull();
        assertThat(validGroupRequest.getCreatedAt()).isNotNull();
        assertThat(validGroupRequest.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Debe actualizar updatedAt en modificaciones")
    void shouldUpdateUpdatedAtOnModification() {
        // Given
        em.persist(validGroupRequest);
        em.flush();
        assertThat(validGroupRequest.getUpdatedAt()).isNull();

        // When
        validGroupRequest.setStatus(RequestStatus.APPROVED);
        em.flush();

        // Then
        assertThat(validGroupRequest.getUpdatedAt()).isNotNull();
        assertThat(validGroupRequest.getUpdatedAt()).isAfter(validGroupRequest.getCreatedAt());
    }

    @Test
    @DisplayName("Debe fallar cuando student es null")
    void shouldFailWhenStudentIsNull() {
        // Given
        validGroupRequest.setStudent(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupRequest);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Student is required");
    }

    @Test
    @DisplayName("Debe fallar cuando subject es null")
    void shouldFailWhenSubjectIsNull() {
        // Given
        validGroupRequest.setSubject(null);

        // When/Then
        assertThatThrownBy(() -> {
            em.persist(validGroupRequest);
            em.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Subject is required");
    }

    @Test
    @DisplayName("Debe establecer status PENDING por defecto si es null")
    void shouldSetDefaultStatusWhenNull() {
        // Given
        GroupRequest requestWithoutStatus = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .build();
        // No establecemos status

        // When
        em.persist(requestWithoutStatus);
        em.flush();

        // Then
        assertThat(requestWithoutStatus.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("Debe aceptar todos los valores de RequestStatus")
    void shouldAcceptAllRequestStatusValues() {
        for (RequestStatus status : RequestStatus.values()) {
            // Crear nuevo estudiante y asignatura para cada iteración
            Student student = Student.builder()
                    .name("Student " + status)
                    .email(status + "@test.com")
                    .password("password123")
                    .major("Ingeniería Informática")
                    .build();
            em.persist(student);

            Subject subject = Subject.builder()
                    .name("Subject " + status)
                    .major("Ingeniería Informática")
                    .courseYear(1)
                    .build();
            em.persist(subject);

            GroupRequest request = GroupRequest.builder()
                    .student(student)
                    .subject(subject)
                    .status(status)
                    .build();

            em.persist(request);
            em.flush();

            assertThat(request.getStatus()).isEqualTo(status);
            em.clear();
        }
    }

    @Test
    @DisplayName("Debe permitir múltiples solicitudes del mismo estudiante para diferentes asignaturas")
    void shouldAllowMultipleRequestsFromSameStudentForDifferentSubjects() {
        // Given - Crear otra asignatura
        Subject anotherSubject = Subject.builder()
                .name("Sistemas Operativos")
                .major("Ingeniería Informática")
                .courseYear(2)
                .build();
        em.persist(anotherSubject);

        // When - Crear solicitudes del mismo estudiante para diferentes asignaturas
        GroupRequest request1 = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .status(RequestStatus.PENDING)
                .build();

        GroupRequest request2 = GroupRequest.builder()
                .student(testStudent) // mismo estudiante
                .subject(anotherSubject) // diferente asignatura
                .status(RequestStatus.PENDING)
                .build();

        em.persist(request1);
        em.persist(request2);
        em.flush();

        // Then
        assertThat(request1.getId()).isNotNull();
        assertThat(request2.getId()).isNotNull();
        assertThat(request1.getId()).isNotEqualTo(request2.getId());
    }

    @Test
    @DisplayName("Debe permitir múltiples solicitudes de diferentes estudiantes para la misma asignatura")
    void shouldAllowMultipleRequestsFromDifferentStudentsForSameSubject() {
        // Given - Crear otro estudiante
        Student anotherStudent = Student.builder()
                .name("Laura Ruiz")
                .email("laura.ruiz@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();
        em.persist(anotherStudent);

        // When - Crear solicitudes de diferentes estudiantes para la misma asignatura
        GroupRequest request1 = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .status(RequestStatus.PENDING)
                .build();

        GroupRequest request2 = GroupRequest.builder()
                .student(anotherStudent) // diferente estudiante
                .subject(testSubject) // misma asignatura
                .status(RequestStatus.PENDING)
                .build();

        em.persist(request1);
        em.persist(request2);
        em.flush();

        // Then
        assertThat(request1.getId()).isNotNull();
        assertThat(request2.getId()).isNotNull();
        assertThat(request1.getId()).isNotEqualTo(request2.getId());
    }

    @Test
    @DisplayName("Debe permitir múltiples solicitudes del mismo estudiante para la misma asignatura con diferentes estados")
    void shouldAllowMultipleRequestsWithDifferentStatuses() {
        // Given - Primera solicitud rechazada
        GroupRequest rejectedRequest = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .status(RequestStatus.REJECTED)
                .build();
        em.persist(rejectedRequest);
        em.flush();

        // When - Nueva solicitud pendiente
        GroupRequest newRequest = GroupRequest.builder()
                .student(testStudent) // mismo estudiante
                .subject(testSubject) // misma asignatura
                .status(RequestStatus.PENDING) // diferente estado
                .build();

        em.persist(newRequest);
        em.flush();

        // Then - Ambas solicitudes pueden coexistir
        assertThat(rejectedRequest.getId()).isNotNull();
        assertThat(newRequest.getId()).isNotNull();
        assertThat(rejectedRequest.getId()).isNotEqualTo(newRequest.getId());
    }

    @Test
    @DisplayName("Debe manejar correctamente la relación con Student")
    void shouldCorrectlyHandleStudentRelation() {
        // When
        em.persist(validGroupRequest);
        em.flush();
        em.clear();

        // Then
        GroupRequest found = em.find(GroupRequest.class, validGroupRequest.getId());
        assertThat(found.getStudent()).isNotNull();
        assertThat(found.getStudent().getId()).isEqualTo(testStudent.getId());
        assertThat(found.getStudent().getName()).isEqualTo("Pablo Fernández");
    }

    @Test
    @DisplayName("Debe manejar correctamente la relación con Subject")
    void shouldCorrectlyHandleSubjectRelation() {
        // When
        em.persist(validGroupRequest);
        em.flush();
        em.clear();

        // Then
        GroupRequest found = em.find(GroupRequest.class, validGroupRequest.getId());
        assertThat(found.getSubject()).isNotNull();
        assertThat(found.getSubject().getId()).isEqualTo(testSubject.getId());
        assertThat(found.getSubject().getName()).isEqualTo("Inteligencia Artificial");
    }

    @Test
    @DisplayName("Debe verificar cascada al eliminar Student")
    void shouldCascadeDeleteWhenStudentIsDeleted() {
        // Given
        testStudent.addGroupRequest(validGroupRequest);
        em.persist(validGroupRequest);
        em.flush();

        Long requestId = validGroupRequest.getId();
        Long studentId = testStudent.getId();
        Long subjectId = testSubject.getId();

        // When - Eliminar el estudiante
        Student student = em.find(Student.class, studentId);
        em.remove(student);
        em.flush();

        // Then
        assertThat(em.find(Student.class, studentId)).isNull();
        assertThat(em.find(GroupRequest.class, requestId)).isNull(); // Request eliminado por cascada
        assertThat(em.find(Subject.class, subjectId)).isNotNull(); // Subject sigue existiendo
    }

    @Test
    @DisplayName("Debe verificar cascada al eliminar Subject")
    void shouldCascadeDeleteWhenSubjectIsDeleted() {
        // Given
        testSubject.addGroupRequest(validGroupRequest);
        em.persist(validGroupRequest);
        em.flush();

        Long requestId = validGroupRequest.getId();
        Long studentId = testStudent.getId();
        Long subjectId = testSubject.getId();

        // When - Eliminar la asignatura
        Subject subject = em.find(Subject.class, subjectId);
        em.remove(subject);
        em.flush();

        // Then
        assertThat(em.find(Subject.class, subjectId)).isNull();
        assertThat(em.find(GroupRequest.class, requestId)).isNull(); // Request eliminado por cascada
        assertThat(em.find(Student.class, studentId)).isNotNull(); // Student sigue existiendo
    }

    @Test
    @DisplayName("Debe verificar equals y hashCode basados en ID")
    void shouldImplementEqualsAndHashCodeBasedOnId() {
        // Given
        GroupRequest request1 = GroupRequest.builder()
                .student(testStudent)
                .subject(testSubject)
                .status(RequestStatus.PENDING)
                .build();

        Student student2 = Student.builder()
                .name("Carmen Vega")
                .email("carmen.vega@universidad.edu")
                .password("password123")
                .major("Ingeniería Informática")
                .build();
        em.persist(student2);

        GroupRequest request2 = GroupRequest.builder()
                .student(student2)
                .subject(testSubject)
                .status(RequestStatus.APPROVED)
                .build();

        // Before persistence
        assertThat(request1).isNotEqualTo(request2);

        // Persist both
        em.persist(request1);
        em.persist(request2);
        em.flush();

        // Con IDs asignados
        assertThat(request1).isNotEqualTo(request2);
        assertThat(request1.getId()).isNotEqualTo(request2.getId());

        // Buscar el mismo request
        GroupRequest sameRequest = em.find(GroupRequest.class, request1.getId());
        assertThat(request1).isEqualTo(sameRequest);
    }

    @Test
    @DisplayName("Debe verificar que student y subject no se incluyen en toString")
    void shouldNotIncludeRelationsInToString() {
        // When
        String toString = validGroupRequest.toString();

        // Then
        assertThat(toString).doesNotContain("student");
        assertThat(toString).doesNotContain("subject");
        assertThat(toString).contains("GroupRequest");
        assertThat(toString).contains("status=");
    }

    @Test
    @DisplayName("Debe gestionar correctamente el uso con métodos helper de Student")
    void shouldWorkCorrectlyWithStudentHelperMethods() {
        // Given
        GroupRequest request = GroupRequest.builder()
                .subject(testSubject)
                .status(RequestStatus.PENDING)
                .build();

        testStudent.addGroupRequest(request);
        em.persist(request);
        em.flush();
        em.clear();

        // Then
        Student foundStudent = em.find(Student.class, testStudent.getId());
        assertThat(foundStudent.getGroupRequests()).hasSize(1);

        GroupRequest foundRequest = foundStudent.getGroupRequests().iterator().next();
        assertThat(foundRequest.getStudent()).isEqualTo(foundStudent);
        assertThat(foundRequest.getSubject().getId()).isEqualTo(testSubject.getId());
    }

    @Test
    @DisplayName("Debe gestionar correctamente el uso con métodos helper de Subject")
    void shouldWorkCorrectlyWithSubjectHelperMethods() {
        // Given
        GroupRequest request = GroupRequest.builder()
                .student(testStudent)
                .status(RequestStatus.PENDING)
                .build();

        testSubject.addGroupRequest(request);
        em.persist(request);
        em.flush();
        em.clear();

        // Then
        Subject foundSubject = em.find(Subject.class, testSubject.getId());
        assertThat(foundSubject.getGroupRequests()).hasSize(1);

        GroupRequest foundRequest = foundSubject.getGroupRequests().iterator().next();
        assertThat(foundRequest.getSubject()).isEqualTo(foundSubject);
        assertThat(foundRequest.getStudent().getId()).isEqualTo(testStudent.getId());
    }

    @Test
    @DisplayName("Debe permitir cambiar el estado de la solicitud")
    void shouldAllowStatusChange() {
        // Given
        em.persist(validGroupRequest);
        em.flush();
        assertThat(validGroupRequest.getStatus()).isEqualTo(RequestStatus.PENDING);

        // When - Aprobar la solicitud
        validGroupRequest.setStatus(RequestStatus.APPROVED);
        em.flush();

        // Then
        GroupRequest found = em.find(GroupRequest.class, validGroupRequest.getId());
        assertThat(found.getStatus()).isEqualTo(RequestStatus.APPROVED);

        // When - Rechazar la solicitud
        validGroupRequest.setStatus(RequestStatus.REJECTED);
        em.flush();

        // Then
        found = em.find(GroupRequest.class, validGroupRequest.getId());
        assertThat(found.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }
}