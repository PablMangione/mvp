package com.acainfo.mvp.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para crear profesor (admin)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeacherDto {
    @jakarta.validation.constraints.NotBlank(message = "Name is required")
    @jakarta.validation.constraints.Size(max = 100)
    private String name;

    @jakarta.validation.constraints.NotBlank(message = "Email is required")
    @jakarta.validation.constraints.Email(message = "Email must be valid")
    @jakarta.validation.constraints.Size(max = 150)
    private String email;

    @jakarta.validation.constraints.NotBlank(message = "Password is required")
    @jakarta.validation.constraints.Size(min = 8)
    private String password;
}
