package com.acainfo.mvp.dto.auth;

import lombok.Builder;
import lombok.Data;

//Para verificar sesión actual
@Data
@Builder
public class CurrentUserDto {
    private Long id;
    private String email;
    private String name;
    private String role; // STUDENT, TEACHER, ADMIN
    private boolean authenticated;
}