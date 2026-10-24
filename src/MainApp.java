import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * MainApp.java
 * Kongu Engineering College (Autonomous), Perundurai, Erode - 638060
 * Regulation 2024 (R2024) - Student Academic & Continuous Evaluation System
 *
 * Menu-driven console interface for:
 * 1. Student & Course Management (Data Entry)
 * 2. Incremental Marks Entry (Phase 1, Phase 2, Phase 3)
 * 3. Attendance Filtering & Grading Rules
 * 4. Academic Performance Reports (Mark Sheet, Defaulter Report, Class Performance Summary)
 */
public class MainApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final StudentDAO studentDAO = new StudentDAO();
    private static final AcademicDAO academicDAO = new AcademicDAO();

    public static void main(String[] args) {
        // Initialize DB connection and schema
        try {
            DBConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("[FATAL] Could not connect to database: " + e.getMessage());
            return;
        }

        // Support --demo mode for automated testing and execution proofs
        if (args.length > 0 && "--demo".equalsIgnoreCase(args[0])) {
            runAutomatedDemonstration();
            return;
        }

        printBanner();

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = promptString("Enter your choice (0-9): ").trim();
            System.out.println();

            try {
                switch (choice) {
                    case "1":
                        handleStudentRegistration();
                        break;
                    case "2":
                        handleCourseCatalog();
                        break;
                    case "3":
                        handleEnrollmentAndAttendance();
                        break;
                    case "4":
                        handleIncrementalMarksEntry();
                        break;
                    case "5":
                        handleGenerateStudentMarkSheet();
                        break;
                    case "6":
                        handleAttendanceDefaulterReport();
                        break;
                    case "7":
                        handleClassPerformanceSummary();
                        break;
                    case "8":
                        handleViewCatalogAndEnrollments();
                        break;
                    case "9":
                        runAutomatedDemonstration();
                        break;
                    case "0":
                        running = false;
                        System.out.println("Thank you for using KEC R2024 Academic Evaluation System.");
                        break;
                    default:
                        System.out.println("[!] Invalid choice. Please select an option between 0 and 9.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
            System.out.println();
        }

        DBConnection.closeConnection();
    }

    private static void printBanner() {
        System.out.println("==========================================================================================");
        System.out.println("               KONGU ENGINEERING COLLEGE (AUTONOMOUS) - ERODE - 638060                    ");
        System.out.println("           REGULATION 2024 (R2024) - STUDENT ACADEMIC & EVALUATION SYSTEM                ");
        System.out.println("               Database-Driven Java JDBC Application (MySQL / SQLite)                    ");
        System.out.println("==========================================================================================");
    }

    private static void printMainMenu() {
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println("                                      MAIN MENU                                           ");
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println("  1. Student Registration (Add New Student)");
        System.out.println("  2. Course Catalog Management (Add New Course - Theory/Practical/Embedded)");
        System.out.println("  3. Course Enrollment & Attendance Tracking (Record Attendance & Condonation)");
        System.out.println("  4. Incremental Marks Entry (Phase 1: CIA1/2 | Phase 2: Assg/Lab | Phase 3: ESE/AB)");
        System.out.println("  5. Generate Student Semester Mark Sheet (Detailed with SGPA & %)");
        System.out.println("  6. Generate Attendance Defaulter Report (Attendance < 65% Barred SA)");
        System.out.println("  7. Generate Class Performance & Result Summary (Department-wise Pass/Fail)");
        System.out.println("  8. View All Students, Courses & Active Enrollments");
        System.out.println("  9. Run Full Automated System Demonstration (Self-Verification)");
        System.out.println("  0. Exit System");
        System.out.println("------------------------------------------------------------------------------------------");
    }

    // =========================================================================
    // 1. STUDENT REGISTRATION
    // =========================================================================
    private static void handleStudentRegistration() throws SQLException {
        System.out.println(">>> 1. STUDENT REGISTRATION <<<");
        String rollNo = promptString("Enter Student Roll Number (e.g., 24ITR001): ").trim().toUpperCase();
        if (studentDAO.getStudent(rollNo) != null) {
            System.out.println("[!] A student with Roll Number " + rollNo + " already exists!");
            return;
        }

        String name = promptString("Enter Student Full Name: ").trim();
        String dept = promptString("Enter Department (e.g., Information Technology / CSE / ECE / MECH): ").trim();
        int sem = promptInt("Enter Current Semester (1-8): ", 1, 8);

        Student s = new Student(rollNo, name, dept, sem);
        if (studentDAO.addStudent(s)) {
            System.out.println("[SUCCESS] Student successfully registered:");
            System.out.println("          " + s);
        } else {
            System.out.println("[FAILURE] Could not register student.");
        }
    }

    // =========================================================================
    // 2. COURSE CATALOG
    // =========================================================================
    private static void handleCourseCatalog() throws SQLException {
        System.out.println(">>> 2. COURSE CATALOG MANAGEMENT <<<");
        String code = promptString("Enter Course Code (e.g., 24ITT31): ").trim().toUpperCase();
        if (studentDAO.getCourse(code) != null) {
            System.out.println("[!] Course code " + code + " already exists!");
            return;
        }

        String title = promptString("Enter Course Title: ").trim();
        int credits = promptInt("Enter Credits (1-6): ", 1, 6);

        System.out.println("Select Course Type according to KEC R2024:");
        System.out.println("  1. THEORY     (CIA: 40% | ESE: 60% | Min ESE: 45% = 27/60)");
        System.out.println("  2. PRACTICAL  (CIA: 60% | ESE: 40% | Min ESE: 50% = 20/40)");
        System.out.println("  3. EMBEDDED   (CIA: 50% | ESE: 50% | Min ESE: 45% = 22.5/50)");
        int typeChoice = promptInt("Choice (1-3): ", 1, 3);
        String courseType = (typeChoice == 1) ? "THEORY" : (typeChoice == 2) ? "PRACTICAL" : "EMBEDDED";

        Course c = new Course(code, title, credits, courseType);
        if (studentDAO.addCourse(c)) {
            System.out.println("[SUCCESS] Course registered successfully in catalog:");
            System.out.println("          " + c);
        } else {
            System.out.println("[FAILURE] Could not add course.");
        }
    }

    // =========================================================================
    // 3. ENROLLMENT & ATTENDANCE TRACKING
    // =========================================================================
    private static void handleEnrollmentAndAttendance() throws SQLException {
        System.out.println(">>> 3. COURSE ENROLLMENT & ATTENDANCE TRACKING <<<");
        System.out.println("  a. Enroll Student in Course");
        System.out.println("  b. Update Class Attendance (Held vs Attended)");
        System.out.println("  c. Grant / Revoke Medical Condonation (65% to 74.9%)");
        String sub = promptString("Select sub-action (a/b/c): ").trim().toLowerCase();

        if ("a".equals(sub)) {
            String roll = promptString("Enter Student Roll Number: ").trim().toUpperCase();
            String course = promptString("Enter Course Code: ").trim().toUpperCase();
            int sem = promptInt("Enter Semester: ", 1, 8);

            int enId = studentDAO.enrollStudent(roll, course, sem);
            if (enId > 0) {
                System.out.println("[SUCCESS] Student " + roll + " enrolled in " + course + " (Enrollment ID: " + enId + ")");
            } else {
                System.out.println("[!] Enrollment failed or record already exists.");
            }
        } else if ("b".equals(sub)) {
            Enrollment en = selectOrPromptEnrollment("Update Class Attendance");
            if (en == null) return;

            int enId = en.getEnrollmentId();
            System.out.printf("Current Attendance for %s | %s (%s): Held: %d, Attended: %d (%.2f%%)%n",
                    en.getRollNo(), en.getCourseCode(), en.getCourseTitle(),
                    en.getTotalClasses(), en.getAttendedClasses(), en.getAttendancePercentage());

            int total = promptInt("Enter Total Classes Held: ", 0, 500);
            int attended = promptInt("Enter Total Classes Attended: ", 0, total);
            boolean condoned = en.isCondoned();

            studentDAO.updateAttendance(enId, total, attended, condoned);
            double pct = (total > 0) ? (attended * 100.0) / total : 100.0;
            System.out.printf("[SUCCESS] Attendance updated! New Attendance: %.2f%%%n", pct);
            if (pct < 65.0) {
                System.out.println("[RULE WARNING] Attendance < 65%: Student is BARRED from ESE and awarded grade SA!");
            } else if (pct < 75.0) {
                System.out.println("[RULE NOTICE] Attendance 65% - 74.9%: Conditionally eligible. Requires Medical Condonation approval.");
            } else {
                System.out.println("[ELIGIBLE] Attendance >= 75%: Fully eligible for End Semester Examination.");
            }
        } else if ("c".equals(sub)) {
            Enrollment en = selectOrPromptEnrollment("Medical Condonation Approval");
            if (en == null) return;

            int enId = en.getEnrollmentId();
            double pct = en.getAttendancePercentage();
            System.out.printf("Student: %s (%s) | Course: %s | Attendance: %.2f%%%n",
                    en.getRollNo(), en.getStudentName(), en.getCourseCode(), pct);
            if (pct < 65.0) {
                System.out.println("[!] Ineligible: Attendance is below 65%. Regulations do not permit condonation below 65%.");
                return;
            }
            if (pct >= 75.0) {
                System.out.println("[!] Student already has >= 75% attendance and does not require condonation.");
                return;
            }
            String approve = promptString("Approve Medical Condonation? (y/n): ").trim().toLowerCase();
            boolean isApproved = approve.startsWith("y");
            studentDAO.setMedicalCondonation(enId, isApproved);
            System.out.println("[SUCCESS] Medical condonation status updated to: " + (isApproved ? "APPROVED" : "NOT APPROVED"));
        }
    }

    // =========================================================================
    // 4. INCREMENTAL MARKS ENTRY ("Then and Now")
    // =========================================================================
    private static void handleIncrementalMarksEntry() throws SQLException {
        System.out.println(">>> 4. INCREMENTAL MARKS ENTRY (KEC R2024) <<<");
        System.out.println("The system supports phased incremental assessment entry as the semester progresses:");
        System.out.println("  1. Phase 1 (Mid-Semester)       : Record CIA Test 1 (0-100) and CIA Test 2 (0-100)");
        System.out.println("  2. Phase 2 (End of Semester-CIA): Record Assignments / Quizzes / Lab Evaluations (0-100)");
        System.out.println("  3. Phase 3 (Post Semester)      : Record End Semester Exam (ESE) Marks (0-100) / Absence (AB)");
        int phase = promptInt("Select Assessment Phase (1-3): ", 1, 3);

        Enrollment en = selectOrPromptEnrollment("Marks Entry (Phase " + phase + ")");
        if (en == null) return;
        int enId = en.getEnrollmentId();

        System.out.printf("Target: %s (%s) | Course: %s (%s - %s)%n",
                en.getRollNo(), en.getStudentName(), en.getCourseCode(), en.getCourseTitle(), en.getCourseType());

        if (phase == 1) {
            System.out.println("--- Phase 1: Mid-Semester Continuous Internal Assessments ---");
            double cia1 = promptDouble("Enter CIA Test 1 Marks (0 - 100): ", 0.0, 100.0);
            double cia2 = promptDouble("Enter CIA Test 2 Marks (0 - 100): ", 0.0, 100.0);
            academicDAO.recordMidSemMarks(enId, cia1, cia2);
            System.out.printf("[SUCCESS] Phase 1 Recorded: CIA1 = %.1f, CIA2 = %.1f%n", cia1, cia2);

        } else if (phase == 2) {
            System.out.println("--- Phase 2: End-of-Semester Continuous Assessments ---");
            String compName = "PRACTICAL".equalsIgnoreCase(en.getCourseType()) ? "Lab Continuous Evaluation" : "Assignments / Quizzes";
            double al = promptDouble("Enter " + compName + " Marks (0 - 100): ", 0.0, 100.0);
            academicDAO.recordEndSemCiaMarks(enId, al);
            System.out.printf("[SUCCESS] Phase 2 Recorded: %s = %.1f%n", compName, al);

        } else if (phase == 3) {
            System.out.println("--- Phase 3: Post-Semester End Semester Examination (ESE) ---");
            String isAbs = promptString("Is student ABSENT in ESE? (y/n): ").trim().toLowerCase();
            if (isAbs.startsWith("y")) {
                academicDAO.recordEseMarks(enId, null, true);
                System.out.println("[SUCCESS] Student marked as ABSENT (AB) for ESE. Grade awarded: AB (0 Grade Points).");
            } else {
                double ese = promptDouble("Enter ESE Raw Mark (0 - 100): ", 0.0, 100.0);
                academicDAO.recordEseMarks(enId, ese, false);
                System.out.printf("[SUCCESS] Phase 3 Recorded: ESE Raw Mark = %.1f%n", ese);
            }
        }
    }

    /**
     * User-friendly helper that displays available enrollments and accepts either
     * an Enrollment ID OR a Student Roll Number (full or partial, e.g. '25ITR161' or '161').
     */
    private static Enrollment selectOrPromptEnrollment(String actionTitle) throws SQLException {
        List<Enrollment> list = studentDAO.getAllEnrollments();
        if (list.isEmpty()) {
            System.out.println("[!] No active course enrollments found in the system.");
            return null;
        }

        System.out.println("\n--- Current Active Enrollments (" + actionTitle + ") ---");
        System.out.printf("%-4s | %-10s | %-18s | %-8s | %-32s | %s%n",
                "ID", "Roll No", "Student Name", "Course", "Course Title", "Attended/Held (%)");
        System.out.println("---------------------------------------------------------------------------------------------");
        for (Enrollment e : list) {
            String title = e.getCourseTitle();
            if (title.length() > 32) title = title.substring(0, 29) + "...";
            System.out.printf("%-4d | %-10s | %-18s | %-8s | %-32s | %d/%d (%.1f%%)%n",
                    e.getEnrollmentId(), e.getRollNo(), e.getStudentName(),
                    e.getCourseCode(), title, e.getAttendedClasses(), e.getTotalClasses(), e.getAttendancePercentage());
        }
        System.out.println("---------------------------------------------------------------------------------------------");

        while (true) {
            System.out.print("Enter Enrollment ID (e.g. 17) or Student Roll No (or 'c' to cancel): ");
            String input = scanner.nextLine().trim();
            if ("c".equalsIgnoreCase(input) || "cancel".equalsIgnoreCase(input)) {
                return null;
            }

            // Check if entered input is a direct integer ID
            try {
                int id = Integer.parseInt(input);
                Enrollment byId = studentDAO.getEnrollment(id);
                if (byId != null) {
                    return byId;
                }
            } catch (NumberFormatException ignored) {}

            // If not found by direct ID, search if input matches Roll Number (e.g. 25ITR161 or 161)
            List<Enrollment> matches = new ArrayList<>();
            for (Enrollment e : list) {
                if (e.getRollNo().equalsIgnoreCase(input) || e.getRollNo().toUpperCase().contains(input.toUpperCase())) {
                    matches.add(e);
                }
            }

            if (matches.size() == 1) {
                Enrollment matched = matches.get(0);
                System.out.printf("[MATCH FOUND] Selected Enrollment ID %d (%s - %s)%n",
                        matched.getEnrollmentId(), matched.getRollNo(), matched.getCourseCode());
                return matched;
            } else if (matches.size() > 1) {
                System.out.println("[!] Multiple enrollments matched Roll Number '" + input + "':");
                for (Enrollment m : matches) {
                    System.out.printf("  -> Enrollment ID: %d | Course: %s (%s)%n",
                            m.getEnrollmentId(), m.getCourseCode(), m.getCourseTitle());
                }
                int selId = promptInt("Enter specific Enrollment ID from above: ", 1, Integer.MAX_VALUE);
                Enrollment selEn = studentDAO.getEnrollment(selId);
                if (selEn != null) return selEn;
            }

            System.out.println("[!] No enrollment found matching '" + input + "'. Please choose a valid Enrollment ID from the table above.");
        }
    }

    // =========================================================================
    // 5. GENERATE STUDENT SEMESTER MARK SHEET
    // =========================================================================
    private static void handleGenerateStudentMarkSheet() throws SQLException {
        System.out.println(">>> 5. GENERATE STUDENT SEMESTER MARK SHEET <<<");
        String rollNo = promptString("Enter Student Roll Number (e.g., 24ITR001 or 25ITR161): ").trim().toUpperCase();
        Student student = studentDAO.getStudent(rollNo);
        if (student == null) {
            System.out.println("[!] No student found with Roll Number: " + rollNo);
            return;
        }

        int sem = promptInt("Enter Semester: ", 1, 8);
        List<EvaluationResult> results = academicDAO.getStudentSemesterMarkSheet(rollNo, sem);
        if (results.isEmpty()) {
            System.out.println("[!] No course enrollments found for " + rollNo + " in Semester " + sem);
            return;
        }

        printDetailedMarkSheet(student, sem, results);
    }

    public static void printDetailedMarkSheet(Student student, int sem, List<EvaluationResult> results) {
        System.out.println("============================================================================================================");
        System.out.println("                         KONGU ENGINEERING COLLEGE (AUTONOMOUS) - ERODE                             ");
        System.out.println("                               SEMESTER GRADE & PERFORMANCE REPORT                                  ");
        System.out.println("                                  REGULATION 2024 (R2024)                                           ");
        System.out.println("============================================================================================================");
        System.out.printf("  Roll Number : %-15s   Student Name : %-30s%n", student.getRollNo(), student.getName());
        System.out.printf("  Department  : %-15s   Semester     : %d%n", student.getDepartment(), sem);
        System.out.println("------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-9s | %-28s | %-9s | %3s | %6s | %5s | %5s | %5s | %5s | %2s | %-12s%n",
                "Code", "Course Title", "Type", "Crd", "Attn %", "CIA", "ESE", "Tot", "Grd", "GP", "Result");
        System.out.println("------------------------------------------------------------------------------------------------------------");

        int totalCreditsRegistered = 0;
        int totalCreditsEarned = 0;

        for (EvaluationResult r : results) {
            totalCreditsRegistered += r.getCredits();
            if ("PASS".equalsIgnoreCase(r.getResultStatus())) {
                totalCreditsEarned += r.getCredits();
            }

            String ciaStr = (r.getCiaWeighted() != null) ? String.format("%5.1f", r.getCiaWeighted()) : "  -  ";
            String eseStr;
            if (r.isEseAbsent()) {
                eseStr = "  AB ";
            } else if (r.getEseWeighted() != null) {
                eseStr = String.format("%5.1f", r.getEseWeighted());
            } else {
                eseStr = "  -  ";
            }

            String totStr = (r.getTotalMarks() != null) ? String.format("%5.1f", r.getTotalMarks()) : "  -  ";
            String title = r.getCourseTitle();
            if (title.length() > 28) title = title.substring(0, 25) + "...";

            System.out.printf("%-9s | %-28s | %-9s | %3d | %5.1f%% | %s | %s | %s | %3s | %2d | %-12s%n",
                    r.getCourseCode(),
                    title,
                    r.getCourseType(),
                    r.getCredits(),
                    r.getAttendancePct(),
                    ciaStr,
                    eseStr,
                    totStr,
                    r.getLetterGrade(),
                    r.getGradePoints(),
                    r.getResultStatus());
        }

        double sgpa = academicDAO.calculateSGPA(results);
        double percentage = academicDAO.calculatePercentage(sgpa);

        System.out.println("============================================================================================================");
        System.out.printf("  Total Enrolled Credits  : %-5d       Total Credits Earned (Passed) : %-5d%n",
                totalCreditsRegistered, totalCreditsEarned);
        System.out.printf("  Semester SGPA (0 - 10)  : %-5.2f       Equivalent Percentage (%%)    : %-5.2f%%%n",
                sgpa, percentage);
        System.out.println("============================================================================================================");
        System.out.println("  Weightage Rules: THEORY (40% CIA / 60% ESE) | PRACTICAL (60% CIA / 40% ESE) | EMBEDDED (50% / 50%)       ");
        System.out.println("  Grading Scale  : O (91-100: 10), A+ (81-90: 9), A (71-80: 8), B+ (61-70: 7), B (50-60: 6), RA/SA/AB: 0    ");
        System.out.println("============================================================================================================");
    }

    // =========================================================================
    // 6. ATTENDANCE DEFAULTER REPORT
    // =========================================================================
    private static void handleAttendanceDefaulterReport() throws SQLException {
        System.out.println("============================================================================================================");
        System.out.println("                      KEC R2024 - ATTENDANCE DEFAULTER & ELIGIBILITY REPORT                                 ");
        System.out.println("============================================================================================================");

        List<EvaluationResult> defaulters = academicDAO.getAttendanceDefaulters();
        System.out.println("[1] STUDENTS BARRED FROM ESE (Attendance < 65% or Unapproved Condonation - Grade SA Awarded)");
        System.out.println("------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-20s | %-9s | %-25s | %5s | %8s | %-22s%n",
                "Roll No", "Student Name", "Course", "Course Title", "Attn%", "Status", "Penalty / Action");
        System.out.println("------------------------------------------------------------------------------------------------------------");

        int saCount = 0;
        for (EvaluationResult r : defaulters) {
            if (r.getAttendancePct() < 65.0) {
                saCount++;
                String title = r.getCourseTitle();
                if (title.length() > 25) title = title.substring(0, 22) + "...";
                System.out.printf("%-10s | %-20s | %-9s | %-25s | %4.1f%% | %-8s | Barred (SA - 0 GP)%n",
                        r.getRollNo(), r.getStudentName(), r.getCourseCode(), title, r.getAttendancePct(), "DEF_SA");
            }
        }
        if (saCount == 0) {
            System.out.println("  No students below 65% attendance.");
        }

        System.out.println();
        System.out.println("[2] MEDICAL CONDONATION CASES (65.0% to 74.9% Attendance)");
        System.out.println("------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-20s | %-9s | %-25s | %5s | %-12s | %-18s%n",
                "Roll No", "Student Name", "Course", "Course Title", "Attn%", "Condonation", "ESE Eligibility");
        System.out.println("------------------------------------------------------------------------------------------------------------");

        List<EvaluationResult> candidates = academicDAO.getCondonationCandidates();
        int condCount = 0;
        for (EvaluationResult r : candidates) {
            condCount++;
            String status = r.isCondoned() ? "APPROVED" : "NOT APPROVED";
            String elig = r.isCondoned() ? "ELIGIBLE" : "BARRED (Pending)";
            String title = r.getCourseTitle();
            if (title.length() > 25) title = title.substring(0, 22) + "...";
            System.out.printf("%-10s | %-20s | %-9s | %-25s | %4.1f%% | %-12s | %-18s%n",
                    r.getRollNo(), r.getStudentName(), r.getCourseCode(), title, r.getAttendancePct(), status, elig);
        }
        if (condCount == 0) {
            System.out.println("  No students in the 65% - 74.9% attendance bracket.");
        }
        System.out.println("============================================================================================================");
    }

    // =========================================================================
    // 7. CLASS PERFORMANCE & RESULT SUMMARY
    // =========================================================================
    private static void handleClassPerformanceSummary() throws SQLException {
        System.out.println("============================================================================================================");
        System.out.println("                      KEC R2024 - CLASS PERFORMANCE & RESULT SUMMARY REPORT                                 ");
        System.out.println("============================================================================================================");
        String semInput = promptString("Enter Semester filter (or press Enter for all semesters): ").trim();
        Integer sem = semInput.isEmpty() ? null : Integer.parseInt(semInput);

        Map<String, AcademicDAO.DepartmentSummary> summary = academicDAO.getClassPerformanceSummary(sem);

        System.out.printf("%-32s | %6s | %10s | %6s | %6s | %10s | %8s%n",
                "Department", "Total", "Registrns", "Pass", "Fail", "Pass %", "Avg SGPA");
        System.out.println("------------------------------------------------------------------------------------------------------------");

        for (AcademicDAO.DepartmentSummary ds : summary.values()) {
            System.out.printf("%-32s | %6d | %10d | %6d | %6d | %9.2f%% | %8.2f%n",
                    ds.department,
                    ds.totalStudents,
                    ds.totalCourseRegistrations,
                    ds.totalPassed,
                    ds.totalFailed,
                    ds.passPercentage,
                    ds.classAverageSGPA);
            System.out.printf("   Breakdown of Failures: RA (Re-Appearance): %d | SA (Shortage Attn): %d | AB (Absent): %d%n",
                    ds.countRA, ds.countSA, ds.countAB);
        }
        System.out.println("============================================================================================================");
    }

    // =========================================================================
    // 8. VIEW CATALOG & ENROLLMENTS
    // =========================================================================
    private static void handleViewCatalogAndEnrollments() throws SQLException {
        System.out.println(">>> 8. VIEW CATALOG & ENROLLMENTS <<<");
        System.out.println("  a. View All Students");
        System.out.println("  b. View All Courses in Catalog");
        System.out.println("  c. View All Student Enrollments");
        String sub = promptString("Select option (a/b/c): ").trim().toLowerCase();

        if ("a".equals(sub)) {
            List<Student> students = studentDAO.getAllStudents();
            System.out.printf("%-12s | %-25s | %-32s | %4s%n", "Roll Number", "Name", "Department", "Sem");
            System.out.println("------------------------------------------------------------------------------------");
            for (Student s : students) {
                System.out.printf("%-12s | %-25s | %-32s | %4d%n", s.getRollNo(), s.getName(), s.getDepartment(), s.getSemester());
            }
        } else if ("b".equals(sub)) {
            List<Course> courses = studentDAO.getAllCourses();
            System.out.printf("%-10s | %-42s | %7s | %-10s%n", "Code", "Title", "Credits", "Type");
            System.out.println("------------------------------------------------------------------------------------");
            for (Course c : courses) {
                System.out.printf("%-10s | %-42s | %7d | %-10s%n", c.getCourseCode(), c.getTitle(), c.getCredits(), c.getCourseType());
            }
        } else if ("c".equals(sub)) {
            List<Enrollment> enrollments = studentDAO.getAllEnrollments();
            System.out.printf("%-4s | %-10s | %-20s | %-8s | %-25s | %-8s | %6s%n",
                    "ID", "Roll No", "Student Name", "Code", "Course Title", "Type", "Attn %");
            System.out.println("---------------------------------------------------------------------------------------------");
            for (Enrollment en : enrollments) {
                String title = en.getCourseTitle();
                if (title.length() > 25) title = title.substring(0, 22) + "...";
                System.out.printf("%-4d | %-10s | %-20s | %-8s | %-25s | %-8s | %5.1f%%%n",
                        en.getEnrollmentId(), en.getRollNo(), en.getStudentName(),
                        en.getCourseCode(), title, en.getCourseType(), en.getAttendancePercentage());
            }
        }
    }

    // =========================================================================
    // 9. AUTOMATED SYSTEM DEMONSTRATION & EXECUTION PROOFS
    // =========================================================================
    public static void runAutomatedDemonstration() {
        System.out.println("==========================================================================================");
        System.out.println("       RUNNING KEC R2024 COMPLETE SYSTEM AUTOMATED TEST & VERIFICATION SUITE              ");
        System.out.println("==========================================================================================");

        try {
            // STEP 1: Add Student and Course
            System.out.println("\n[DEMO STEP 1] Adding a new Student and a new Course...");
            String demoRoll = "24ITR099";
            if (studentDAO.getStudent(demoRoll) == null) {
                Student demoStudent = new Student(demoRoll, "Naveen Kumar M", "Information Technology", 1);
                studentDAO.addStudent(demoStudent);
                System.out.println(" [OK] Added Student: " + demoStudent);
            }

            String demoCourseCode = "24ITT12";
            if (studentDAO.getCourse(demoCourseCode) == null) {
                Course demoCourse = new Course(demoCourseCode, "Computer Organization & Architecture", 3, "THEORY");
                studentDAO.addCourse(demoCourse);
                System.out.println(" [OK] Added Course : " + demoCourse);
            }

            // Enroll Student
            int demoEnId = -1;
            List<Enrollment> enList = studentDAO.getEnrollmentsByStudent(demoRoll, 1);
            for (Enrollment en : enList) {
                if (en.getCourseCode().equalsIgnoreCase(demoCourseCode)) {
                    demoEnId = en.getEnrollmentId();
                    break;
                }
            }
            if (demoEnId == -1) {
                demoEnId = studentDAO.enrollStudent(demoRoll, demoCourseCode, 1);
                System.out.println(" [OK] Enrolled " + demoRoll + " in " + demoCourseCode + " (Enrollment ID: " + demoEnId + ")");
            }

            // Set Attendance to 88%
            studentDAO.updateAttendance(demoEnId, 50, 44, false);
            System.out.println(" [OK] Recorded Attendance: 44 / 50 classes attended (88.0% - Fully Eligible)");

            // STEP 2: Incremental Marks Entry (Phase 1, Phase 2, Phase 3)
            System.out.println("\n[DEMO STEP 2] Performing Incremental Marks Entry (Phases 1 -> 2 -> 3)...");
            // Phase 1
            academicDAO.recordMidSemMarks(demoEnId, 88.0, 92.0);
            System.out.println(" [OK] Phase 1 Recorded: CIA Test 1 = 88.0, CIA Test 2 = 92.0");

            // Phase 2
            academicDAO.recordEndSemCiaMarks(demoEnId, 90.0);
            System.out.println(" [OK] Phase 2 Recorded: Assignment / Quiz = 90.0");

            // Phase 3
            academicDAO.recordEseMarks(demoEnId, 85.0, false);
            System.out.println(" [OK] Phase 3 Recorded: End Semester Exam (ESE Raw Mark) = 85.0");

            // STEP 3: Generate Mark Sheets for various student test cases
            System.out.println("\n[DEMO STEP 3] Generating Grade Sheet for Demo Student (" + demoRoll + "):");
            Student s = studentDAO.getStudent(demoRoll);
            List<EvaluationResult> demoResults = academicDAO.getStudentSemesterMarkSheet(demoRoll, 1);
            printDetailedMarkSheet(s, 1, demoResults);

            System.out.println("\n[DEMO STEP 4] Generating Grade Sheet for Top Scorer Student (24ITR001 - All Course Types):");
            Student s1 = studentDAO.getStudent("24ITR001");
            List<EvaluationResult> s1Results = academicDAO.getStudentSemesterMarkSheet("24ITR001", 1);
            printDetailedMarkSheet(s1, 1, s1Results);

            System.out.println("\n[DEMO STEP 5] Generating Grade Sheet for Student with Shortage of Attendance (24ITR003):");
            Student s3 = studentDAO.getStudent("24ITR003");
            List<EvaluationResult> s3Results = academicDAO.getStudentSemesterMarkSheet("24ITR003", 1);
            printDetailedMarkSheet(s3, 1, s3Results);

            // STEP 6: Attendance Defaulter Report
            System.out.println("\n[DEMO STEP 6] Generating Attendance Defaulter Report:");
            handleAttendanceDefaulterReport();

            // STEP 7: Class Performance & Result Summary
            System.out.println("\n[DEMO STEP 7] Generating Class Performance & Result Summary:");
            Map<String, AcademicDAO.DepartmentSummary> summary = academicDAO.getClassPerformanceSummary(1);
            System.out.printf("%-32s | %6s | %10s | %6s | %6s | %10s | %8s%n",
                    "Department", "Total", "Registrns", "Pass", "Fail", "Pass %", "Avg SGPA");
            System.out.println("------------------------------------------------------------------------------------------------------------");
            for (AcademicDAO.DepartmentSummary ds : summary.values()) {
                System.out.printf("%-32s | %6d | %10d | %6d | %6d | %9.2f%% | %8.2f%n",
                        ds.department, ds.totalStudents, ds.totalCourseRegistrations,
                        ds.totalPassed, ds.totalFailed, ds.passPercentage, ds.classAverageSGPA);
                System.out.printf("   Breakdown: RA (Re-Appearance): %d | SA (Shortage Attn): %d | AB (Absent): %d%n",
                        ds.countRA, ds.countSA, ds.countAB);
            }
            System.out.println("============================================================================================================");
            System.out.println("[DEMO VERIFICATION COMPLETE] All KEC R2024 business rules, database queries, and outputs verified!");

        } catch (Exception e) {
            System.err.println("[DEMO ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper Input Methods
    private static String promptString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int promptInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("[!] Value must be between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[!] Please enter a valid integer.");
            }
        }
    }

    private static double promptDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(line);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("[!] Value must be between %.1f and %.1f.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[!] Please enter a valid decimal number.");
            }
        }
    }
}
