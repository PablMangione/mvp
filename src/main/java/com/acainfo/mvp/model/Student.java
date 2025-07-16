package com.acainfo.mvp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "student", indexes = {
        @Index(name = "idx_student_email", columnList = "email", unique = true)
})
@Getter
@Setter
@ToString(exclude = {"enrollments", "groupRequests","password"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    @NotBlank(message = "Major is required")
    @Size(max = 100, message = "Major must not exceed 100 characters")
    @Column(name = "major", nullable = false, length = 100)
    private String major;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<GroupRequest> groupRequests = new HashSet<>();

    // Helper methods for managing bidirectional relationships
    public void addEnrollment(Enrollment enrollment) {
        enrollments.add(enrollment);
        enrollment.setStudent(this);
    }

    public void removeEnrollment(Enrollment enrollment) {
        enrollments.remove(enrollment);
        enrollment.setStudent(null);
    }

    public void addGroupRequest(GroupRequest groupRequest) {
        groupRequests.add(groupRequest);
        groupRequest.setStudent(this);
    }

    public void removeGroupRequest(GroupRequest groupRequest) {
        groupRequests.remove(groupRequest);
        groupRequest.setStudent(null);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}