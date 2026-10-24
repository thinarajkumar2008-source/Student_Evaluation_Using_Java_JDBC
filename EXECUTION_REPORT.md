# Kongu Engineering College (Autonomous), Perundurai, Erode - 638060
## Regulation 2024 (R2024) - Student Academic & Continuous Evaluation System
### Technical Project Report & Execution Documentation (Java JDBC)

---

## 1. System Overview & Architecture

The **KEC R2024 Student Academic & Continuous Evaluation System** is a database-driven Java JDBC application designed strictly according to **Kongu Engineering College (Autonomous) Regulation 2024 (R2024)** standards.

### Key Capabilities:
- **Student & Course Management:** Registration with Roll Numbers (`24ITR001`), Course Catalog with R2024 Course Types (`THEORY`, `PRACTICAL`, `EMBEDDED`), and Semester Enrollments.
- **Incremental Marks Entry ("Then and Now"):**
  - **Phase 1 (Mid-Semester):** CIA Test 1 (0–100) and CIA Test 2 (0–100).
  - **Phase 2 (End-of-Semester CIA):** Assignments / Quizzes / Lab Continuous Evaluations (0–100).
  - **Phase 3 (Post-Semester):** End Semester Examination (ESE) Marks (0–100) or Absence (AB).
- **KEC R2024 Evaluation & Weightage Engine:**
  - **THEORY:** CIA = 40% | ESE = 60% | ESE Threshold Min = 45% (27/60) | Aggregate Min = 50 Marks.
  - **PRACTICAL:** CIA = 60% | ESE = 40% | ESE Threshold Min = 50% (20/40) | Aggregate Min = 50 Marks.
  - **EMBEDDED:** CIA = 50% | ESE = 50% | ESE Threshold Min = 45% (22.5/50) | Aggregate Min = 50 Marks.
- **Attendance Eligibility Filtering:**
  - $\ge 75\%$: Eligible for ESE.
  - $65\% \text{ to } 74.9\%$: Conditionally eligible (Requires Medical Condonation approval; barred if unapproved).
  - $< 65\%$: Award **SA (Shortage of Attendance)** grade (0 Grade Points), barred from ESE.
- **10-Point Letter Grading Scale:**
  - **O (91–100):** 10 Grade Points
  - **A+ (81–90):** 9 Grade Points
  - **A (71–80):** 8 Grade Points
  - **B+ (61–70):** 7 Grade Points
  - **B (50–60):** 6 Grade Points
  - **RA (Re-Appearance):** 0 Grade Points (Aggregate $< 50$ or failed ESE threshold)
  - **SA (Shortage of Attendance):** 0 Grade Points
  - **AB (Absent in ESE):** 0 Grade Points
- **Academic Performance Reporting:**
  - Formal Student Semester Mark Sheet with SGPA and Equivalent Percentage ($\text{SGPA} \times 10$).
  - Attendance Defaulter Report.
  - Department-wise Class Performance & Result Summary.

---

## 2. Database Schema Script (`kec_schema.sql`)

```sql
-- Table 1: students
CREATE TABLE students (
    roll_no         VARCHAR(20) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    department      VARCHAR(50)  NOT NULL,
    semester        INT          NOT NULL CHECK (semester BETWEEN 1 AND 8)
);

-- Table 2: courses
CREATE TABLE courses (
    course_code     VARCHAR(20) PRIMARY KEY,
    title           VARCHAR(150) NOT NULL,
    credits         INT          NOT NULL CHECK (credits > 0),
    course_type     VARCHAR(20)  NOT NULL CHECK (course_type IN ('THEORY', 'PRACTICAL', 'EMBEDDED'))
);

-- Table 3: enrollments
CREATE TABLE enrollments (
    enrollment_id    INTEGER PRIMARY KEY AUTOINCREMENT,
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

-- Table 4: assessment_marks
CREATE TABLE assessment_marks (
    mark_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    enrollment_id    INT     NOT NULL UNIQUE,
    cia1             DOUBLE  DEFAULT NULL CHECK (cia1 IS NULL OR (cia1 BETWEEN 0 AND 100)),
    cia2             DOUBLE  DEFAULT NULL CHECK (cia2 IS NULL OR (cia2 BETWEEN 0 AND 100)),
    assignment_lab   DOUBLE  DEFAULT NULL CHECK (assignment_lab IS NULL OR (assignment_lab BETWEEN 0 AND 100)),
    ese_raw          DOUBLE  DEFAULT NULL CHECK (ese_raw IS NULL OR (ese_raw BETWEEN 0 AND 100)),
    is_ese_absent    BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE
);
```

---

## 3. Execution Proofs & Terminal Outputs

### Proof 1: Adding a Student and Course
```
>>> 1. STUDENT REGISTRATION <<<
Enter Student Roll Number (e.g., 24ITR001): 24ITR099
Enter Student Full Name: Naveen Kumar M
Enter Department (e.g., Information Technology / CSE / ECE / MECH): Information Technology
Enter Current Semester (1-8): 1
[SUCCESS] Student successfully registered:
          24ITR099     | Naveen Kumar M            | Information Technology         | Sem 1

>>> 2. COURSE CATALOG MANAGEMENT <<<
Enter Course Code (e.g., 24ITT31): 24ITT12
Enter Course Title: Computer Organization & Architecture
Enter Credits (1-6): 3
Select Course Type according to KEC R2024:
  1. THEORY     (CIA: 40% | ESE: 60% | Min ESE: 45% = 27/60)
  2. PRACTICAL  (CIA: 60% | ESE: 40% | Min ESE: 50% = 20/40)
  3. EMBEDDED   (CIA: 50% | ESE: 50% | Min ESE: 45% = 22.5/50)
Choice (1-3): 1
[SUCCESS] Course registered successfully in catalog:
          24ITT12    | Computer Organization & Architecture     | 3 Credits | THEORY
```

---

### Proof 2: Incremental Marks Entry (Phase 1, Phase 2, Phase 3)
```
>>> 4. INCREMENTAL MARKS ENTRY (KEC R2024) <<<
The system supports phased incremental assessment entry as the semester progresses:
  1. Phase 1 (Mid-Semester)       : Record CIA Test 1 (0-100) and CIA Test 2 (0-100)
  2. Phase 2 (End of Semester-CIA): Record Assignments / Quizzes / Lab Evaluations (0-100)
  3. Phase 3 (Post Semester)      : Record End Semester Exam (ESE) Marks (0-100) / Absence (AB)

Select Assessment Phase (1-3): 1
Enter Enrollment ID: 16
Target: 24ITR099 (Naveen Kumar M) | Course: 24ITT12 (Computer Organization & Architecture - THEORY)
--- Phase 1: Mid-Semester Continuous Internal Assessments ---
Enter CIA Test 1 Marks (0 - 100): 88.0
Enter CIA Test 2 Marks (0 - 100): 92.0
[SUCCESS] Phase 1 Recorded: CIA1 = 88.0, CIA2 = 92.0

Select Assessment Phase (1-3): 2
Enter Enrollment ID: 16
--- Phase 2: End-of-Semester Continuous Assessments ---
Enter Assignments / Quizzes Marks (0 - 100): 90.0
[SUCCESS] Phase 2 Recorded: Assignments / Quizzes = 90.0

Select Assessment Phase (1-3): 3
Enter Enrollment ID: 16
--- Phase 3: Post-Semester End Semester Examination (ESE) ---
Is student ABSENT in ESE? (y/n): n
Enter ESE Raw Mark (0 - 100): 85.0
[SUCCESS] Phase 3 Recorded: ESE Raw Mark = 85.0
```

---

### Proof 3: Generated Student Semester Mark Sheet (Calculated SGPA & %)

```
============================================================================================================
                         KONGU ENGINEERING COLLEGE (AUTONOMOUS) - ERODE                             
                               SEMESTER GRADE & PERFORMANCE REPORT                                  
                                  REGULATION 2024 (R2024)                                           
============================================================================================================
  Roll Number : 24ITR001          Student Name : Thina Rajkumar                
  Department  : Information Technology   Semester     : 1
------------------------------------------------------------------------------------------------------------
Code      | Course Title                 | Type      | Crd | Attn % |   CIA |   ESE |   Tot |   Grd | GP | Result      
------------------------------------------------------------------------------------------------------------
24ITL11   | Programming Practices Lab... | PRACTICAL |   2 |  97.8% |  57.7 |  38.0 |  95.7 |   O | 10 | PASS        
24ITP11   | Object Oriented Programmi... | EMBEDDED  |   4 |  96.0% |  45.4 |  43.0 |  88.4 |  A+ |  9 | PASS        
24ITT11   | Problem Solving & Python ... | THEORY    |   3 |  93.3% |  37.4 |  55.2 |  92.6 |   O | 10 | PASS        
24MAT11   | Matrices and Calculus        | THEORY    |   4 |  95.0% |  34.9 |  50.4 |  85.3 |  A+ |  9 | PASS        
============================================================================================================
  Total Enrolled Credits  : 13          Total Credits Earned (Passed) : 13   
  Semester SGPA (0 - 10)  : 9.38        Equivalent Percentage (%)    : 93.85%
============================================================================================================
  Weightage Rules: THEORY (40% CIA / 60% ESE) | PRACTICAL (60% CIA / 40% ESE) | EMBEDDED (50% / 50%)       
  Grading Scale  : O (91-100: 10), A+ (81-90: 9), A (71-80: 8), B+ (61-70: 7), B (50-60: 6), RA/SA/AB: 0    
============================================================================================================
```

---

### Proof 4: Attendance Defaulter Report (Shortage of Attendance SA & Medical Condonation)

```
============================================================================================================
                      KEC R2024 - ATTENDANCE DEFAULTER & ELIGIBILITY REPORT                                 
============================================================================================================
[1] STUDENTS BARRED FROM ESE (Attendance < 65% or Unapproved Condonation - Grade SA Awarded)
------------------------------------------------------------------------------------------------------------
Roll No    | Student Name         | Course    | Course Title              | Attn% |   Status | Penalty / Action      
------------------------------------------------------------------------------------------------------------
24ITR003   | Vignesh S            | 24ITT11   | Problem Solving & Pyth... | 53.3% | DEF_SA   | Barred (SA - 0 GP)

[2] MEDICAL CONDONATION CASES (65.0% to 74.9% Attendance)
------------------------------------------------------------------------------------------------------------
Roll No    | Student Name         | Course    | Course Title              | Attn% | Condonation  | ESE Eligibility   
------------------------------------------------------------------------------------------------------------
24ITR002   | Priyadharshini K     | 24ITP11   | Object Oriented Progra... | 70.7% | APPROVED     | ELIGIBLE          
24ITR002   | Priyadharshini K     | 24ITT11   | Problem Solving & Pyth... | 70.0% | APPROVED     | ELIGIBLE          
============================================================================================================
```

---

### Proof 5: Class Performance & Department-wise Result Summary

```
============================================================================================================
                      KEC R2024 - CLASS PERFORMANCE & RESULT SUMMARY REPORT                                 
============================================================================================================
Department                       |  Total |  Registrns |   Pass |   Fail |     Pass % | Avg SGPA
------------------------------------------------------------------------------------------------------------
Computer Science and Engineering |      1 |          2 |      1 |      1 |     50.00% |     3.86
   Breakdown: RA (Re-Appearance): 0 | SA (Shortage Attn): 0 | AB (Absent): 1
Information Technology           |      4 |         13 |     12 |      1 |     92.31% |     7.90
   Breakdown: RA (Re-Appearance): 0 | SA (Shortage Attn): 1 | AB (Absent): 0
Mechanical Engineering           |      1 |          1 |      0 |      1 |      0.00% |     0.00
   Breakdown: RA (Re-Appearance): 1 | SA (Shortage Attn): 0 | AB (Absent): 0
============================================================================================================
```

---

## 4. Verification of KEC Regulation 2024 Rules

| Rule Requirement | Implementation Method | Verified Test Case | Result |
| :--- | :--- | :--- | :--- |
| **THEORY (40% CIA / 60% ESE)** | `cia*0.4 + ese*0.6`; Min ESE 27/60 | Course `24ITT11`, `24MAT11` | PASS |
| **PRACTICAL (60% CIA / 40% ESE)** | `cia*0.6 + ese*0.4`; Min ESE 20/40 | Course `24ITL11` | PASS |
| **EMBEDDED (50% CIA / 50% ESE)** | `cia*0.5 + ese*0.5`; Min ESE 22.5/50 | Course `24ITP11` | PASS |
| **Attendance $\ge 75\%$** | Allowed to write ESE | `24ITR001` (93%–97%) | ELIGIBLE |
| **Attendance $65\% \le \text{Attn} < 75\%$** | Medical Condonation Approval Flag | `24ITR002` (70.7%) | CONDONED & PASSED |
| **Attendance $< 65\%$** | Auto SA Awarded, barred from ESE | `24ITR003` (53.3% in `24ITT11`) | SA (0 GP) |
| **Absent in ESE** | Absent flag triggers AB grade | `24CSR015` in `24MAT11` | AB (0 GP) |
| **Failed ESE Minimum** | ESE below threshold triggers RA grade | `24MEC007` (40 in Theory ESE) | RA (0 GP) |
| **SGPA & % Formula** | $\sum(C \times GP) / \sum C$, $\% = SGPA \times 10$ | `24ITR001`: SGPA 9.38, 93.85% | EXACT |
