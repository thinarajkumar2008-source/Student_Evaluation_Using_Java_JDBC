import java.sql.*;
import java.util.*;

/**
 * AcademicDAO - Data Access Object & Evaluation Engine
 * Kongu Engineering College (Autonomous) - Regulation 2024 (R2024)
 */
public class AcademicDAO {

    // =========================================================================
    // 1. STUDENT & COURSE REGISTRATION
    // =========================================================================

    public boolean registerStudent(String rollNo, String name, String dept, int sem) {
        String sql = "INSERT INTO students (roll_no, name, department, semester) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, name.trim());
            ps.setString(3, dept.trim());
            ps.setInt(4, sem);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Register Student: " + e.getMessage());
            return false;
        }
    }

    public boolean addCourse(String code, String title, int credits, String type) {
        String sql = "INSERT INTO courses (course_code, title, credits, course_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase());
            ps.setString(2, title.trim());
            ps.setInt(3, credits);
            ps.setString(4, type.trim().toUpperCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Add Course: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 2. ENROLLMENT & ATTENDANCE MANAGEMENT
    // =========================================================================

    public int enrollStudent(String rollNo, String courseCode, int sem) {
        String sql = "INSERT INTO enrollments (roll_no, course_code, semester, total_classes, attended_classes, is_condoned) VALUES (?, ?, ?, 0, 0, 0)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, courseCode.trim().toUpperCase());
            ps.setInt(3, sem);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int enId = rs.getInt(1);
                    ensureMarksRow(conn, enId);
                    return enId;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Enroll Student: " + e.getMessage());
        }
        return -1;
    }

    public int getEnrollmentId(String rollNo, String courseCode, int sem) {
        String sql = "SELECT enrollment_id FROM enrollments WHERE UPPER(roll_no) = ? AND UPPER(course_code) = ? AND semester = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, courseCode.trim().toUpperCase());
            ps.setInt(3, sem);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("enrollment_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Get Enrollment ID: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateAttendance(int enrollmentId, int totalClasses, int attendedClasses, boolean condoned) {
        String sql = "UPDATE enrollments SET total_classes = ?, attended_classes = ?, is_condoned = ? WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalClasses);
            ps.setInt(2, attendedClasses);
            ps.setBoolean(3, condoned);
            ps.setInt(4, enrollmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] Update Attendance: " + e.getMessage());
            return false;
        }
    }

    private void ensureMarksRow(Connection conn, int enrollmentId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO assessment_marks (enrollment_id) VALUES (?)")) {
            ps.setInt(1, enrollmentId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // =========================================================================
    // 3. INCREMENTAL MARKS ENTRY ("Then and Now")
    // =========================================================================

    public boolean recordPhase1Marks(int enrollmentId, double cia1, double cia2) {
        if (cia1 < 0 || cia1 > 100 || cia2 < 0 || cia2 > 100) {
            System.out.println("[!] Marks must be between 0 and 100.");
            return false;
        }
        String sql = "UPDATE assessment_marks SET cia1 = ?, cia2 = ? WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            ensureMarksRecord(conn, enrollmentId);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, cia1);
                ps.setDouble(2, cia2);
                ps.setInt(3, enrollmentId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Phase 1 Marks: " + e.getMessage());
            return false;
        }
    }

    public boolean recordPhase2Marks(int enrollmentId, double assignment) {
        if (assignment < 0 || assignment > 100) {
            System.out.println("[!] Marks must be between 0 and 100.");
            return false;
        }
        String sql = "UPDATE assessment_marks SET assignment_lab = ? WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            ensureMarksRecord(conn, enrollmentId);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, assignment);
                ps.setInt(2, enrollmentId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Phase 2 Marks: " + e.getMessage());
            return false;
        }
    }

    public boolean recordPhase3Marks(int enrollmentId, Double eseMarks, boolean absent) {
        // Attendance check before ESE
        try (Connection conn = DBConnection.getConnection()) {
            ensureMarksRecord(conn, enrollmentId);
            
            // Check attendance rule
            String attSql = "SELECT total_classes, attended_classes, is_condoned FROM enrollments WHERE enrollment_id = ?";
            try (PreparedStatement attPs = conn.prepareStatement(attSql)) {
                attPs.setInt(1, enrollmentId);
                try (ResultSet rs = attPs.executeQuery()) {
                    if (rs.next()) {
                        int tot = rs.getInt("total_classes");
                        int att = rs.getInt("attended_classes");
                        boolean cond = rs.getBoolean("is_condoned");
                        double pct = (tot > 0) ? (att * 100.0) / tot : 100.0;
                        if (pct < 65.0) {
                            System.out.printf("[BARRED] Attendance is %.1f%% (<65%%). Student awarded SA grade and barred from ESE!%n", pct);
                            return false;
                        } else if (pct < 75.0 && !cond) {
                            System.out.printf("[CONDITIONAL] Attendance is %.1f%% (65%%-74.9%%). Medical condonation required before ESE!%n", pct);
                            return false;
                        }
                    }
                }
            }

            String sql = "UPDATE assessment_marks SET ese_raw = ?, is_ese_absent = ? WHERE enrollment_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (absent || eseMarks == null) {
                    ps.setNull(1, Types.DOUBLE);
                } else {
                    if (eseMarks < 0 || eseMarks > 100) {
                        System.out.println("[!] ESE Marks must be between 0 and 100.");
                        return false;
                    }
                    ps.setDouble(1, eseMarks);
                }
                ps.setBoolean(2, absent);
                ps.setInt(3, enrollmentId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Phase 3 Marks: " + e.getMessage());
            return false;
        }
    }

    private void ensureMarksRecord(Connection conn, int enrollmentId) throws SQLException {
        String chk = "SELECT mark_id FROM assessment_marks WHERE enrollment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(chk)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    try (PreparedStatement ins = conn.prepareStatement("INSERT INTO assessment_marks (enrollment_id) VALUES (?)")) {
                        ins.setInt(1, enrollmentId);
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. REPORT 1: STUDENT SEMESTER MARK SHEET (KEC R2024)
    // =========================================================================

    public void printStudentMarkSheet(String rollNo, int semester) {
        String studentSql = "SELECT name, department FROM students WHERE UPPER(roll_no) = ?";
        String markSql = "SELECT e.enrollment_id, e.course_code, c.title, c.credits, c.course_type, " +
                "e.total_classes, e.attended_classes, e.is_condoned, " +
                "m.cia1, m.cia2, m.assignment_lab, m.ese_raw, m.is_ese_absent " +
                "FROM enrollments e " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "LEFT JOIN assessment_marks m ON e.enrollment_id = m.enrollment_id " +
                "WHERE UPPER(e.roll_no) = ? AND e.semester = ? " +
                "ORDER BY e.course_code";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement psStudent = conn.prepareStatement(studentSql);
             PreparedStatement psMarks = conn.prepareStatement(markSql)) {

            psStudent.setString(1, rollNo.trim().toUpperCase());
            String studentName = "";
            String dept = "";
            try (ResultSet rs = psStudent.executeQuery()) {
                if (rs.next()) {
                    studentName = rs.getString("name");
                    dept = rs.getString("department");
                } else {
                    System.out.println("[!] Student not found: " + rollNo);
                    return;
                }
            }

            psMarks.setString(1, rollNo.trim().toUpperCase());
            psMarks.setInt(2, semester);

            System.out.println("\n============================================================================================================");
            System.out.println("                         KONGU ENGINEERING COLLEGE (AUTONOMOUS) - ERODE                             ");
            System.out.println("                               SEMESTER GRADE & PERFORMANCE REPORT                                  ");
            System.out.println("                                  REGULATION 2024 (R2024)                                           ");
            System.out.println("============================================================================================================");
            System.out.printf("  Roll Number : %-15s   Student Name : %-30s%n", rollNo.toUpperCase(), studentName);
            System.out.printf("  Department  : %-15s   Semester     : %d%n", dept, semester);
            System.out.println("------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-9s | %-28s | %-9s | %3s | %6s | %5s | %5s | %5s | %5s | %2s | %-12s%n",
                    "Code", "Course Title", "Type", "Crd", "Attn %", "CIA", "ESE", "Tot", "Grd", "GP", "Result");
            System.out.println("------------------------------------------------------------------------------------------------------------");

            int totalCredits = 0;
            int earnedCredits = 0;
            double sumWeightedPoints = 0.0;
            int rowCount = 0;

            try (ResultSet rs = psMarks.executeQuery()) {
                while (rs.next()) {
                    rowCount++;
                    String code = rs.getString("course_code");
                    String title = rs.getString("title");
                    if (title.length() > 28) title = title.substring(0, 25) + "...";
                    String type = rs.getString("course_type").toUpperCase();
                    int crd = rs.getInt("credits");
                    totalCredits += crd;

                    int totalCls = rs.getInt("total_classes");
                    int attCls = rs.getInt("attended_classes");
                    boolean cond = rs.getBoolean("is_condoned");
                    double attnPct = (totalCls > 0) ? (attCls * 100.0) / totalCls : 100.0;

                    Double c1 = (Double) rs.getObject("cia1");
                    Double c2 = (Double) rs.getObject("cia2");
                    Double al = (Double) rs.getObject("assignment_lab");
                    Double eseRaw = (Double) rs.getObject("ese_raw");
                    boolean isAbsent = rs.getBoolean("is_ese_absent");

                    // R2024 Evaluation
                    String grade;
                    int gp;
                    String status;
                    Double ciaWt = null;
                    Double eseWt = null;
                    Double totalMarks = null;

                    if (attnPct < 65.0 || (attnPct < 75.0 && !cond)) {
                        grade = "SA";
                        gp = 0;
                        status = "FAIL (SA)";
                        ciaWt = calculateCiaWeighted(type, c1, c2, al);
                        totalMarks = ciaWt;
                    } else if (isAbsent) {
                        grade = "AB";
                        gp = 0;
                        status = "ABSENT (AB)";
                        ciaWt = calculateCiaWeighted(type, c1, c2, al);
                        totalMarks = ciaWt;
                    } else if (eseRaw == null) {
                        grade = "PENDING";
                        gp = 0;
                        status = "PENDING";
                        ciaWt = calculateCiaWeighted(type, c1, c2, al);
                        totalMarks = ciaWt;
                    } else {
                        ciaWt = calculateCiaWeighted(type, c1, c2, al);
                        double eseMultiplier = type.equals("THEORY") ? 0.60 : type.equals("PRACTICAL") ? 0.40 : 0.50;
                        double minEseThreshold = type.equals("THEORY") ? 27.0 : type.equals("PRACTICAL") ? 20.0 : 22.5;

                        eseWt = eseRaw * eseMultiplier;
                        totalMarks = (ciaWt != null ? ciaWt : 0.0) + eseWt;

                        boolean thresholdPassed = eseWt >= (minEseThreshold - 0.001);
                        if (!thresholdPassed || totalMarks < 50.0) {
                            grade = "RA";
                            gp = 0;
                            status = "FAIL (RA)";
                        } else {
                            status = "PASS";
                            earnedCredits += crd;
                            int roundedTot = (int) Math.round(totalMarks);
                            if (roundedTot >= 91) { grade = "O"; gp = 10; }
                            else if (roundedTot >= 81) { grade = "A+"; gp = 9; }
                            else if (roundedTot >= 71) { grade = "A"; gp = 8; }
                            else if (roundedTot >= 61) { grade = "B+"; gp = 7; }
                            else { grade = "B"; gp = 6; }
                        }
                    }

                    sumWeightedPoints += (crd * gp);

                    String ciaStr = (ciaWt != null) ? String.format("%5.1f", ciaWt) : "  -  ";
                    String eseStr = isAbsent ? "  AB " : (eseWt != null) ? String.format("%5.1f", eseWt) : "  -  ";
                    String totStr = (totalMarks != null) ? String.format("%5.1f", totalMarks) : "  -  ";

                    System.out.printf("%-9s | %-28s | %-9s | %3d | %5.1f%% | %s | %s | %s | %3s | %2d | %-12s%n",
                            code, title, type, crd, attnPct, ciaStr, eseStr, totStr, grade, gp, status);
                }
            }

            if (rowCount == 0) {
                System.out.println("  No course enrollments found for this semester.");
                return;
            }

            double sgpa = (totalCredits > 0) ? (sumWeightedPoints / totalCredits) : 0.0;
            double percentage = sgpa * 10.0;

            System.out.println("============================================================================================================");
            System.out.printf("  Total Enrolled Credits  : %-5d       Total Credits Earned (Passed) : %-5d%n", totalCredits, earnedCredits);
            System.out.printf("  Semester SGPA (0 - 10)  : %-5.2f       Equivalent Percentage (%%)    : %-5.2f%%%n", sgpa, percentage);
            System.out.println("============================================================================================================");
            System.out.println("  Weightage Rules: THEORY (40% CIA / 60% ESE) | PRACTICAL (60% CIA / 40% ESE) | EMBEDDED (50% / 50%)       ");
            System.out.println("  Grading Scale  : O (91-100: 10), A+ (81-90: 9), A (71-80: 8), B+ (61-70: 7), B (50-60: 6), RA/SA/AB: 0    ");
            System.out.println("============================================================================================================");

        } catch (SQLException e) {
            System.err.println("[ERROR] Mark sheet generation: " + e.getMessage());
        }
    }

    private Double calculateCiaWeighted(String type, Double c1, Double c2, Double al) {
        if (c1 == null && c2 == null && al == null) return null;
        double v1 = (c1 != null) ? c1 : 0.0;
        double v2 = (c2 != null) ? c2 : 0.0;
        double val = (al != null) ? al : 0.0;

        double norm100;
        if (type.equals("PRACTICAL")) norm100 = (v1 * 0.30) + (v2 * 0.30) + (val * 0.40);
        else if (type.equals("EMBEDDED")) norm100 = (v1 * 0.35) + (v2 * 0.35) + (val * 0.30);
        else norm100 = (v1 * 0.40) + (v2 * 0.40) + (val * 0.20);

        if (type.equals("THEORY")) return norm100 * 0.40;
        if (type.equals("PRACTICAL")) return norm100 * 0.60;
        return norm100 * 0.50;
    }

    // =========================================================================
    // 5. REPORT 2: ATTENDANCE DEFAULTERS
    // =========================================================================

    public void printAttendanceDefaulters(int semester) {
        String sql = "SELECT e.roll_no, s.name, e.course_code, c.title, e.total_classes, e.attended_classes, e.is_condoned " +
                "FROM enrollments e " +
                "JOIN students s ON e.roll_no = s.roll_no " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "WHERE e.semester = ? AND e.total_classes > 0 " +
                "ORDER BY e.roll_no, e.course_code";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, semester);

            System.out.println("\n============================================================================================================");
            System.out.printf("             KEC R2024 - ATTENDANCE DEFAULTER & CONDONATION REPORT (SEMESTER %d)                            %n", semester);
            System.out.println("============================================================================================================");
            System.out.println("[1] STUDENTS BARRED FROM ESE (Attendance < 65% - Awarded Grade SA, 0 Grade Points)");
            System.out.println("------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-10s | %-20s | %-9s | %-28s | %5s | %s%n",
                    "Roll No", "Student Name", "Course", "Course Title", "Attn%", "Status / Action");
            System.out.println("------------------------------------------------------------------------------------------------------------");

            int saCount = 0;
            List<String[]> condList = new ArrayList<>();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String roll = rs.getString("roll_no");
                    String name = rs.getString("name");
                    String code = rs.getString("course_code");
                    String title = rs.getString("title");
                    if (title.length() > 28) title = title.substring(0, 25) + "...";
                    int tot = rs.getInt("total_classes");
                    int att = rs.getInt("attended_classes");
                    boolean cond = rs.getBoolean("is_condoned");
                    double pct = (att * 100.0) / tot;

                    if (pct < 65.0) {
                        saCount++;
                        System.out.printf("%-10s | %-20s | %-9s | %-28s | %4.1f%% | Barred (SA Awarded)%n",
                                roll, name, code, title, pct);
                    } else if (pct < 75.0) {
                        condList.add(new String[]{roll, name, code, title, String.format("%.1f%%", pct),
                                cond ? "APPROVED (Eligible)" : "NOT APPROVED (Barred)"});
                    }
                }
            }

            if (saCount == 0) {
                System.out.println("  No students with shortage of attendance (<65%).");
            }

            System.out.println("\n[2] MEDICAL CONDONATION CASES (65.0% to 74.9% Attendance)");
            System.out.println("------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-10s | %-20s | %-9s | %-28s | %5s | %s%n",
                    "Roll No", "Student Name", "Course", "Course Title", "Attn%", "Condonation Status");
            System.out.println("------------------------------------------------------------------------------------------------------------");
            if (condList.isEmpty()) {
                System.out.println("  No students in 65% - 74.9% bracket.");
            } else {
                for (String[] row : condList) {
                    System.out.printf("%-10s | %-20s | %-9s | %-28s | %5s | %s%n",
                            row[0], row[1], row[2], row[3], row[4], row[5]);
                }
            }
            System.out.println("============================================================================================================");

        } catch (SQLException e) {
            System.err.println("[ERROR] Attendance report: " + e.getMessage());
        }
    }

    // =========================================================================
    // 6. REPORT 3: DEPARTMENT SUMMARY
    // =========================================================================

    public void printDepartmentSummary(int semester) {
        String studentSql = "SELECT DISTINCT s.roll_no, s.department FROM students s " +
                "JOIN enrollments e ON s.roll_no = e.roll_no WHERE e.semester = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(studentSql)) {
            ps.setInt(1, semester);

            Map<String, List<String>> deptMap = new LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    deptMap.computeIfAbsent(rs.getString("department"), k -> new ArrayList<>())
                            .add(rs.getString("roll_no"));
                }
            }

            System.out.println("\n============================================================================================================");
            System.out.printf("             KEC R2024 - DEPARTMENT-WISE PERFORMANCE SUMMARY (SEMESTER %d)                                  %n", semester);
            System.out.println("============================================================================================================");
            System.out.printf("%-32s | %6s | %10s | %6s | %6s | %10s | %8s%n",
                    "Department", "Total", "Registrns", "Pass", "Fail", "Pass %", "Avg SGPA");
            System.out.println("------------------------------------------------------------------------------------------------------------");

            for (Map.Entry<String, List<String>> entry : deptMap.entrySet()) {
                String dept = entry.getKey();
                List<String> rollList = entry.getValue();

                int totalRegistrations = 0;
                int passed = 0;
                int failed = 0;
                double sumSgpa = 0.0;
                int countWithSgpa = 0;

                for (String roll : rollList) {
                    double sgpa = computeStudentSgpa(conn, roll, semester);
                    sumSgpa += sgpa;
                    countWithSgpa++;

                    // Count course pass/fail
                    String courseSql = "SELECT e.course_code, c.course_type, e.total_classes, e.attended_classes, e.is_condoned, " +
                            "m.cia1, m.cia2, m.assignment_lab, m.ese_raw, m.is_ese_absent " +
                            "FROM enrollments e JOIN courses c ON e.course_code = c.course_code " +
                            "LEFT JOIN assessment_marks m ON e.enrollment_id = m.enrollment_id " +
                            "WHERE UPPER(e.roll_no) = ? AND e.semester = ?";
                    try (PreparedStatement cps = conn.prepareStatement(courseSql)) {
                        cps.setString(1, roll.toUpperCase());
                        cps.setInt(2, semester);
                        try (ResultSet crs = cps.executeQuery()) {
                            while (crs.next()) {
                                totalRegistrations++;
                                if (isCoursePassed(crs)) passed++;
                                else failed++;
                            }
                        }
                    }
                }

                double passPct = (totalRegistrations > 0) ? (passed * 100.0) / totalRegistrations : 0.0;
                double avgSgpa = (countWithSgpa > 0) ? (sumSgpa / countWithSgpa) : 0.0;

                System.out.printf("%-32s | %6d | %10d | %6d | %6d | %9.2f%% | %8.2f%n",
                        dept, rollList.size(), totalRegistrations, passed, failed, passPct, avgSgpa);
            }
            System.out.println("============================================================================================================");

        } catch (SQLException e) {
            System.err.println("[ERROR] Department summary: " + e.getMessage());
        }
    }

    private double computeStudentSgpa(Connection conn, String roll, int sem) throws SQLException {
        String sql = "SELECT c.credits, c.course_type, e.total_classes, e.attended_classes, e.is_condoned, " +
                "m.cia1, m.cia2, m.assignment_lab, m.ese_raw, m.is_ese_absent " +
                "FROM enrollments e JOIN courses c ON e.course_code = c.course_code " +
                "LEFT JOIN assessment_marks m ON e.enrollment_id = m.enrollment_id " +
                "WHERE UPPER(e.roll_no) = ? AND e.semester = ?";
        int totCrd = 0;
        double weightedPoints = 0.0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roll.toUpperCase());
            ps.setInt(2, sem);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int crd = rs.getInt("credits");
                    totCrd += crd;
                    int gp = calculateGradePoints(rs);
                    weightedPoints += (crd * gp);
                }
            }
        }
        return (totCrd > 0) ? (weightedPoints / totCrd) : 0.0;
    }

    private boolean isCoursePassed(ResultSet rs) throws SQLException {
        return calculateGradePoints(rs) >= 6;
    }

    private int calculateGradePoints(ResultSet rs) throws SQLException {
        int tot = rs.getInt("total_classes");
        int att = rs.getInt("attended_classes");
        boolean cond = rs.getBoolean("is_condoned");
        double pct = (tot > 0) ? (att * 100.0) / tot : 100.0;
        if (pct < 65.0 || (pct < 75.0 && !cond)) return 0; // SA

        if (rs.getBoolean("is_ese_absent")) return 0; // AB
        Double ese = (Double) rs.getObject("ese_raw");
        if (ese == null) return 0;

        String type = rs.getString("course_type").toUpperCase();
        Double c1 = (Double) rs.getObject("cia1");
        Double c2 = (Double) rs.getObject("cia2");
        Double al = (Double) rs.getObject("assignment_lab");

        Double ciaWt = calculateCiaWeighted(type, c1, c2, al);
        double mult = type.equals("THEORY") ? 0.60 : type.equals("PRACTICAL") ? 0.40 : 0.50;
        double minEse = type.equals("THEORY") ? 27.0 : type.equals("PRACTICAL") ? 20.0 : 22.5;

        double eseWt = ese * mult;
        if (eseWt < (minEse - 0.001)) return 0; // RA

        double totalMarks = (ciaWt != null ? ciaWt : 0.0) + eseWt;
        if (totalMarks < 50.0) return 0; // RA

        int rounded = (int) Math.round(totalMarks);
        if (rounded >= 91) return 10;
        if (rounded >= 81) return 9;
        if (rounded >= 71) return 8;
        if (rounded >= 61) return 7;
        return 6;
    }
}
