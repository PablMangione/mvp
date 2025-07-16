package com.acainfo.mvp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para respuesta de login
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private Long id;
    private String email;
    private String name;
    private String role; // STUDENT, TEACHER, ADMIN
    private String token; // JWT token para futuras fases
}
