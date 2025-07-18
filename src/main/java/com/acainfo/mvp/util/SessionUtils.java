package com.acainfo.mvp.util;

import com.acainfo.mvp.security.CustomUserDetails;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utilidades para el manejo de sesiones HTTP y contexto de seguridad.
 * Centraliza la lógica de acceso a información del usuario autenticado.
 */
@Component
public class SessionUtils {

    private static final String USER_ID_KEY = "userId";
    private static final String USER_ROLE_KEY = "userRole";
    private static final String USER_EMAIL_KEY = "userEmail";
    private static final String USER_NAME_KEY = "userName";

    /**
     * Obtiene la autenticación actual del contexto de seguridad.
     */
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Verifica si hay un usuario autenticado.
     */
    public boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails;
    }

    /**
     * Obtiene el CustomUserDetails del usuario autenticado.
     */
    public CustomUserDetails getCurrentUserDetails() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) auth.getPrincipal();
        }
        return null;
    }

    /**
     * Obtiene el ID del usuario actual.
     */
    public Long getCurrentUserId() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getId() : null;
    }

    /**
     * Obtiene el email del usuario actual.
     */
    public String getCurrentUserEmail() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getEmail() : null;
    }

    /**
     * Obtiene el nombre del usuario actual.
     */
    public String getCurrentUserName() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getName() : null;
    }

    /**
     * Obtiene el rol del usuario actual.
     */
    public String getCurrentUserRole() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getRole() : null;
    }

    /**
     * Verifica si el usuario actual es un estudiante.
     */
    public boolean isStudent() {
        return "STUDENT".equals(getCurrentUserRole());
    }

    /**
     * Verifica si el usuario actual es un profesor.
     */
    public boolean isTeacher() {
        return "TEACHER".equals(getCurrentUserRole());
    }

    /**
     * Verifica si el usuario actual es un administrador.
     */
    public boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }

    /**
     * Almacena información adicional en la sesión HTTP.
     */
    public void setSessionAttribute(HttpSession session, String key, Object value) {
        if (session != null) {
            session.setAttribute(key, value);
        }
    }

    /**
     * Obtiene información de la sesión HTTP.
     */
    public Object getSessionAttribute(HttpSession session, String key) {
        return session != null ? session.getAttribute(key) : null;
    }

    /**
     * Invalida la sesión actual (para logout).
     */
    public void invalidateSession(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * Configura atributos básicos en la sesión tras el login.
     */
    public void setupSessionAttributes(HttpSession session, CustomUserDetails userDetails) {
        if (session != null && userDetails != null) {
            session.setAttribute(USER_ID_KEY, userDetails.getId());
            session.setAttribute(USER_ROLE_KEY, userDetails.getRole());
            session.setAttribute(USER_EMAIL_KEY, userDetails.getEmail());
            session.setAttribute(USER_NAME_KEY, userDetails.getName());
        }
    }
}