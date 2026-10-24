/**
 * Course.java
 * Represents course catalog entry conforming to KEC Regulation 2024 Course Types:
 * THEORY, PRACTICAL, or EMBEDDED
 */
public class Course {
    private String courseCode;
    private String title;
    private int credits;
    private String courseType; // THEORY, PRACTICAL, EMBEDDED

    public Course() {}

    public Course(String courseCode, String title, int credits, String courseType) {
        this.courseCode = courseCode;
        this.title = title;
        this.credits = credits;
        this.courseType = courseType.toUpperCase();
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType.toUpperCase();
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-40s | %d Credits | %-10s", courseCode, title, credits, courseType);
    }
}
