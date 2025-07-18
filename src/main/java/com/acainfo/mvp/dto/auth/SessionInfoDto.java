package com.acainfo.mvp.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

//Para verificar permisos
@Data
@Builder
public class SessionInfoDto {
    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private int maxInactiveInterval;
}
