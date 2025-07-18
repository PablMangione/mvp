package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Admin;
import com.acainfo.mvp.model.Admin.PermissionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de persistencia de Admin.
 * Proporciona métodos para autenticación y gestión de administradores.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Busca un administrador por email.
     * Usado principalmente para autenticación.
     *
     * @param email email del administrador
     * @return Optional con el admin si existe
     */
    Optional<Admin> findByEmail(String email);

    /**
     * Verifica si existe un admin con el email dado.
     * Útil para validar duplicados en registro.
     *
     * @param email email a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByEmail(String email);

    /**
     * Busca un admin activo por email.
     * Solo retorna admins con isActive = true.
     *
     * @param email email del administrador
     * @return Optional con el admin activo si existe
     */
    Optional<Admin> findByEmailAndIsActiveTrue(String email);

    /**
     * Obtiene todos los administradores activos.
     *
     * @return lista de admins activos
     */
    List<Admin> findByIsActiveTrue();

    /**
     * Obtiene administradores por nivel de permisos.
     *
     * @param permissionLevel nivel de permisos a buscar
     * @return lista de admins con ese nivel de permisos
     */
    List<Admin> findByPermissionLevel(PermissionLevel permissionLevel);

    /**
     * Obtiene administradores activos por nivel de permisos.
     *
     * @param permissionLevel nivel de permisos
     * @return lista de admins activos con ese nivel
     */
    List<Admin> findByPermissionLevelAndIsActiveTrue(PermissionLevel permissionLevel);

    /**
     * Cuenta el número de administradores activos.
     * Útil para validaciones de negocio (ej: no dejar el sistema sin admins).
     *
     * @return número de admins activos
     */
    Long countByIsActiveTrue();

    /**
     * Busca administradores cuyo nombre contenga el texto dado.
     * Búsqueda case-insensitive para la gestión de admins.
     *
     * @param name texto a buscar en el nombre
     * @return lista de admins que coinciden
     */
    @Query("SELECT a FROM Admin a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%')) AND a.isActive = true")
    List<Admin> findActiveAdminsByNameContaining(@Param("name") String name);
}