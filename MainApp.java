import java.util.*;

/**
 * KEC (Autonomous) - Regulation 2024
 * Student Academic & Continuous Evaluation System
 * MainApp.java
 */
public class MainApp {
    private static final AcademicDAO dao = new AcademicDAO();
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        if (!DBConnection.testConnection()) {
            System.out.println("Database connection failed. Check DBConnection.java.");
            return;
        }

        System.out.println("==========================================================================================");
        System.out.println("               KONGU ENGINEERING COLLEGE (AUTONOMOUS) - ERODE - 638060                    ");
        System.out.println("           REGULATION 2024 (R2024) - STUDENT ACADEMIC & EVALUATION SYSTEM                ");
        System.out.println("==========================================================================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1.Student 2.Course 3.Enrollment/Attendance");
            System.out.println("4.Enter Marks 5.Mark Sheet 6.Defaulters");
            System.out.println("7.Department Summary 0.Exit");
            System.out.print("Choice: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    registerStudent();
                    break;
                case "2":
                    addCourse();
                    break;
                case "3":
                    enrollStudent();
                    break;
                case "4":
                    enterMarks();
                    break;
                case "5":
                    System.out.print("Roll number: ");
                    String roll = sc.nextLine().trim();
                    System.out.print("Semester: ");
                    printStudentMarkSheet(roll, Integer.parseInt(sc.nextLine().trim()));
                    break;
                case "6":
                    System.out.print("Semester: ");
                    printAttendanceDefaulters(Integer.parseInt(sc.nextLine().trim()));
                    break;
                case "7":
                    System.out.print("Semester: ");
                    printDepartmentSummary(Integer.parseInt(sc.nextLine().trim()));
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        System.out.println("System closed.");
    }

    private static void registerStudent() {
        System.out.println("\n--- Student Registration ---");
        System.out.print("Roll Number (e.g., 24ITR001): ");
        String roll = sc.nextLine().trim().toUpperCase();
        System.out.print("Student Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Department (e.g., Information Technology): ");
        String dept = sc.nextLine().trim();
        System.out.print("Current Semester (1-8): ");
        int sem = Integer.parseInt(sc.nextLine().trim());

        if (dao.registerStudent(roll, name, dept, sem)) {
            System.out.println("[SUCCESS] Student registered: " + roll + " - " + name);
        } else {
            System.out.println("[FAILED] Could not register student (may already exist).");
        }
    }

    private static void addCourse() {
        System.out.println("\n--- Add Course to Catalog ---");
        System.out.print("Course Code (e.g., 24ITT31): ");
        String code = sc.nextLine().trim().toUpperCase();
        System.out.print("Course Title: ");
        String title = sc.nextLine().trim();
        System.out.print("Credits (1-6): ");
        int credits = Integer.parseInt(sc.nextLine().trim());
        System.out.println("Course Type: 1.THEORY (40/60) 2.PRACTICAL (60/40) 3.EMBEDDED (50/50)");
        System.out.print("Select (1-3): ");
        String typeChoice = sc.nextLine().trim();
        String type = typeChoice.equals("2") ? "PRACTICAL" : typeChoice.equals("3") ? "EMBEDDED" : "THEORY";

        if (dao.addCourse(code, title, credits, type)) {
            System.out.println("[SUCCESS] Course added: " + code + " - " + title + " (" + type + ")");
        } else {
            System.out.println("[FAILED] Could not add course.");
        }
    }

    private static void enrollStudent() {
        System.out.println("\n--- Course Enrollment & Attendance ---");
        System.out.println("1. New Enrollment | 2. Update Attendance (Held vs Attended)");
        System.out.print("Choice: ");
        String sub = sc.nextLine().trim();

        if (sub.equals("1")) {
            System.out.print("Roll Number: ");
            String roll = sc.nextLine().trim().toUpperCase();
            System.out.print("Course Code: ");
            String course = sc.nextLine().trim().toUpperCase();
            System.out.print("Semester: ");
            int sem = Integer.parseInt(sc.nextLine().trim());

            int enId = dao.enrollStudent(roll, course, sem);
            if (enId > 0) {
                System.out.println("[SUCCESS] Enrolled " + roll + " in " + course + " (Enrollment ID: " + enId + ")");
            } else {
                System.out.println("[FAILED] Enrollment failed or student/course does not exist.");
            }
        } else if (sub.equals("2")) {
            System.out.print("Roll Number: ");
            String roll = sc.nextLine().trim().toUpperCase();
            System.out.print("Course Code: ");
            String course = sc.nextLine().trim().toUpperCase();
            System.out.print("Semester: ");
            int sem = Integer.parseInt(sc.nextLine().trim());

            int id = dao.getEnrollmentId(roll, course, sem);
            if (id <= 0) {
                System.out.println("Enrollment not found.");
                return;
            }

            System.out.print("Total Classes Held: ");
            int total = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Classes Attended: ");
            int att = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Medical Condonation Approved (y/n)? ");
            boolean condoned = sc.nextLine().trim().equalsIgnoreCase("y");

            if (dao.updateAttendance(id, total, att, condoned)) {
                double pct = (total > 0) ? (att * 100.0) / total : 100.0;
                System.out.printf("[SUCCESS] Attendance updated: %d/%d (%.1f%%)%n", att, total, pct);
                if (pct < 65.0) {
                    System.out.println("[RULE WARNING] Attendance < 65%: Student is BARRED from ESE and awarded grade SA!");
                } else if (pct < 75.0) {
                    System.out.println("[RULE NOTICE] Attendance 65%-74.9%: " + (condoned ? "Condonation approved (Eligible)." : "Conditionally eligible (Condonation required)."));
                } else {
                    System.out.println("[ELIGIBLE] Attendance >= 75%: Fully eligible for ESE.");
                }
            } else {
                System.out.println("[FAILED] Could not update attendance.");
            }
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private static void enterMarks() {
        System.out.print("Roll number: ");
        String roll = sc.nextLine().trim();
        System.out.print("Course code: ");
        String course = sc.nextLine().trim();
        System.out.print("Semester: ");
        int sem = Integer.parseInt(sc.nextLine().trim());

        int id = dao.getEnrollmentId(roll, course, sem);
        if (id <= 0) {
            System.out.println("Enrollment not found.");
            return;
        }

        System.out.println("1.CIA marks 2.Assignment marks 3.ESE marks");
        String phase = sc.nextLine().trim();
        if (phase.equals("1")) {
            System.out.print("CIA-1: ");
            double c1 = Double.parseDouble(sc.nextLine().trim());
            System.out.print("CIA-2: ");
            double c2 = Double.parseDouble(sc.nextLine().trim());
            System.out.println(dao.recordPhase1Marks(id, c1, c2) ? "Saved." : "Failed.");
        } else if (phase.equals("2")) {
            System.out.print("Assignment: ");
            double a = Double.parseDouble(sc.nextLine().trim());
            System.out.println(dao.recordPhase2Marks(id, a) ? "Saved." : "Failed.");
        } else if (phase.equals("3")) {
            System.out.print("Absent in ESE? (y/n): ");
            boolean absent = sc.nextLine().trim().equalsIgnoreCase("y");
            Double marks = null;
            if (!absent) {
                System.out.print("ESE marks: ");
                marks = Double.parseDouble(sc.nextLine().trim());
            }
            System.out.println(dao.recordPhase3Marks(id, marks, absent) ? "Saved." : "Failed.");
        } else {
            System.out.println("Invalid phase.");
        }
    }

    private static void printStudentMarkSheet(String roll, int semester) {
        dao.printStudentMarkSheet(roll, semester);
    }

    private static void printAttendanceDefaulters(int semester) {
        dao.printAttendanceDefaulters(semester);
    }

    private static void printDepartmentSummary(int semester) {
        dao.printDepartmentSummary(semester);
    }
}
