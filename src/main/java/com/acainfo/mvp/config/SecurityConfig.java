package com.acainfo.mvp.config;

import com.acainfo.mvp.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

/**
 * Configuración de seguridad para la aplicación.
 * Define autenticación basada en sesiones, autorización por roles y protección de endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .securityContext(context->
                        context.requireExplicitSave(false))
                // Deshabilitar CSRF para API REST (las sesiones son HttpOnly)
                .csrf(csrf -> csrf.disable())

                // Configuración de CORS (ajustar según necesidades del frontend)
                .cors(cors -> cors.disable())

                // Configuración de autorización
                .authorizeHttpRequests(authz -> authz
                        // Endpoints públicos
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/error", "/api/error").permitAll()

                        // Endpoints de estudiantes
                        .requestMatchers("/api/students/**").hasRole("STUDENT")
                        .requestMatchers("/api/enrollments/**").hasRole("STUDENT")
                        .requestMatchers("/api/group-requests/**").hasRole("STUDENT")

                        // Endpoints de profesores
                        .requestMatchers("/api/teachers/**").hasRole("TEACHER")

                        // Endpoints de administradores
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Endpoints compartidos (requieren autenticación)
                        .requestMatchers("/api/subjects/**").authenticated()
                        .requestMatchers("/api/groups/**").authenticated()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()

                        // Cualquier otro endpoint requiere autenticación
                        .anyRequest().authenticated()
                )

                // Configuración de manejo de excepciones
                .exceptionHandling(exceptions -> exceptions
                        // Retornar 401 en lugar de redireccionar a login
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // Retornar 403 para acceso denegado
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Access Denied\",\"message\":\"No tiene permisos para acceder a este recurso\"}");
                        })
                )

                // Configuración de sesiones
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )

                // Configuración de logout
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Logout exitoso\"}");
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }
}