package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actividad reciente del sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private Long id;
    private String type; // ENROLLMENT, CANCELLATION, GROUP_REQUEST, USER_REGISTRATION
    private String description;
    private String timestamp;
    private Long userId;
    private String userName;
}