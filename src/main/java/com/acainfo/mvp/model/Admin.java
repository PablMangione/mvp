package com.acainfo.mvp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad que representa un administrador del sistema.
 * Los administradores tienen permisos completos para gestionar:
 * - Alumnos (alta/baja)
 * - Profesores (alta/baja)
 * - Asignaturas (creación/eliminación)
 * - Grupos (creación/cambio de estado)
 */
@Entity
@Table(name = "admin", indexes = {
        @Index(name = "idx_admin_email", columnList = "email", unique = true)
})
@Getter
@Setter
@ToString(exclude = {"password"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    /**
     * Nivel de permisos del administrador.
     * Para futuras iteraciones donde podríamos tener diferentes niveles de admin.
     * Por ahora todos los admins tienen permisos completos (FULL).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false)
    @Builder.Default
    private PermissionLevel permissionLevel = PermissionLevel.FULL;

    /**
     * Indica si el administrador está activo.
     * Permite desactivar admins sin eliminarlos de la BD.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Notas internas sobre el administrador.
     * Útil para registrar quién dio de alta al admin o cualquier observación.
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Enum para niveles de permisos de administrador.
     * Preparado para futuras iteraciones con diferentes niveles de acceso.
     */
    public enum PermissionLevel {
        FULL,        // Acceso completo al sistema
        ACADEMIC,    // Solo gestión académica (asignaturas, grupos)
        USERS,       // Solo gestión de usuarios (alumnos, profesores)
        READONLY     // Solo lectura (para auditoría)
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Valida si el admin puede realizar una acción específica.
     * Para la iteración 1, todos los admins activos pueden hacer cualquier cosa
     */
    public boolean canPerformAction() {
        return isActive && permissionLevel == PermissionLevel.FULL;
    }
}