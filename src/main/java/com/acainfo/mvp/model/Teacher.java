package com.acainfo.mvp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teacher", indexes = {
        @Index(name = "idx_teacher_email", columnList = "email", unique = true)
})
@Getter
@Setter
@ToString(exclude = {"courseGroups","password"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher extends BaseEntity {

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

    @OneToMany(mappedBy = "teacher",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}, // ¡sin REMOVE!
            orphanRemoval = false)
    private Set<CourseGroup> courseGroups = new HashSet<>();

    @PreRemove
    private void dissociateGroups() {
        courseGroups.forEach(cg -> cg.setTeacher(null));
    }

    // Helper methods for managing bidirectional relationships
    public void addCourseGroup(CourseGroup courseGroup) {
        courseGroups.add(courseGroup);
        courseGroup.setTeacher(this);
    }

    public void removeCourseGroup(CourseGroup courseGroup) {
        courseGroups.remove(courseGroup);
        courseGroup.setTeacher(null);
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
