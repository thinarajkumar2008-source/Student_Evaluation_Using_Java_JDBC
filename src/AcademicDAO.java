import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AcademicDAO.java
 * Kongu Engineering College (Autonomous) - Regulation 2024
 * Implements incremental assessment marks entry (Phase 1, 2, 3),
 * KEC R2024 evaluation rules, weightage engine, attendance filtering,
 * 10-point letter grading, SGPA computation, and reporting queries.
 */
public class AcademicDAO {

    // =========================================================================
    // 1. INCREMENTAL MARKS ENTRY ("Then and Now")
    // =========================================================================

    /**
     * Phase 1 (Mid-Semester): Record CIA Test 1 (0-100) and CIA Test 2 (0-100)
     */
    public boolean recordMidSemMarks(int enrollmentId, double cia1, double cia2) throws SQLException {
        validateMarksRange(cia1, "CIA Test 1");
        validateMarksRange(cia2, "CIA Test 2");
        ensureAssessmentRecord(enrollmentId);

        String sql = "UPDATE assessment_marks SET cia1 = ?, cia2 = ? WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, cia1);
            ps.setDouble(2, cia2);
            ps.setInt(3, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Phase 2 (End of Semester - CIA): Record Assignments / Quizzes / Lab Evaluations (0-100)
     */
    public boolean recordEndSemCiaMarks(int enrollmentId, double assignmentLab) throws SQLException {
        validateMarksRange(assignmentLab, "Assignment / Quiz / Lab Evaluation");
        ensureAssessmentRecord(enrollmentId);

        String sql = "UPDATE assessment_marks SET assignment_lab = ? WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, assignmentLab);
            ps.setInt(2, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Phase 3 (Post Semester): Record End Semester Examination (ESE) Marks (0-100) or Mark Absence (AB)
     * Enforces attendance eligibility check: Students with SA (<65% or unapproved condonation) are barred!
     */
    public boolean recordEseMarks(int enrollmentId, Double eseRaw, boolean isAbsent) throws SQLException {
        StudentDAO studentDAO = new StudentDAO();
        Enrollment enrollment = studentDAO.getEnrollment(enrollmentId);
        if (enrollment == null) {
            throw new IllegalArgumentException("Enrollment record not found for ID: " + enrollmentId);
        }

        // Attendance check before allowing ESE entry
        double attendancePct = enrollment.getAttendancePercentage();
        if (attendancePct < 65.0) {
            throw new IllegalStateException("Student has attendance of " + String.format("%.2f", attendancePct) +
                    "%, which is below 65%. Barred from ESE (Awarded Grade SA).");
        } else if (attendancePct < 75.0 && !enrollment.isCondoned()) {
            throw new IllegalStateException("Student attendance is " + String.format("%.2f", attendancePct) +
                    "% (between 65% and 74.9%). Medical condonation approval is required before ESE entry.");
        }

        if (!isAbsent) {
            if (eseRaw == null) {
                throw new IllegalArgumentException("ESE mark is required when student is present.");
            }
            validateMarksRange(eseRaw, "End Semester Examination Mark");
        }

        ensureAssessmentRecord(enrollmentId);

        String sql = "UPDATE assessment_marks SET ese_raw = ?, is_ese_absent = ? WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (isAbsent || eseRaw == null) {
                ps.setNull(1, java.sql.Types.DOUBLE);
            } else {
                ps.setDouble(1, eseRaw);
            }
            ps.setBoolean(2, isAbsent);
            ps.setInt(3, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    private void ensureAssessmentRecord(int enrollmentId) throws SQLException {
        String checkSql = "SELECT mark_id FROM assessment_marks WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    String insertSql = "INSERT INTO assessment_marks (enrollment_id) VALUES (?)";
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        insertPs.setInt(1, enrollmentId);
                        insertPs.executeUpdate();
                    }
                }
            }
        }
    }

    private void validateMarksRange(double mark, String fieldName) {
        if (mark < 0.0 || mark > 100.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100. Given: " + mark);
        }
    }

    public AssessmentMark getAssessmentMark(int enrollmentId) throws SQLException {
        String sql = "SELECT mark_id, enrollment_id, cia1, cia2, assignment_lab, ese_raw, is_ese_absent " +
                     "FROM assessment_marks WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Double cia1 = (Double) rs.getObject("cia1");
                    Double cia2 = (Double) rs.getObject("cia2");
                    Double al = (Double) rs.getObject("assignment_lab");
                    Double ese = (Double) rs.getObject("ese_raw");
                    boolean absent = rs.getBoolean("is_ese_absent");
                    return new AssessmentMark(rs.getInt("mark_id"), enrollmentId, cia1, cia2, al, ese, absent);
                }
            }
        }
        return null;
    }

    // =========================================================================
    // 2. KEC R2024 EVALUATION RULES & WEIGHTAGE ENGINE
    // =========================================================================

    public EvaluationResult evaluateEnrollment(int enrollmentId) throws SQLException {
        String sql = "SELECT e.enrollment_id, e.roll_no, e.course_code, e.semester, " +
                     "e.total_classes, e.attended_classes, e.is_condoned, " +
                     "s.name AS student_name, s.department, " +
                     "c.title AS course_title, c.course_type, c.credits, " +
                     "m.cia1, m.cia2, m.assignment_lab, m.ese_raw, m.is_ese_absent " +
                     "FROM enrollments e " +
                     "JOIN students s ON e.roll_no = s.roll_no " +
                     "JOIN courses c ON e.course_code = c.course_code " +
                     "LEFT JOIN assessment_marks m ON e.enrollment_id = m.enrollment_id " +
                     "WHERE e.enrollment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return buildEvaluationResult(rs);
                }
            }
        }
        return null;
    }

    public List<EvaluationResult> getStudentSemesterMarkSheet(String rollNo, int semester) throws SQLException {
        List<EvaluationResult> list = new ArrayList<>();
        String sql = "SELECT e.enrollment_id, e.roll_no, e.course_code, e.semester, " +
                     "e.total_classes, e.attended_classes, e.is_condoned, " +
                     "s.name AS student_name, s.department, " +
                     "c.title AS course_title, c.course_type, c.credits, " +
                     "m.cia1, m.cia2, m.assignment_lab, m.ese_raw, m.is_ese_absent " +
                     "FROM enrollments e " +
                     "JOIN students s ON e.roll_no = s.roll_no " +
                     "JOIN courses c ON e.course_code = c.course_code " +
                     "LEFT JOIN assessment_marks m ON e.enrollment_id = m.enrollment_id " +
                     "WHERE e.roll_no = ? AND e.semester = ? " +
                     "ORDER BY e.course_code";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setInt(2, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildEvaluationResult(rs));
                }
            }
        }
        return list;
    }

    /**
     * Applies KEC Regulation 2024 Evaluation Engine
     */
    private EvaluationResult buildEvaluationResult(ResultSet rs) throws SQLException {
        EvaluationResult res = new EvaluationResult();
        res.setRollNo(rs.getString("roll_no"));
        res.setStudentName(rs.getString("student_name"));
        res.setDepartment(rs.getString("department"));
        res.setSemester(rs.getInt("semester"));

        res.setCourseCode(rs.getString("course_code"));
        res.setCourseTitle(rs.getString("course_title"));
        res.setCredits(rs.getInt("credits"));
        String courseType = rs.getString("course_type").toUpperCase();
        res.setCourseType(courseType);

        int totalClasses = rs.getInt("total_classes");
        int attendedClasses = rs.getInt("attended_classes");
        boolean isCondoned = rs.getBoolean("is_condoned");
        res.setTotalClasses(totalClasses);
        res.setAttendedClasses(attendedClasses);
        res.setCondoned(isCondoned);

        double attendancePct = (totalClasses > 0) ? (attendedClasses * 100.0) / totalClasses : 100.0;
        res.setAttendancePct(attendancePct);

        Double cia1 = (Double) rs.getObject("cia1");
        Double cia2 = (Double) rs.getObject("cia2");
        Double assignmentLab = (Double) rs.getObject("assignment_lab");
        Double eseRaw = (Double) rs.getObject("ese_raw");
        boolean isEseAbsent = rs.getBoolean("is_ese_absent");

        res.setCia1(cia1);
        res.setCia2(cia2);
        res.setAssignmentLab(assignmentLab);
        res.setEseRaw(eseRaw);
        res.setEseAbsent(isEseAbsent);

        // ---------------------------------------------------------------------
        // 1. Attendance Filtering (KEC R2024)
        // ---------------------------------------------------------------------
        if (attendancePct < 65.0) {
            // Shortage of Attendance (SA): Barred from ESE, 0 Grade Points
            res.setAttendanceStatus("BARRED (SA)");
            res.setLetterGrade("SA");
            res.setGradePoints(0);
            res.setResultStatus("FAIL (SA - Shortage of Attendance)");
            res.setCiaNormalized100(computeNormalizedCia(courseType, cia1, cia2, assignmentLab));
            res.setCiaWeighted(computeCiaWeighted(courseType, res.getCiaNormalized100()));
            res.setTotalMarks(res.getCiaWeighted());
            return res;
        } else if (attendancePct < 75.0) {
            if (isCondoned) {
                res.setAttendanceStatus("CONDONED (Eligible)");
            } else {
                res.setAttendanceStatus("CONDONATION_PENDING (Barred until approved)");
                res.setLetterGrade("SA");
                res.setGradePoints(0);
                res.setResultStatus("FAIL (SA - Medical Condonation Not Approved)");
                res.setCiaNormalized100(computeNormalizedCia(courseType, cia1, cia2, assignmentLab));
                res.setCiaWeighted(computeCiaWeighted(courseType, res.getCiaNormalized100()));
                res.setTotalMarks(res.getCiaWeighted());
                return res;
            }
        } else {
            res.setAttendanceStatus("ELIGIBLE");
        }

        // ---------------------------------------------------------------------
        // 2. CIA Calculation
        // ---------------------------------------------------------------------
        Double ciaNorm = computeNormalizedCia(courseType, cia1, cia2, assignmentLab);
        res.setCiaNormalized100(ciaNorm);
        Double ciaWeighted = computeCiaWeighted(courseType, ciaNorm);
        res.setCiaWeighted(ciaWeighted);

        // ---------------------------------------------------------------------
        // 3. ESE Examination & Absence Filter
        // ---------------------------------------------------------------------
        if (isEseAbsent) {
            res.setLetterGrade("AB");
            res.setGradePoints(0);
            res.setResultStatus("ABSENT (AB)");
            res.setTotalMarks(ciaWeighted);
            return res;
        }

        if (eseRaw == null) {
            res.setLetterGrade("PENDING");
            res.setGradePoints(0);
            res.setResultStatus("PENDING (ESE Not Entered)");
            res.setTotalMarks(ciaWeighted);
            return res;
        }

        // Calculate ESE Weighted Score and check threshold
        // THEORY: CIA=40%, ESE=60%, ESE Min=45% (27/60), Pass=50
        // PRACTICAL: CIA=60%, ESE=40%, ESE Min=50% (20/40), Pass=50
        // EMBEDDED: CIA=50%, ESE=50%, ESE Min=45% (22.5/50), Pass=50
        double eseWtMultiplier;
        double eseThreshold; // out of weighted ESE
        if ("THEORY".equals(courseType)) {
            eseWtMultiplier = 0.60;
            eseThreshold = 27.0; // 45% of 60
        } else if ("PRACTICAL".equals(courseType)) {
            eseWtMultiplier = 0.40;
            eseThreshold = 20.0; // 50% of 40
        } else { // EMBEDDED
            eseWtMultiplier = 0.50;
            eseThreshold = 22.5; // 45% of 50
        }

        double eseWeighted = eseRaw * eseWtMultiplier;
        res.setEseWeighted(eseWeighted);
        boolean thresholdPassed = eseWeighted >= (eseThreshold - 0.001); // floating point tolerance
        res.setEseThresholdPassed(thresholdPassed);

        double totalMarks = (ciaWeighted != null ? ciaWeighted : 0.0) + eseWeighted;
        res.setTotalMarks(totalMarks);

        // ---------------------------------------------------------------------
        // 4. Final Grading & Pass/Fail Decision
        // ---------------------------------------------------------------------
        // Overall Pass Minimum = 50 Marks AND ESE threshold met
        if (!thresholdPassed || totalMarks < 50.0) {
            res.setLetterGrade("RA");
            res.setGradePoints(0);
            res.setResultStatus("FAIL (RA - Re-Appearance)");
        } else {
            res.setResultStatus("PASS");
            // 10-Point Grading System:
            // O (91 - 100) -> 10
            // A+ (81 - 90) -> 9
            // A (71 - 80)  -> 8
            // B+ (61 - 70) -> 7
            // B (50 - 60)  -> 6
            int roundedTotal = (int) Math.round(totalMarks);
            if (roundedTotal >= 91) {
                res.setLetterGrade("O");
                res.setGradePoints(10);
            } else if (roundedTotal >= 81) {
                res.setLetterGrade("A+");
                res.setGradePoints(9);
            } else if (roundedTotal >= 71) {
                res.setLetterGrade("A");
                res.setGradePoints(8);
            } else if (roundedTotal >= 61) {
                res.setLetterGrade("B+");
                res.setGradePoints(7);
            } else {
                res.setLetterGrade("B");
                res.setGradePoints(6);
            }
        }

        return res;
    }

    private Double computeNormalizedCia(String courseType, Double cia1, Double cia2, Double assignmentLab) {
        if (cia1 == null && cia2 == null && assignmentLab == null) {
            return null;
        }
        double c1 = (cia1 != null) ? cia1 : 0.0;
        double c2 = (cia2 != null) ? cia2 : 0.0;
        double al = (assignmentLab != null) ? assignmentLab : 0.0;

        if ("PRACTICAL".equalsIgnoreCase(courseType)) {
            return (c1 * 0.30) + (c2 * 0.30) + (al * 0.40);
        } else if ("EMBEDDED".equalsIgnoreCase(courseType)) {
            return (c1 * 0.35) + (c2 * 0.35) + (al * 0.30);
        } else {
            return (c1 * 0.40) + (c2 * 0.40) + (al * 0.20);
        }
    }

    private Double computeCiaWeighted(String courseType, Double ciaNorm) {
        if (ciaNorm == null) return null;
        if ("THEORY".equalsIgnoreCase(courseType)) {
            return ciaNorm * 0.40;
        } else if ("PRACTICAL".equalsIgnoreCase(courseType)) {
            return ciaNorm * 0.60;
        } else { // EMBEDDED
            return ciaNorm * 0.50;
        }
    }

    // =========================================================================
    // 3. SGPA & PERCENTAGE FORMULAS
    // =========================================================================

    public double calculateSGPA(List<EvaluationResult> results) {
        int totalCredits = 0;
        double totalWeightedPoints = 0.0;

        for (EvaluationResult r : results) {
            totalCredits += r.getCredits();
            totalWeightedPoints += (r.getCredits() * r.getGradePoints());
        }

        if (totalCredits == 0) return 0.0;
        return totalWeightedPoints / totalCredits;
    }

    public double calculatePercentage(double sgpa) {
        return sgpa * 10.0;
    }

    // =========================================================================
    // 4. ATTENDANCE DEFAULTER REPORT QUERY
    // =========================================================================

    public List<EvaluationResult> getAttendanceDefaulters() throws SQLException {
        List<EvaluationResult> defaulters = new ArrayList<>();
        List<Integer> enIds = new ArrayList<>();
        String sql = "SELECT enrollment_id FROM enrollments WHERE total_classes > 0 ORDER BY roll_no, course_code";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                enIds.add(rs.getInt("enrollment_id"));
            }
        }

        for (int enId : enIds) {
            EvaluationResult res = evaluateEnrollment(enId);
            if (res != null && (res.getAttendancePct() < 65.0 || (res.getAttendancePct() < 75.0 && !res.isCondoned()))) {
                defaulters.add(res);
            }
        }
        return defaulters;
    }

    public List<EvaluationResult> getCondonationCandidates() throws SQLException {
        List<EvaluationResult> condonations = new ArrayList<>();
        List<Integer> enIds = new ArrayList<>();
        String sql = "SELECT enrollment_id FROM enrollments WHERE total_classes > 0 ORDER BY roll_no, course_code";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                enIds.add(rs.getInt("enrollment_id"));
            }
        }

        for (int enId : enIds) {
            EvaluationResult res = evaluateEnrollment(enId);
            if (res != null && res.getAttendancePct() >= 65.0 && res.getAttendancePct() < 75.0) {
                condonations.add(res);
            }
        }
        return condonations;
    }

    // =========================================================================
    // 5. CLASS PERFORMANCE & RESULT SUMMARY
    // =========================================================================

    public static class DepartmentSummary {
        public String department;
        public int totalStudents;
        public int totalCourseRegistrations;
        public int totalPassed;
        public int totalFailed; // RA + SA + AB
        public int countRA;
        public int countSA;
        public int countAB;
        public double passPercentage;
        public double classAverageSGPA;
    }

    public Map<String, DepartmentSummary> getClassPerformanceSummary(Integer semester) throws SQLException {
        Map<String, DepartmentSummary> summaryMap = new LinkedHashMap<>();

        // Group students by department
        StudentDAO studentDAO = new StudentDAO();
        List<Student> students = studentDAO.getAllStudents();

        // Map department -> list of roll numbers
        Map<String, List<String>> deptStudents = new LinkedHashMap<>();
        for (Student s : students) {
            if (semester != null && s.getSemester() != semester) {
                continue;
            }
            deptStudents.computeIfAbsent(s.getDepartment(), k -> new ArrayList<>()).add(s.getRollNo());
        }

        for (Map.Entry<String, List<String>> entry : deptStudents.entrySet()) {
            String dept = entry.getKey();
            List<String> rollNos = entry.getValue();

            DepartmentSummary ds = new DepartmentSummary();
            ds.department = dept;
            ds.totalStudents = rollNos.size();

            double sumSGPA = 0.0;
            int studentsWithSGPA = 0;

            for (String roll : rollNos) {
                int targetSem = (semester != null) ? semester : studentDAO.getStudent(roll).getSemester();
                List<EvaluationResult> marksheet = getStudentSemesterMarkSheet(roll, targetSem);
                if (!marksheet.isEmpty()) {
                    double sgpa = calculateSGPA(marksheet);
                    sumSGPA += sgpa;
                    studentsWithSGPA++;

                    for (EvaluationResult r : marksheet) {
                        ds.totalCourseRegistrations++;
                        if ("PASS".equalsIgnoreCase(r.getResultStatus())) {
                            ds.totalPassed++;
                        } else {
                            ds.totalFailed++;
                            if ("RA".equalsIgnoreCase(r.getLetterGrade())) ds.countRA++;
                            else if ("SA".equalsIgnoreCase(r.getLetterGrade())) ds.countSA++;
                            else if ("AB".equalsIgnoreCase(r.getLetterGrade())) ds.countAB++;
                        }
                    }
                }
            }

            if (ds.totalCourseRegistrations > 0) {
                ds.passPercentage = (ds.totalPassed * 100.0) / ds.totalCourseRegistrations;
            }
            if (studentsWithSGPA > 0) {
                ds.classAverageSGPA = sumSGPA / studentsWithSGPA;
            }

            summaryMap.put(dept, ds);
        }

        return summaryMap;
    }
}
