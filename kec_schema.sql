-- ============================================================================
-- Kongu Engineering College (Autonomous), Perundurai, Erode - 638060
-- Regulation 2024 (R2024) - Academic & Continuous Evaluation System
-- Database Schema Script: kec_schema.sql
-- Compatible with MySQL 8.x and SQLite 3.x
-- ============================================================================

-- Drop tables if they already exist
DROP TABLE IF EXISTS assessment_marks;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;

-- ----------------------------------------------------------------------------
-- Table 1: students
-- ----------------------------------------------------------------------------
CREATE TABLE students (
    roll_no         VARCHAR(20) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    department      VARCHAR(50)  NOT NULL,
    semester        INT          NOT NULL CHECK (semester BETWEEN 1 AND 8)
);

-- ----------------------------------------------------------------------------
-- Table 2: courses
-- ----------------------------------------------------------------------------
CREATE TABLE courses (
    course_code     VARCHAR(20) PRIMARY KEY,
    title           VARCHAR(150) NOT NULL,
    credits         INT          NOT NULL CHECK (credits > 0),
    course_type     VARCHAR(20)  NOT NULL CHECK (course_type IN ('THEORY', 'PRACTICAL', 'EMBEDDED'))
);

-- ----------------------------------------------------------------------------
-- Table 3: enrollments
-- ----------------------------------------------------------------------------
CREATE TABLE enrollments (
    enrollment_id    INTEGER PRIMARY KEY AUTO_INCREMENT,
    roll_no          VARCHAR(20) NOT NULL,
    course_code      VARCHAR(20) NOT NULL,
    semester         INT         NOT NULL,
    total_classes    INT         DEFAULT 0 CHECK (total_classes >= 0),
    attended_classes INT         DEFAULT 0 CHECK (attended_classes >= 0 AND attended_classes <= total_classes),
    is_condoned      BOOLEAN     DEFAULT FALSE,
    FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE,
    FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE,
    UNIQUE (roll_no, course_code, semester)
);

-- ----------------------------------------------------------------------------
-- Table 4: assessment_marks
-- ----------------------------------------------------------------------------
CREATE TABLE assessment_marks (
    mark_id          INTEGER PRIMARY KEY AUTO_INCREMENT,
    enrollment_id    INT     NOT NULL UNIQUE,
    cia1             DOUBLE  DEFAULT NULL CHECK (cia1 IS NULL OR (cia1 BETWEEN 0 AND 100)),
    cia2             DOUBLE  DEFAULT NULL CHECK (cia2 IS NULL OR (cia2 BETWEEN 0 AND 100)),
    assignment_lab   DOUBLE  DEFAULT NULL CHECK (assignment_lab IS NULL OR (assignment_lab BETWEEN 0 AND 100)),
    ese_raw          DOUBLE  DEFAULT NULL CHECK (ese_raw IS NULL OR (ese_raw BETWEEN 0 AND 100)),
    is_ese_absent    BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE
);

-- ============================================================================
-- SEED DATA FOR KEC R2024 DEMONSTRATION & VERIFICATION
-- ============================================================================

-- Students
INSERT INTO students (roll_no, name, department, semester) VALUES ('24ITR001', 'Thina Rajkumar', 'Information Technology', 1);
INSERT INTO students (roll_no, name, department, semester) VALUES ('24ITR002', 'Priyadharshini K', 'Information Technology', 1);
INSERT INTO students (roll_no, name, department, semester) VALUES ('24ITR003', 'Vignesh S', 'Information Technology', 1);
INSERT INTO students (roll_no, name, department, semester) VALUES ('24CSR015', 'Ananya R', 'Computer Science and Engineering', 1);
INSERT INTO students (roll_no, name, department, semester) VALUES ('24MEC007', 'Karthik V', 'Mechanical Engineering', 1);

-- Courses
INSERT INTO courses (course_code, title, credits, course_type) VALUES ('24ITT11', 'Problem Solving & Python Programming', 3, 'THEORY');
INSERT INTO courses (course_code, title, credits, course_type) VALUES ('24ITP11', 'Object Oriented Programming with Java', 4, 'EMBEDDED');
INSERT INTO courses (course_code, title, credits, course_type) VALUES ('24ITL11', 'Programming Practices Laboratory', 2, 'PRACTICAL');
INSERT INTO courses (course_code, title, credits, course_type) VALUES ('24MAT11', 'Matrices and Calculus', 4, 'THEORY');

-- Enrollments: 24ITR001 (>75% attendance)
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (1, '24ITR001', '24ITT11', 1, 60, 56, 0);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (2, '24ITR001', '24ITP11', 1, 75, 72, 0);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (3, '24ITR001', '24ITL11', 1, 45, 44, 0);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (4, '24ITR001', '24MAT11', 1, 60, 57, 0);

-- Enrollments: 24ITR002 (Condonation approved)
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (5, '24ITR002', '24ITT11', 1, 60, 42, 1);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (6, '24ITR002', '24ITP11', 1, 75, 53, 1);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (7, '24ITR002', '24ITL11', 1, 45, 34, 1);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (8, '24ITR002', '24MAT11', 1, 60, 46, 0);

-- Enrollments: 24ITR003 (Shortage of Attendance in 24ITT11)
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (9, '24ITR003', '24ITT11', 1, 60, 32, 0);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (10, '24ITR003', '24ITP11', 1, 75, 62, 0);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (11, '24ITR003', '24ITL11', 1, 45, 38, 0);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (12, '24ITR003', '24MAT11', 1, 60, 48, 0);

-- Enrollments: 24CSR015 (Absent in ESE for 24MAT11)
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (13, '24CSR015', '24ITT11', 1, 60, 55, 0);
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (14, '24CSR015', '24MAT11', 1, 60, 52, 0);

-- Enrollments: 24MEC007 (Failed ESE minimum threshold)
INSERT INTO enrollments (enrollment_id, roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (15, '24MEC007', '24ITT11', 1, 60, 50, 0);

-- Assessment Marks for 24ITR001
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (1, 92.0, 94.0, 95.0, 92.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (2, 90.0, 88.0, 95.0, 86.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (3, 96.0, 94.0, 98.0, 95.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (4, 85.0, 88.0, 90.0, 84.0, 0);

-- Assessment Marks for 24ITR002
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (5, 75.0, 72.0, 80.0, 70.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (6, 68.0, 70.0, 75.0, 65.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (7, 80.0, 82.0, 85.0, 78.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (8, 62.0, 60.0, 65.0, 58.0, 0);

-- Assessment Marks for 24ITR003 (Enrollment 9 has SA: ESE is NULL)
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (9, 60.0, 55.0, 70.0, NULL, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (10, 72.0, 74.0, 80.0, 75.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (11, 85.0, 88.0, 90.0, 82.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (12, 65.0, 68.0, 70.0, 64.0, 0);

-- Assessment Marks for 24CSR015 (Enrollment 14 is absent in ESE)
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (13, 88.0, 85.0, 90.0, 86.0, 0);
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (14, 80.0, 82.0, 85.0, NULL, 1);

-- Assessment Marks for 24MEC007 (Enrollment 15 failed ESE threshold < 45)
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent) VALUES (15, 80.0, 82.0, 85.0, 40.0, 0);
