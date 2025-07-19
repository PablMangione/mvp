-- ============================================================================
-- V7__dev_data.sql - Datos de prueba para desarrollo
-- ============================================================================
-- IMPORTANTE: Este script solo debe ejecutarse en entornos de desarrollo
-- Contiene usuarios de prueba, asignaturas, grupos y datos de ejemplo
-- ============================================================================

-- ============================================================================
-- 1. ADMINISTRADORES
-- ============================================================================
-- Password para todos: admin123
INSERT INTO admin (name, email, password, permission_level, is_active, notes)
VALUES
    ('Admin Principal', 'admin@acainfo.edu', '$2a$12$uZPOUn28mX04KOACU4ZXmegy54Ayk6v.y6n5TXMkVnAm3DdGDxxdW', 'FULL', TRUE, 'Administrador principal del sistema'),
    ('Admin Académico', 'academico@acainfo.edu', '$2a$12$uZPOUn28mX04KOACU4ZXmegy54Ayk6v.y6n5TXMkVnAm3DdGDxxdW', 'ACADEMIC', TRUE, 'Gestión académica'),
    ('Admin Usuarios', 'usuarios@acainfo.edu', '$2a$12$uZPOUn28mX04KOACU4ZXmegy54Ayk6v.y6n5TXMkVnAm3DdGDxxdW', 'USERS', TRUE, 'Gestión de usuarios');

-- ============================================================================
-- 2. PROFESORES
-- ============================================================================
-- Password para todos: prof123
INSERT INTO teacher (name, email, password)
VALUES
    ('Dr. Roberto Martínez', 'roberto.martinez@acainfo.edu', '$2a$12$dA1RGFXDVJBrDyf6du5Sz.x7KD/pCINFAkn1iOC3vovXNTuHt2IqO'),
    ('Dra. María García', 'maria.garcia@acainfo.edu', '$2a$12$dA1RGFXDVJBrDyf6du5Sz.x7KD/pCINFAkn1iOC3vovXNTuHt2IqO'),
    ('Ing. Carlos López', 'carlos.lopez@acainfo.edu', '$2a$12$dA1RGFXDVJBrDyf6du5Sz.x7KD/pCINFAkn1iOC3vovXNTuHt2IqO'),
    ('Lic. Ana Sánchez', 'ana.sanchez@acainfo.edu', '$2a$12$dA1RGFXDVJBrDyf6du5Sz.x7KD/pCINFAkn1iOC3vovXNTuHt2IqO'),
    ('Dr. Juan Pérez', 'juan.perez@acainfo.edu', '$2a$12$dA1RGFXDVJBrDyf6du5Sz.x7KD/pCINFAkn1iOC3vovXNTuHt2IqO'),
    ('Ing. Laura Rodríguez', 'laura.rodriguez@acainfo.edu', '$2a$12$dA1RGFXDVJBrDyf6du5Sz.x7KD/pCINFAkn1iOC3vovXNTuHt2IqO');

-- ============================================================================
-- 3. ESTUDIANTES
-- ============================================================================
-- Password para todos: student123
INSERT INTO student (name, email, password, major)
VALUES
    -- Ingeniería en Sistemas
    ('Ana García López', 'ana.garcia@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Ingeniería en Sistemas'),
    ('Carlos Mendoza Ruiz', 'carlos.mendoza@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Ingeniería en Sistemas'),
    ('María Fernández Silva', 'maria.fernandez@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Ingeniería en Sistemas'),
    ('José Martín Gómez', 'jose.martin@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Ingeniería en Sistemas'),
    ('Laura Jiménez Torres', 'laura.jimenez@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Ingeniería en Sistemas'),

    -- Ingeniería Civil
    ('Pedro Álvarez Castro', 'pedro.alvarez@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Ingeniería Civil'),
    ('Sofía Morales Díaz', 'sofia.morales@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Ingeniería Civil'),
    ('Diego Herrera Luna', 'diego.herrera@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Ingeniería Civil'),

    -- Administración de Empresas
    ('Lucía Vargas Soto', 'lucia.vargas@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Administración de Empresas'),
    ('Roberto Cruz Mejía', 'roberto.cruz@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Administración de Empresas'),
    ('Carmen Ramos Vega', 'carmen.ramos@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Administración de Empresas'),

    -- Psicología
    ('Patricia Molina Ríos', 'patricia.molina@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Psicología'),
    ('Andrés Guerrero Paz', 'andres.guerrero@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Psicología'),
    ('Isabel Navarro Flores', 'isabel.navarro@estudiantes.edu', '$2a$12$31FjZVBjDMTRCQt5/eVVE./FrOiKnSirUOpCEYfhqwNdwFqqwAVUq', 'Psicología');

-- ============================================================================
-- 4. ASIGNATURAS
-- ============================================================================

-- Ingeniería en Sistemas
INSERT INTO subject (name, major, course_year)
VALUES
    -- Primer año
    ('Fundamentos de Programación', 'Ingeniería en Sistemas', 1),
    ('Cálculo I', 'Ingeniería en Sistemas', 1),
    ('Álgebra Lineal', 'Ingeniería en Sistemas', 1),
    ('Introducción a la Ingeniería', 'Ingeniería en Sistemas', 1),

    -- Segundo año
    ('Programación Orientada a Objetos', 'Ingeniería en Sistemas', 2),
    ('Estructuras de Datos', 'Ingeniería en Sistemas', 2),
    ('Base de Datos I', 'Ingeniería en Sistemas', 2),
    ('Cálculo II', 'Ingeniería en Sistemas', 2),

    -- Tercer año
    ('Algoritmos y Complejidad', 'Ingeniería en Sistemas', 3),
    ('Base de Datos II', 'Ingeniería en Sistemas', 3),
    ('Ingeniería de Software I', 'Ingeniería en Sistemas', 3),
    ('Sistemas Operativos', 'Ingeniería en Sistemas', 3),

    -- Cuarto año
    ('Inteligencia Artificial', 'Ingeniería en Sistemas', 4),
    ('Desarrollo Web', 'Ingeniería en Sistemas', 4),
    ('Redes de Computadoras', 'Ingeniería en Sistemas', 4),
    ('Seguridad Informática', 'Ingeniería en Sistemas', 4);

-- Ingeniería Civil
INSERT INTO subject (name, major, course_year)
VALUES
    ('Física I', 'Ingeniería Civil', 1),
    ('Química General', 'Ingeniería Civil', 1),
    ('Dibujo Técnico', 'Ingeniería Civil', 1),
    ('Resistencia de Materiales', 'Ingeniería Civil', 2),
    ('Mecánica de Fluidos', 'Ingeniería Civil', 2),
    ('Estructuras I', 'Ingeniería Civil', 3),
    ('Hidráulica', 'Ingeniería Civil', 3);

-- Administración de Empresas
INSERT INTO subject (name, major, course_year)
VALUES
    ('Introducción a la Administración', 'Administración de Empresas', 1),
    ('Contabilidad I', 'Administración de Empresas', 1),
    ('Microeconomía', 'Administración de Empresas', 1),
    ('Marketing', 'Administración de Empresas', 2),
    ('Finanzas Corporativas', 'Administración de Empresas', 2),
    ('Recursos Humanos', 'Administración de Empresas', 3);

-- Psicología
INSERT INTO subject (name, major, course_year)
VALUES
    ('Psicología General', 'Psicología', 1),
    ('Neurociencias', 'Psicología', 1),
    ('Estadística', 'Psicología', 1),
    ('Psicología del Desarrollo', 'Psicología', 2),
    ('Psicopatología', 'Psicología', 3);

-- ============================================================================
-- 5. GRUPOS DE CURSO
-- ============================================================================

-- Grupos ACTIVOS de Ingeniería en Sistemas
INSERT INTO course_group (subject_id, teacher_id, status, type, price, max_capacity)
VALUES
    -- Fundamentos de Programación - 2 grupos
    ((SELECT id FROM subject WHERE name = 'Fundamentos de Programación' AND major = 'Ingeniería en Sistemas'),
     (SELECT id FROM teacher WHERE email = 'roberto.martinez@acainfo.edu'),
     'ACTIVE', 'REGULAR', 120.00, 30),

    ((SELECT id FROM subject WHERE name = 'Fundamentos de Programación' AND major = 'Ingeniería en Sistemas'),
     (SELECT id FROM teacher WHERE email = 'maria.garcia@acainfo.edu'),
     'ACTIVE', 'INTENSIVE', 180.00, 20),

    -- POO - 2 grupos
    ((SELECT id FROM subject WHERE name = 'Programación Orientada a Objetos' AND major = 'Ingeniería en Sistemas'),
     (SELECT id FROM teacher WHERE email = 'carlos.lopez@acainfo.edu'),
     'ACTIVE', 'REGULAR', 150.00, 25),

    ((SELECT id FROM subject WHERE name = 'Programación Orientada a Objetos' AND major = 'Ingeniería en Sistemas'),
     (SELECT id FROM teacher WHERE email = 'laura.rodriguez@acainfo.edu'),
     'ACTIVE', 'INTENSIVE', 200.00, 15),

    -- Base de Datos I
    ((SELECT id FROM subject WHERE name = 'Base de Datos I' AND major = 'Ingeniería en Sistemas'),
     (SELECT id FROM teacher WHERE email = 'juan.perez@acainfo.edu'),
     'ACTIVE', 'REGULAR', 140.00, 30),

    -- Estructuras de Datos
    ((SELECT id FROM subject WHERE name = 'Estructuras de Datos' AND major = 'Ingeniería en Sistemas'),
     (SELECT id FROM teacher WHERE email = 'roberto.martinez@acainfo.edu'),
     'ACTIVE', 'REGULAR', 160.00, 25);

-- Grupos PLANIFICADOS
INSERT INTO course_group (subject_id, teacher_id, status, type, price, max_capacity)
VALUES
    -- Inteligencia Artificial
    ((SELECT id FROM subject WHERE name = 'Inteligencia Artificial' AND major = 'Ingeniería en Sistemas'),
     (SELECT id FROM teacher WHERE email = 'maria.garcia@acainfo.edu'),
     'PLANNED', 'REGULAR', 180.00, 20),

    -- Desarrollo Web
    ((SELECT id FROM subject WHERE name = 'Desarrollo Web' AND major = 'Ingeniería en Sistemas'),
     (SELECT id FROM teacher WHERE email = 'carlos.lopez@acainfo.edu'),
     'PLANNED', 'INTENSIVE', 220.00, 25);

-- Grupos de otras carreras
INSERT INTO course_group (subject_id, teacher_id, status, type, price, max_capacity)
VALUES
    -- Ingeniería Civil
    ((SELECT id FROM subject WHERE name = 'Física I' AND major = 'Ingeniería Civil'),
     (SELECT id FROM teacher WHERE email = 'ana.sanchez@acainfo.edu'),
     'ACTIVE', 'REGULAR', 130.00, 35),

    -- Administración
    ((SELECT id FROM subject WHERE name = 'Marketing' AND major = 'Administración de Empresas'),
     (SELECT id FROM teacher WHERE email = 'laura.rodriguez@acainfo.edu'),
     'ACTIVE', 'REGULAR', 110.00, 40);

-- ============================================================================
-- 6. HORARIOS DE GRUPOS (GROUP_SESSION)
-- ============================================================================

-- Horarios para Fundamentos de Programación - Grupo 1
INSERT INTO group_session (course_group_id, day_of_week, start_time, end_time, classroom)
VALUES
    (1, 'MONDAY', '08:00:00', '10:00:00', 'Lab A-101'),
    (1, 'WEDNESDAY', '08:00:00', '10:00:00', 'Lab A-101'),
    (1, 'FRIDAY', '08:00:00', '10:00:00', 'Lab A-101');

-- Horarios para Fundamentos de Programación - Grupo 2 (Intensivo)
INSERT INTO group_session (course_group_id, day_of_week, start_time, end_time, classroom)
VALUES
    (2, 'MONDAY', '14:00:00', '17:00:00', 'Lab B-203'),
    (2, 'TUESDAY', '14:00:00', '17:00:00', 'Lab B-203'),
    (2, 'THURSDAY', '14:00:00', '17:00:00', 'Lab B-203');

-- Horarios para POO - Grupo 1
INSERT INTO group_session (course_group_id, day_of_week, start_time, end_time, classroom)
VALUES
    (3, 'TUESDAY', '10:00:00', '12:00:00', 'Lab C-105'),
    (3, 'THURSDAY', '10:00:00', '12:00:00', 'Lab C-105');

-- Horarios para POO - Grupo 2 (Intensivo)
INSERT INTO group_session (course_group_id, day_of_week, start_time, end_time, classroom)
VALUES
    (4, 'SATURDAY', '08:00:00', '13:00:00', 'Lab D-301');

-- Horarios para Base de Datos I
INSERT INTO group_session (course_group_id, day_of_week, start_time, end_time, classroom)
VALUES
    (5, 'MONDAY', '16:00:00', '18:00:00', 'Aula 201'),
    (5, 'WEDNESDAY', '16:00:00', '18:00:00', 'Aula 201');

-- Horarios para Estructuras de Datos
INSERT INTO group_session (course_group_id, day_of_week, start_time, end_time, classroom)
VALUES
    (6, 'TUESDAY', '14:00:00', '16:00:00', 'Lab A-102'),
    (6, 'THURSDAY', '14:00:00', '16:00:00', 'Lab A-102');

-- ============================================================================
-- 7. INSCRIPCIONES (ENROLLMENTS)
-- ============================================================================

-- Ana García - Inscrita en POO y Base de Datos
INSERT INTO enrollment (student_id, course_group_id, payment_status)
VALUES
    ((SELECT id FROM student WHERE email = 'ana.garcia@estudiantes.edu'), 3, 'PAID'),
    ((SELECT id FROM student WHERE email = 'ana.garcia@estudiantes.edu'), 5, 'PENDING');

-- Carlos Mendoza - Inscrito en POO (intensivo) y Estructuras
INSERT INTO enrollment (student_id, course_group_id, payment_status)
VALUES
    ((SELECT id FROM student WHERE email = 'carlos.mendoza@estudiantes.edu'), 4, 'PAID'),
    ((SELECT id FROM student WHERE email = 'carlos.mendoza@estudiantes.edu'), 6, 'PAID');

-- María Fernández - Inscrita en Fundamentos y Base de Datos
INSERT INTO enrollment (student_id, course_group_id, payment_status)
VALUES
    ((SELECT id FROM student WHERE email = 'maria.fernandez@estudiantes.edu'), 1, 'PAID'),
    ((SELECT id FROM student WHERE email = 'maria.fernandez@estudiantes.edu'), 5, 'PENDING');

-- José Martín - Inscrito en Fundamentos (intensivo)
INSERT INTO enrollment (student_id, course_group_id, payment_status)
VALUES
    ((SELECT id FROM student WHERE email = 'jose.martin@estudiantes.edu'), 2, 'PAID');

-- Laura Jiménez - Inscrita en POO y Estructuras
INSERT INTO enrollment (student_id, course_group_id, payment_status)
VALUES
    ((SELECT id FROM student WHERE email = 'laura.jimenez@estudiantes.edu'), 3, 'PENDING'),
    ((SELECT id FROM student WHERE email = 'laura.jimenez@estudiantes.edu'), 6, 'PAID');

-- Pedro Álvarez (Civil) - Inscrito en Física I
INSERT INTO enrollment (student_id, course_group_id, payment_status)
VALUES
    ((SELECT id FROM student WHERE email = 'pedro.alvarez@estudiantes.edu'), 9, 'PAID');

-- Lucía Vargas (Administración) - Inscrita en Marketing
INSERT INTO enrollment (student_id, course_group_id, payment_status)
VALUES
    ((SELECT id FROM student WHERE email = 'lucia.vargas@estudiantes.edu'), 10, 'PAID');

-- ============================================================================
-- 8. SOLICITUDES DE GRUPO (GROUP_REQUESTS)
-- ============================================================================

-- Solicitudes PENDIENTES
INSERT INTO group_request (student_id, subject_id, status)
VALUES
    -- Ana solicita grupo de Algoritmos
    ((SELECT id FROM student WHERE email = 'ana.garcia@estudiantes.edu'),
     (SELECT id FROM subject WHERE name = 'Algoritmos y Complejidad' AND major = 'Ingeniería en Sistemas'),
     'PENDING'),

    -- Carlos solicita grupo de Inteligencia Artificial
    ((SELECT id FROM student WHERE email = 'carlos.mendoza@estudiantes.edu'),
     (SELECT id FROM subject WHERE name = 'Inteligencia Artificial' AND major = 'Ingeniería en Sistemas'),
     'PENDING'),

    -- María solicita grupo de Cálculo II
    ((SELECT id FROM student WHERE email = 'maria.fernandez@estudiantes.edu'),
     (SELECT id FROM subject WHERE name = 'Cálculo II' AND major = 'Ingeniería en Sistemas'),
     'PENDING');

-- Solicitudes APROBADAS (históricas)
INSERT INTO group_request (student_id, subject_id, status, updated_at)
VALUES
    ((SELECT id FROM student WHERE email = 'jose.martin@estudiantes.edu'),
     (SELECT id FROM subject WHERE name = 'Base de Datos I' AND major = 'Ingeniería en Sistemas'),
     'APPROVED', NOW());

-- Solicitudes RECHAZADAS (históricas)
INSERT INTO group_request (student_id, subject_id, status, updated_at)
VALUES
    ((SELECT id FROM student WHERE email = 'laura.jimenez@estudiantes.edu'),
     (SELECT id FROM subject WHERE name = 'Sistemas Operativos' AND major = 'Ingeniería en Sistemas'),
     'REJECTED', NOW());

-- ============================================================================
-- 9. INFORMACIÓN ADICIONAL PARA DESARROLLO
-- ============================================================================

-- Actualizar algunos timestamps para simular actividad
UPDATE student SET updated_at = DATE_SUB(NOW(), INTERVAL 2 DAY) WHERE id % 2 = 0;
UPDATE teacher SET updated_at = DATE_SUB(NOW(), INTERVAL 5 DAY) WHERE id % 3 = 0;
UPDATE course_group SET updated_at = DATE_SUB(NOW(), INTERVAL 1 WEEK) WHERE status = 'ACTIVE';

-- ============================================================================
-- RESUMEN DE USUARIOS DE PRUEBA
-- ============================================================================
-- Administradores:
--   - admin@acainfo.edu (password: admin123) - FULL
--   - academico@acainfo.edu (password: admin123) - ACADEMIC
--   - usuarios@acainfo.edu (password: admin123) - USERS
--
-- Profesores (6 total):
--   - roberto.martinez@acainfo.edu (password: prof123)
--   - maria.garcia@acainfo.edu (password: prof123)
--   - carlos.lopez@acainfo.edu (password: prof123)
--   - ana.sanchez@acainfo.edu (password: prof123)
--   - juan.perez@acainfo.edu (password: prof123)
--   - laura.rodriguez@acainfo.edu (password: prof123)
--
-- Estudiantes (14 total):
--   - Ingeniería en Sistemas: 5 estudiantes
--   - Ingeniería Civil: 3 estudiantes
--   - Administración de Empresas: 3 estudiantes
--   - Psicología: 3 estudiantes
--   - Todos con password: student123
--
-- Grupos activos: 10 (con horarios asignados)
-- Grupos planificados: 2
-- Inscripciones: 11 (mixtas entre PAID y PENDING)
-- Solicitudes de grupo: 5 (3 pendientes, 1 aprobada, 1 rechazada)
-- ============================================================================