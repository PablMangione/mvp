package com.acainfo.mvp.dto.enrollment;

import com.acainfo.mvp.dto.common.BaseDto;
import com.acainfo.mvp.model.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

// DTO para inscripción
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentDto extends BaseDto {
    private Long studentId;
    private String studentName;
    private Long courseGroupId;
    private String subjectName;
    private String teacherName;
    private LocalDateTime enrollmentDate;
    private PaymentStatus paymentStatus;
}
