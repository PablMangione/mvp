package com.acainfo.mvp.dto.enrollment;

import com.acainfo.mvp.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponseDto {
    private Long enrollmentId;
    private boolean success;
    private String message;
    private PaymentStatus paymentStatus;
}
