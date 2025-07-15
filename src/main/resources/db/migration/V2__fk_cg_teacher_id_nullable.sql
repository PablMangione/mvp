ALTER TABLE course_group
    MODIFY teacher_id BIGINT NULL;

ALTER TABLE course_group
    DROP FOREIGN KEY course_group_ibfk_2;

ALTER TABLE course_group
    ADD CONSTRAINT course_group_ibfk_2
        FOREIGN KEY (teacher_id)
            REFERENCES teacher(id)
            ON DELETE SET NULL;