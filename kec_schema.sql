-- ============================================================================
-- Kongu Engineering College (Autonomous) - Regulation 2024
-- Department of Information Technology: 24ITT32 - Programming in Java
-- Database Schema Script: kec_schema.sql (MySQL)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS kec_eval;
USE kec_eval;

-- ----------------------------------------------------------------------------
-- Table 1: students
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS assessment_marks;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;

CREATE TABLE students (
    roll_no          VARCHAR(20) NOT NULL,
    name             VARCHAR(100) NOT NULL,
    department       VARCHAR(20) NOT NULL,
    current_semester INT NOT NULL DEFAULT 3,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (roll_no)
);

-- ----------------------------------------------------------------------------
-- Table 2: courses
-- ----------------------------------------------------------------------------
CREATE TABLE courses (
    course_code      VARCHAR(20) NOT NULL,
    title            VARCHAR(150) NOT NULL,
    credits          INT NOT NULL,
    course_type      ENUM('THEORY', 'PRACTICAL', 'EMBEDDED') NOT NULL,
    semester         INT NOT NULL DEFAULT 3,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_code)
);

-- ----------------------------------------------------------------------------
-- Table 3: enrollments
-- ----------------------------------------------------------------------------
CREATE TABLE enrollments (
    enrollment_id    INT NOT NULL AUTO_INCREMENT,
    roll_no          VARCHAR(20) NOT NULL,
    course_code      VARCHAR(20) NOT NULL,
    semester         INT NOT NULL DEFAULT 3,
    total_classes    INT NOT NULL DEFAULT 0,
    attended_classes INT NOT NULL DEFAULT 0,
    is_condoned      TINYINT(1) NOT NULL DEFAULT 0,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (enrollment_id),
    KEY (roll_no),
    KEY (course_code),
    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE,
    FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------
-- Table 4: assessment_marks
-- ----------------------------------------------------------------------------
CREATE TABLE assessment_marks (
    mark_id          INT NOT NULL AUTO_INCREMENT,
    enrollment_id    INT NOT NULL UNIQUE,
    cia1_marks       DECIMAL(5,2) DEFAULT NULL,
    cia2_marks       DECIMAL(5,2) DEFAULT NULL,
    assignment_marks DECIMAL(5,2) DEFAULT NULL,
    ese_marks        DECIMAL(5,2) DEFAULT NULL,
    is_ese_absent    TINYINT(1) NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (mark_id),
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE
);
