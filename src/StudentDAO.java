import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDAO.java
 * Kongu Engineering College (Autonomous) - Regulation 2024
 * Data Access Object for Student, Course Catalog, and Course Enrollment CRUD operations.
 */
public class StudentDAO {

    // =========================================================================
    // 1. STUDENT MANAGEMENT
    // =========================================================================

    public boolean addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (roll_no, name, department, semester) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getRollNo().trim().toUpperCase());
            ps.setString(2, student.getName().trim());
            ps.setString(3, student.getDepartment().trim());
            ps.setInt(4, student.getSemester());
            return ps.executeUpdate() > 0;
        }
    }

    public Student getStudent(String rollNo) throws SQLException {
        String sql = "SELECT roll_no, name, department, semester FROM students WHERE roll_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getString("roll_no"),
                            rs.getString("name"),
                            rs.getString("department"),
                            rs.getInt("semester")
                    );
                }
            }
        }
        return null;
    }

    public List<Student> getAllStudents() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT roll_no, name, department, semester FROM students ORDER BY roll_no";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Student(
                        rs.getString("roll_no"),
                        rs.getString("name"),
                        rs.getString("department"),
                        rs.getInt("semester")
                ));
            }
        }
        return list;
    }

    // =========================================================================
    // 2. COURSE CATALOG MANAGEMENT
    // =========================================================================

    public boolean addCourse(Course course) throws SQLException {
        String type = course.getCourseType().toUpperCase();
        if (!type.equals("THEORY") && !type.equals("PRACTICAL") && !type.equals("EMBEDDED")) {
            throw new IllegalArgumentException("Invalid Course Type: Must be THEORY, PRACTICAL, or EMBEDDED.");
        }

        String sql = "INSERT INTO courses (course_code, title, credits, course_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course.getCourseCode().trim().toUpperCase());
            ps.setString(2, course.getTitle().trim());
            ps.setInt(3, course.getCredits());
            ps.setString(4, type);
            return ps.executeUpdate() > 0;
        }
    }

    public Course getCourse(String courseCode) throws SQLException {
        String sql = "SELECT course_code, title, credits, course_type FROM courses WHERE course_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Course(
                            rs.getString("course_code"),
                            rs.getString("title"),
                            rs.getInt("credits"),
                            rs.getString("course_type")
                    );
                }
            }
        }
        return null;
    }

    public List<Course> getAllCourses() throws SQLException {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT course_code, title, credits, course_type FROM courses ORDER BY course_code";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Course(
                        rs.getString("course_code"),
                        rs.getString("title"),
                        rs.getInt("credits"),
                        rs.getString("course_type")
                ));
            }
        }
        return list;
    }

    // =========================================================================
    // 3. COURSE ENROLLMENT & ATTENDANCE TRACKING
    // =========================================================================

    public int enrollStudent(String rollNo, String courseCode, int semester) throws SQLException {
        rollNo = rollNo.trim().toUpperCase();
        courseCode = courseCode.trim().toUpperCase();

        if (getStudent(rollNo) == null) {
            throw new IllegalArgumentException("Student with Roll Number " + rollNo + " does not exist.");
        }
        if (getCourse(courseCode) == null) {
            throw new IllegalArgumentException("Course with Code " + courseCode + " does not exist.");
        }

        String sql = "INSERT INTO enrollments (roll_no, course_code, semester, total_classes, attended_classes, is_condoned) " +
                     "VALUES (?, ?, ?, 0, 0, 0)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rollNo);
            ps.setString(2, courseCode);
            ps.setInt(3, semester);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    // Also initialize an empty assessment_marks row for this enrollment
                    initializeAssessmentMarks(conn, id);
                    return id;
                }
            }
        }
        return -1;
    }

    private void initializeAssessmentMarks(Connection conn, int enrollmentId) {
        String sql = "INSERT OR IGNORE INTO assessment_marks (enrollment_id) VALUES (?)";
        if ("mysql".equalsIgnoreCase(DBConnection.getDbType())) {
            sql = "INSERT IGNORE INTO assessment_marks (enrollment_id) VALUES (?)";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public boolean updateAttendance(int enrollmentId, int totalClasses, int attendedClasses, boolean isCondoned) throws SQLException {
        if (attendedClasses > totalClasses || attendedClasses < 0 || totalClasses < 0) {
            throw new IllegalArgumentException("Invalid attendance count: Attended classes must be between 0 and total classes.");
        }

        String sql = "UPDATE enrollments SET total_classes = ?, attended_classes = ?, is_condoned = ? WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalClasses);
            ps.setInt(2, attendedClasses);
            ps.setBoolean(3, isCondoned);
            ps.setInt(4, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setMedicalCondonation(int enrollmentId, boolean approved) throws SQLException {
        String sql = "UPDATE enrollments SET is_condoned = ? WHERE enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, approved);
            ps.setInt(2, enrollmentId);
            return ps.executeUpdate() > 0;
        }
    }

    public Enrollment getEnrollment(int enrollmentId) throws SQLException {
        String sql = "SELECT e.enrollment_id, e.roll_no, e.course_code, e.semester, " +
                     "e.total_classes, e.attended_classes, e.is_condoned, " +
                     "s.name AS student_name, c.title AS course_title, c.course_type, c.credits " +
                     "FROM enrollments e " +
                     "JOIN students s ON e.roll_no = s.roll_no " +
                     "JOIN courses c ON e.course_code = c.course_code " +
                     "WHERE e.enrollment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Enrollment en = new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getString("roll_no"),
                            rs.getString("course_code"),
                            rs.getInt("semester"),
                            rs.getInt("total_classes"),
                            rs.getInt("attended_classes"),
                            rs.getBoolean("is_condoned")
                    );
                    en.setStudentName(rs.getString("student_name"));
                    en.setCourseTitle(rs.getString("course_title"));
                    en.setCourseType(rs.getString("course_type"));
                    en.setCredits(rs.getInt("credits"));
                    return en;
                }
            }
        }
        return null;
    }

    public Enrollment getEnrollmentByRollAndCourse(String rollNo, String courseCode) throws SQLException {
        String sql = "SELECT e.enrollment_id, e.roll_no, e.course_code, e.semester, " +
                     "e.total_classes, e.attended_classes, e.is_condoned, " +
                     "s.name AS student_name, c.title AS course_title, c.course_type, c.credits " +
                     "FROM enrollments e " +
                     "JOIN students s ON e.roll_no = s.roll_no " +
                     "JOIN courses c ON e.course_code = c.course_code " +
                     "WHERE UPPER(e.roll_no) = ? AND UPPER(e.course_code) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setString(2, courseCode.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Enrollment en = new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getString("roll_no"),
                            rs.getString("course_code"),
                            rs.getInt("semester"),
                            rs.getInt("total_classes"),
                            rs.getInt("attended_classes"),
                            rs.getBoolean("is_condoned")
                    );
                    en.setStudentName(rs.getString("student_name"));
                    en.setCourseTitle(rs.getString("course_title"));
                    en.setCourseType(rs.getString("course_type"));
                    en.setCredits(rs.getInt("credits"));
                    return en;
                }
            }
        }
        return null;
    }

    public List<Enrollment> getEnrollmentsByStudent(String rollNo, int semester) throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        String sql = "SELECT e.enrollment_id, e.roll_no, e.course_code, e.semester, " +
                     "e.total_classes, e.attended_classes, e.is_condoned, " +
                     "s.name AS student_name, c.title AS course_title, c.course_type, c.credits " +
                     "FROM enrollments e " +
                     "JOIN students s ON e.roll_no = s.roll_no " +
                     "JOIN courses c ON e.course_code = c.course_code " +
                     "WHERE e.roll_no = ? AND e.semester = ? " +
                     "ORDER BY e.course_code";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            ps.setInt(2, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Enrollment en = new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getString("roll_no"),
                            rs.getString("course_code"),
                            rs.getInt("semester"),
                            rs.getInt("total_classes"),
                            rs.getInt("attended_classes"),
                            rs.getBoolean("is_condoned")
                    );
                    en.setStudentName(rs.getString("student_name"));
                    en.setCourseTitle(rs.getString("course_title"));
                    en.setCourseType(rs.getString("course_type"));
                    en.setCredits(rs.getInt("credits"));
                    list.add(en);
                }
            }
        }
        return list;
    }

    public List<Enrollment> getAllEnrollments() throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        String sql = "SELECT e.enrollment_id, e.roll_no, e.course_code, e.semester, " +
                     "e.total_classes, e.attended_classes, e.is_condoned, " +
                     "s.name AS student_name, c.title AS course_title, c.course_type, c.credits " +
                     "FROM enrollments e " +
                     "JOIN students s ON e.roll_no = s.roll_no " +
                     "JOIN courses c ON e.course_code = c.course_code " +
                     "ORDER BY e.semester, e.roll_no, e.course_code";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Enrollment en = new Enrollment(
                        rs.getInt("enrollment_id"),
                        rs.getString("roll_no"),
                        rs.getString("course_code"),
                        rs.getInt("semester"),
                        rs.getInt("total_classes"),
                        rs.getInt("attended_classes"),
                        rs.getBoolean("is_condoned")
                );
                en.setStudentName(rs.getString("student_name"));
                en.setCourseTitle(rs.getString("course_title"));
                en.setCourseType(rs.getString("course_type"));
                en.setCredits(rs.getInt("credits"));
                list.add(en);
            }
        }
        return list;
    }
}
