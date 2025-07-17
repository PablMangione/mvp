package com.acainfo.mvp.model;

import com.acainfo.mvp.model.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollment", indexes = {
        @Index(name = "idx_enrollment_unique", columnList = "student_id,course_group_id", unique = true)
})
@Getter
@Setter
@ToString(exclude = {"student", "courseGroup"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment extends BaseEntity {

    @NotNull(message = "Student is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull(message = "Course group is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_group_id", nullable = false)
    private CourseGroup courseGroup;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

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
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
    }
}
