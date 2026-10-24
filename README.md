# Kongu Engineering College (Autonomous), Perundurai, Erode - 638060
# Regulation 2024 (R2024) - Student Academic & Continuous Evaluation System

A complete database-driven Java JDBC application to manage student records, perform incremental marks entry, enforce attendance eligibility rules, and generate official academic reports.

---

## Deliverables & Directory Structure

```
C:\Users\Thina rajkumar\Downloads\KEC_Student_Evaluation_System\
│
├── kec_schema.sql                  <- SQL DDL Script (students, courses, enrollments, assessment_marks)
├── DBConnection.java               <- JDBC connection manager (Supports SQLite & MySQL)
├── StudentDAO.java                 <- CRUD queries for Students, Courses, Enrollments & Attendance
├── AcademicDAO.java                <- Incremental marks entry (Phase 1, 2, 3) & KEC R2024 engine
├── MainApp.java                    <- Menu-driven interactive interface & report generator
├── Student.java                    <- Model class
├── Course.java                     <- Model class
├── Enrollment.java                 <- Model class
├── AssessmentMark.java             <- Model class
├── EvaluationResult.java           <- Model class
├── db.properties                   <- Database configuration (SQLite / MySQL)
│
├── compile.bat                     <- One-click compilation script
├── run.bat                         <- One-click interactive app launcher
├── run_demo.bat                    <- One-click automated verification & demo script
├── EXECUTION_REPORT.md             <- Full technical report with screenshot proofs
│
├── lib/                            <- Pre-packaged JDBC drivers & dependencies
│   ├── sqlite-jdbc.jar
│   ├── mysql-connector-j.jar
│   ├── slf4j-api.jar
│   └── slf4j-simple.jar
│
├── sql/
│   └── kec_schema.sql
├── data/
│   └── kec_academic.db             <- Pre-seeded SQLite database
└── docs/
    └── EXECUTION_REPORT.md
```

---

## How to Run

### Method 1: Double-Click Batch Files (Windows)
1. Double-click `run.bat` to launch the menu-driven application.
2. Double-click `run_demo.bat` to run the automated test suite with full proofs.
3. Double-click `compile.bat` if you ever make modifications to any Java source file.

### Method 2: Command Line (PowerShell / CMD)
```powershell
cd "C:\Users\Thina rajkumar\Downloads\KEC_Student_Evaluation_System"

# To compile:
javac -cp ".;lib/*" -d . *.java

# To run interactive menu:
java -cp ".;lib/*" MainApp

# To run automated demo & verification suite:
java -cp ".;lib/*" MainApp --demo
```

---

## Features Implemented According to KEC R2024

1. **Student Registration:** Roll Number (e.g. `24ITR001`), Name, Department, Semester.
2. **Course Catalog:** Code (e.g. `24ITT31`), Title, Credits, Course Type (`THEORY`, `PRACTICAL`, `EMBEDDED`).
3. **Course Enrollment & Attendance Tracking:** Total classes held vs attended, with medical condonation toggle.
4. **Incremental Marks Entry ("Then and Now"):**
   - **Phase 1 (Mid-Semester):** CIA 1 (0–100) & CIA 2 (0–100).
   - **Phase 2 (End of Semester - CIA):** Assignments / Quizzes / Lab Evaluations (0–100).
   - **Phase 3 (Post Semester):** ESE Raw Marks (0–100) or Absence (AB).
5. **KEC R2024 Evaluation Rules:**
   - **THEORY:** CIA = 40% | ESE = 60% | ESE Min Threshold = 45% (27/60) | Overall Pass Min = 50.
   - **PRACTICAL:** CIA = 60% | ESE = 40% | ESE Min Threshold = 50% (20/40) | Overall Pass Min = 50.
   - **EMBEDDED:** CIA = 50% | ESE = 50% | ESE Min Threshold = 45% (22.5/50) | Overall Pass Min = 50.
6. **Attendance & Grading Rules:**
   - $\ge 75\%$: Eligible for ESE.
   - $65\% \text{ to } 74.9\%$: Conditionally eligible (Medical condonation required).
   - $< 65\%$: Award **SA (Shortage of Attendance)** grade (0 Grade Points), barred from ESE.
   - 10-Point Scale: `O` (10), `A+` (9), `A` (8), `B+` (7), `B` (6), `RA` (0), `SA` (0), `AB` (0).
7. **SGPA & Percentage Formulas:**
   - $\text{SGPA} = \frac{\sum (\text{Credits} \times \text{Grade Points})}{\sum \text{Enrolled Credits}}$
   - $\text{Equivalent Percentage} = \text{SGPA} \times 10$
8. **Reports Generated:**
   - Student Semester Mark Sheet with comprehensive component breakdown.
   - Attendance Defaulter Report (Defaulters with SA & Condonation candidate tracking).
   - Class Performance & Result Summary with Department-wise pass/fail count and class average SGPA.
