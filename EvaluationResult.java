/**
 * EvaluationResult.java
 * Encapsulates the complete academic performance calculation for a single course enrollment
 * following Kongu Engineering College Regulation 2024 (R2024) rules.
 */
public class EvaluationResult {
    private String rollNo;
    private String studentName;
    private String department;
    private int semester;

    private String courseCode;
    private String courseTitle;
    private int credits;
    private String courseType;

    private int totalClasses;
    private int attendedClasses;
    private double attendancePct;
    private boolean isCondoned;
    private String attendanceStatus; // ELIGIBLE, CONDONED, BARRED_SA

    private Double cia1;
    private Double cia2;
    private Double assignmentLab;
    private Double ciaNormalized100;
    private Double ciaWeighted;

    private Double eseRaw;
    private Double eseWeighted;
    private boolean isEseAbsent;
    private boolean eseThresholdPassed;

    private Double totalMarks;
    private String letterGrade;
    private int gradePoints;
    private String resultStatus; // PASS, FAIL (RA), BARRED (SA), ABSENT (AB), PENDING

    public EvaluationResult() {}

    // Getters and Setters
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }

    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

    public int getAttendedClasses() { return attendedClasses; }
    public void setAttendedClasses(int attendedClasses) { this.attendedClasses = attendedClasses; }

    public double getAttendancePct() { return attendancePct; }
    public void setAttendancePct(double attendancePct) { this.attendancePct = attendancePct; }

    public boolean isCondoned() { return isCondoned; }
    public void setCondoned(boolean condoned) { isCondoned = condoned; }

    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }

    public Double getCia1() { return cia1; }
    public void setCia1(Double cia1) { this.cia1 = cia1; }

    public Double getCia2() { return cia2; }
    public void setCia2(Double cia2) { this.cia2 = cia2; }

    public Double getAssignmentLab() { return assignmentLab; }
    public void setAssignmentLab(Double assignmentLab) { this.assignmentLab = assignmentLab; }

    public Double getCiaNormalized100() { return ciaNormalized100; }
    public void setCiaNormalized100(Double ciaNormalized100) { this.ciaNormalized100 = ciaNormalized100; }

    public Double getCiaWeighted() { return ciaWeighted; }
    public void setCiaWeighted(Double ciaWeighted) { this.ciaWeighted = ciaWeighted; }

    public Double getEseRaw() { return eseRaw; }
    public void setEseRaw(Double eseRaw) { this.eseRaw = eseRaw; }

    public Double getEseWeighted() { return eseWeighted; }
    public void setEseWeighted(Double eseWeighted) { this.eseWeighted = eseWeighted; }

    public boolean isEseAbsent() { return isEseAbsent; }
    public void setEseAbsent(boolean eseAbsent) { isEseAbsent = eseAbsent; }

    public boolean isEseThresholdPassed() { return eseThresholdPassed; }
    public void setEseThresholdPassed(boolean eseThresholdPassed) { this.eseThresholdPassed = eseThresholdPassed; }

    public Double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }

    public String getLetterGrade() { return letterGrade; }
    public void setLetterGrade(String letterGrade) { this.letterGrade = letterGrade; }

    public int getGradePoints() { return gradePoints; }
    public void setGradePoints(int gradePoints) { this.gradePoints = gradePoints; }

    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
}
