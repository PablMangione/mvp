package com.acainfo.mvp.dto.student;

import com.acainfo.mvp.dto.common.BaseDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

// DTO básico de estudiante (sin información sensible)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDto extends BaseDto {
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @Email(message = "El email debe ser válido")
    private String email;

    @Size(max = 100, message = "La carrera no puede exceder 100 caracteres")
    private String major;
}
