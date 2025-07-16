-- Agregar columna max_capacity a course_group
ALTER TABLE course_group
    ADD COLUMN max_capacity INT NOT NULL DEFAULT 30
        COMMENT 'Capacidad máxima de estudiantes en el grupo';

-- Agregar índice para búsquedas de grupos con capacidad disponible
CREATE INDEX idx_course_group_status_capacity
    ON course_group(status, max_capacity);