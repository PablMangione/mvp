package com.acainfo.mvp.model;

import com.acainfo.mvp.model.enums.DayOfWeek;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "group_session", indexes = {
        @Index(name = "idx_session_group_day", columnList = "course_group_id,day_of_week,start_time", unique = true)
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSession extends BaseEntity {

    @NotNull(message = "Course group is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_group_id", nullable = false)
    @ToString.Exclude
    private CourseGroup courseGroup;

    @NotNull(message = "Day of week is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Size(max = 50, message = "Classroom must not exceed 50 characters")
    @Column(name = "classroom", length = 50)
    private String classroom;

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @AssertTrue(message = "End time must be after start time")
    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null) {
            return true; // Let @NotNull handle null validation
        }
        return endTime.isAfter(startTime);
    }
}
