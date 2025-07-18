package com.acainfo.mvp.dto.auth;

import lombok.Builder;
import lombok.Data;

//Para logout
@Data
@Builder
public class LogoutResponseDto {
    private boolean success;
    private String message;
}
