-- Tabla alumno
CREATE TABLE student (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         email VARCHAR(150) NOT NULL UNIQUE,
                         password VARCHAR(255) NOT NULL,
                         major VARCHAR(100) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE INDEX idx_student_email (email)
);

-- Tabla profesor
CREATE TABLE teacher (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         email VARCHAR(150) NOT NULL UNIQUE,
                         password VARCHAR(255) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE INDEX idx_teacher_email (email)
);

-- Tabla asignatura
CREATE TABLE subject (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(150) NOT NULL,
                         major VARCHAR(100) NOT NULL,
                         course_year INT NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE INDEX idx_subject_name_major (name, major)
);

-- Tabla grupo
CREATE TABLE course_group (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              subject_id BIGINT NOT NULL,
                              teacher_id BIGINT NOT NULL,
                              status ENUM('PLANNED', 'ACTIVE', 'CLOSED') NOT NULL DEFAULT 'PLANNED',
                              type ENUM('REGULAR', 'INTENSIVE') NOT NULL DEFAULT 'REGULAR',
                              price DECIMAL(10, 2) NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (subject_id) REFERENCES subject(id),
                              FOREIGN KEY (teacher_id) REFERENCES teacher(id)
);

-- Tabla inscripción (enrollment)
CREATE TABLE enrollment (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            student_id BIGINT NOT NULL,
                            course_group_id BIGINT NOT NULL,
                            enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            payment_status ENUM('PENDING', 'PAID', 'FAILED') NOT NULL DEFAULT 'PENDING',
                            FOREIGN KEY (student_id) REFERENCES student(id),
                            FOREIGN KEY (course_group_id) REFERENCES course_group(id),
                            UNIQUE INDEX idx_enrollment_unique (student_id, course_group_id)
);

-- Tabla solicitudes de creación de grupo
CREATE TABLE group_request (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               student_id BIGINT NOT NULL,
                               subject_id BIGINT NOT NULL,
                               request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
                               FOREIGN KEY (student_id) REFERENCES student(id),
                               FOREIGN KEY (subject_id) REFERENCES subject(id)
);

-- Tabla sesiones del grupo
CREATE TABLE group_session (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               course_group_id BIGINT NOT NULL,
                               day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
                               start_time TIME NOT NULL,
                               end_time TIME NOT NULL,
                               classroom VARCHAR(50),
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (course_group_id) REFERENCES course_group(id),
                               UNIQUE INDEX idx_session_group_day (course_group_id, day_of_week, start_time)
);
