package com.acainfo.mvp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subject", indexes = {
        @Index(name = "idx_subject_name_major", columnList = "name,major", unique = true)
})
@Getter
@Setter
@ToString(exclude = {"courseGroups", "groupRequests"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Major is required")
    @Size(max = 100, message = "Major must not exceed 100 characters")
    @Column(name = "major", nullable = false, length = 100)
    private String major;

    @NotNull(message = "Course year is required")
    @Min(value = 1, message = "Course year must be at least 1")
    @Max(value = 6, message = "Course year must not exceed 6")
    @Column(name = "course_year", nullable = false)
    private Integer courseYear;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CourseGroup> courseGroups = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<GroupRequest> groupRequests = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // Helper methods for managing bidirectional relationships
    public void addCourseGroup(CourseGroup courseGroup) {
        courseGroups.add(courseGroup);
        courseGroup.setSubject(this);
    }

    public void removeCourseGroup(CourseGroup courseGroup) {
        courseGroups.remove(courseGroup);
        courseGroup.setSubject(null);
    }

    public void addGroupRequest(GroupRequest groupRequest) {
        groupRequests.add(groupRequest);
        groupRequest.setSubject(this);
    }

    public void removeGroupRequest(GroupRequest groupRequest) {
        groupRequests.remove(groupRequest);
        groupRequest.setSubject(null);
    }
}
