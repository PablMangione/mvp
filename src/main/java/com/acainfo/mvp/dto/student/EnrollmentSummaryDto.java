package com.acainfo.mvp.dto.student;

import com.acainfo.mvp.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentSummaryDto {
    private Long enrollmentId;
    private Long courseGroupId;
    private String subjectName;
    private String teacherName;
    private String groupType;
    private String groupStatus;
    private LocalDateTime createdAt;
    private PaymentStatus paymentStatus;
}
