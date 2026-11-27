CREATE TABLE IF NOT EXISTS teacher
(
    id        BIGSERIAL PRIMARY KEY,
    reference VARCHAR(20)  NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email     VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS student
(
    id        BIGSERIAL PRIMARY KEY,
    reference VARCHAR(20)  NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email     VARCHAR(150) NOT NULL UNIQUE,
    phone     VARCHAR(20),
    cohort    VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS exercise
(
    id          BIGSERIAL PRIMARY KEY,
    teacher_id  BIGINT       NOT NULL REFERENCES teacher (id),
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    max_points  NUMERIC(5, 2) NOT NULL DEFAULT 20,
    deadline    TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

ALTER TABLE exercise
    ALTER COLUMN deadline TYPE TIMESTAMP
    USING CASE
        WHEN pg_typeof(deadline)::text = 'date' THEN deadline::date + TIME '23:59:59.999999'
        ELSE deadline::timestamp
    END;

CREATE TABLE IF NOT EXISTS submission
(
    id           BIGSERIAL PRIMARY KEY,
    exercise_id  BIGINT    NOT NULL REFERENCES exercise (id),
    student_id   BIGINT    NOT NULL REFERENCES student (id),
    content      TEXT,
    repo_url     VARCHAR(300),
    submitted_at TIMESTAMP NOT NULL DEFAULT now(),
    raw_points   NUMERIC(5, 2),
    feedback     TEXT,
    graded_at    TIMESTAMP,
    UNIQUE (exercise_id, student_id)
);
