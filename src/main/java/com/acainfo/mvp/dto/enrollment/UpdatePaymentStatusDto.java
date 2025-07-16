package com.acainfo.mvp.dto.enrollment;

import com.acainfo.mvp.model.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusDto {

    @NotNull(message = "Payment status is required")
    private PaymentStatus status;

    private String transactionId; // Para futuras integraciones
}
