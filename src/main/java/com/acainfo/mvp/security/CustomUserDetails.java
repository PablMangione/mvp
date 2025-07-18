package com.acainfo.mvp.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementación personalizada de UserDetails para trabajar con nuestras entidades.
 * Permite autenticar tanto estudiantes como profesores con Spring Security.
 */
public class CustomUserDetails implements UserDetails {

    // Getters adicionales para acceder a información personalizada
    @Getter
    private final Long id;
    @Getter
    private final String email;
    private final String password;
    @Getter
    private final String name;
    @Getter
    private final String role;
    private final boolean enabled;

    public CustomUserDetails(Long id, String email, String password, String name, String role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.enabled = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Usamos email como username
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}