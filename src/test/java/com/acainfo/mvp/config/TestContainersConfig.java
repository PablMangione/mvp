package com.acainfo.mvp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuración compartida para TestContainers.
 * Permite reutilizar la misma instancia de MySQL para múltiples tests,
 * mejorando el rendimiento de la suite de pruebas.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    /**
     * Define un contenedor MySQL reutilizable para los tests.
     * Spring Boot 3.1+ puede auto-configurar la conexión usando @ServiceConnection.
     */
    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("acainfo_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true); // Reutilizar el contenedor entre tests
    }
}
