package com.acainfo.mvp.model;

import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_group")
@Getter
@Setter
@ToString(exclude = {"enrollments", "groupSessions", "subject", "teacher"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseGroup extends BaseEntity {

    @NotNull(message = "Subject is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CourseGroupStatus status = CourseGroupStatus.PLANNED;

    @NotNull(message = "Type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    private CourseGroupType type = CourseGroupType.REGULAR;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Max capacity is required")
    @Min(value = 1, message = "Max capacity must be at least 1")
    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private Integer maxCapacity = 30;

    @OneToMany(mappedBy = "courseGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "courseGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<GroupSession> groupSessions = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    protected void onPrePersist() {
        if (status == null) {
            status = CourseGroupStatus.PLANNED;
        }
        if (type == null) {
            type = CourseGroupType.REGULAR;
        }
        if (maxCapacity == null) {
            maxCapacity = 30;
        }
    }

    // Helper methods for managing bidirectional relationships
    public void addEnrollment(Enrollment enrollment) {
        enrollments.add(enrollment);
        enrollment.setCourseGroup(this);
    }

    public void removeEnrollment(Enrollment enrollment) {
        enrollments.remove(enrollment);
        enrollment.setCourseGroup(null);
    }

    public void addGroupSession(GroupSession groupSession) {
        groupSessions.add(groupSession);
        groupSession.setCourseGroup(this);
    }

    public void removeGroupSession(GroupSession groupSession) {
        groupSessions.remove(groupSession);
        groupSession.setCourseGroup(null);
    }

    // Helper method to check if group has capacity
    public boolean hasCapacity() {
        return enrollments.size() < maxCapacity;
    }

    // Helper method to get available spots
    public int getAvailableSpots() {
        return Math.max(0, maxCapacity - enrollments.size());
    }
}
