package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para reporte de pagos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReportDto {
    private String period;
    private BigDecimal totalAmount;
    private BigDecimal pendingAmount;
    private BigDecimal paidAmount;
    private BigDecimal overdueAmount;
    private List<StudentPayment> byStudent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentPayment {
        private Long studentId;
        private String studentName;
        private BigDecimal amount;
        private String status;
    }
}