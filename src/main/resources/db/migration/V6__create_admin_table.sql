CREATE TABLE admin (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       permission_level ENUM('FULL', 'ACADEMIC', 'USERS', 'READONLY') NOT NULL DEFAULT 'FULL',
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       notes VARCHAR(500),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NULL,
                       UNIQUE INDEX idx_admin_email (email),
                       INDEX idx_admin_active (is_active),
                       INDEX idx_admin_permission (permission_level)
);