/**
 * AssessmentMark.java
 * Holds continuous assessment (CIA) and End Semester Examination (ESE) marks.
 * Supports phased incremental entry:
 * Phase 1: cia1, cia2
 * Phase 2: assignmentLab
 * Phase 3: eseRaw, isEseAbsent
 */
public class AssessmentMark {
    private int markId;
    private int enrollmentId;
    private Double cia1;
    private Double cia2;
    private Double assignmentLab;
    private Double eseRaw;
    private boolean isEseAbsent;

    public AssessmentMark() {}

    public AssessmentMark(int markId, int enrollmentId, Double cia1, Double cia2, 
                          Double assignmentLab, Double eseRaw, boolean isEseAbsent) {
        this.markId = markId;
        this.enrollmentId = enrollmentId;
        this.cia1 = cia1;
        this.cia2 = cia2;
        this.assignmentLab = assignmentLab;
        this.eseRaw = eseRaw;
        this.isEseAbsent = isEseAbsent;
    }

    public int getMarkId() {
        return markId;
    }

    public void setMarkId(int markId) {
        this.markId = markId;
    }

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Double getCia1() {
        return cia1;
    }

    public void setCia1(Double cia1) {
        this.cia1 = cia1;
    }

    public Double getCia2() {
        return cia2;
    }

    public void setCia2(Double cia2) {
        this.cia2 = cia2;
    }

    public Double getAssignmentLab() {
        return assignmentLab;
    }

    public void setAssignmentLab(Double assignmentLab) {
        this.assignmentLab = assignmentLab;
    }

    public Double getEseRaw() {
        return eseRaw;
    }

    public void setEseRaw(Double eseRaw) {
        this.eseRaw = eseRaw;
    }

    public boolean isEseAbsent() {
        return isEseAbsent;
    }

    public void setEseAbsent(boolean eseAbsent) {
        isEseAbsent = eseAbsent;
    }

    /**
     * Computes the normalized CIA out of 100 based on recorded components.
     * KEC R2024 standard evaluation distribution:
     * - Phase 1 CIA1 (40%) + CIA2 (40%)
     * - Phase 2 Assignment/Quiz/Lab Evaluation (20%)
     */
    public Double computeNormalizedCia100(String courseType) {
        if (cia1 == null && cia2 == null && assignmentLab == null) {
            return null;
        }

        double c1 = (cia1 != null) ? cia1 : 0.0;
        double c2 = (cia2 != null) ? cia2 : 0.0;
        double al = (assignmentLab != null) ? assignmentLab : 0.0;

        if ("PRACTICAL".equalsIgnoreCase(courseType)) {
            // Practical: Model/Continuous tests (60%) + Lab Evaluation/Record (40%)
            return (c1 * 0.3) + (c2 * 0.3) + (al * 0.4);
        } else if ("EMBEDDED".equalsIgnoreCase(courseType)) {
            // Embedded: Theory CIA (70%) + Integrated Lab CIA (30%)
            return (c1 * 0.35) + (c2 * 0.35) + (al * 0.30);
        } else {
            // Theory: CIA1 (40%) + CIA2 (40%) + Assignment/Quiz (20%)
            return (c1 * 0.40) + (c2 * 0.40) + (al * 0.20);
        }
    }
}
