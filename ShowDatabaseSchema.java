/**
 * ShowDatabaseSchema - Utility to display the exact KEC R2024 Database Schema
 * matching MySQL DESCRIBE outputs.
 */
public class ShowDatabaseSchema {

    public static void main(String[] args) {
        System.out.println("DATABASE SCHEMA:\n");

        printStudentsTable();
        System.out.println();
        printCoursesTable();
        System.out.println();
        printEnrollmentsTable();
        System.out.println();
        printAssessmentMarksTable();
    }

    private static void printStudentsTable() {
        System.out.println("1.STUDENTS TABLE:");
        System.out.println("mysql> describe students;");
        System.out.println("+------------------+--------------+------+-----+-------------------+-------------------+");
        System.out.println("| Field            | Type         | Null | Key | Default           | Extra             |");
        System.out.println("+------------------+--------------+------+-----+-------------------+-------------------+");
        System.out.println("| roll_no          | varchar(20)  | NO   | PRI | NULL              |                   |");
        System.out.println("| name             | varchar(100) | NO   |     | NULL              |                   |");
        System.out.println("| department       | varchar(20)  | NO   |     | NULL              |                   |");
        System.out.println("| current_semester | int          | NO   |     | 3                 |                   |");
        System.out.println("| created_at       | timestamp    | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED |");
        System.out.println("+------------------+--------------+------+-----+-------------------+-------------------+");
        System.out.println("5 rows in set (0.03 sec)");
    }

    private static void printCoursesTable() {
        System.out.println("2. COURSES TABLE:");
        System.out.println("mysql> describe courses;");
        System.out.println("+-------------+------------------------------------+------+-----+-------------------+-------------------+");
        System.out.println("| Field       | Type                               | Null | Key | Default           | Extra             |");
        System.out.println("+-------------+------------------------------------+------+-----+-------------------+-------------------+");
        System.out.println("| course_code | varchar(20)                        | NO   | PRI | NULL              |                   |");
        System.out.println("| title       | varchar(150)                       | NO   |     | NULL              |                   |");
        System.out.println("| credits     | int                                | NO   |     | NULL              |                   |");
        System.out.println("| course_type | enum('THEORY','PRACTICAL','EMBEDDED')| NO   |     | NULL              |                   |");
        System.out.println("| semester    | int                                | NO   |     | 3                 |                   |");
        System.out.println("| created_at  | timestamp                          | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED |");
        System.out.println("+-------------+------------------------------------+------+-----+-------------------+-------------------+");
        System.out.println("6 rows in set (0.01 sec)");
    }

    private static void printEnrollmentsTable() {
        System.out.println("3. ENROLLMENTS TABLE:");
        System.out.println("mysql> describe enrollments;");
        System.out.println("+------------------+-------------+------+-----+-------------------+-------------------+");
        System.out.println("| Field            | Type        | Null | Key | Default           | Extra             |");
        System.out.println("+------------------+-------------+------+-----+-------------------+-------------------+");
        System.out.println("| enrollment_id    | int         | NO   | PRI | NULL              | auto_increment    |");
        System.out.println("| roll_no          | varchar(20) | NO   | MUL | NULL              |                   |");
        System.out.println("| course_code      | varchar(20) | NO   | MUL | NULL              |                   |");
        System.out.println("| semester         | int         | NO   |     | 3                 |                   |");
        System.out.println("| total_classes    | int         | NO   |     | 0                 |                   |");
        System.out.println("| attended_classes | int         | NO   |     | 0                 |                   |");
        System.out.println("| is_condoned      | tinyint(1)  | NO   |     | 0                 |                   |");
        System.out.println("| created_at       | timestamp   | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED |");
        System.out.println("+------------------+-------------+------+-----+-------------------+-------------------+");
        System.out.println("8 rows in set (0.01 sec)");
    }

    private static void printAssessmentMarksTable() {
        System.out.println("4. ASSESSMENT MARKS TABLE:");
        System.out.println("mysql> describe assessment_marks;");
        System.out.println("+------------------+--------------+------+-----+-------------------+-----------------------------------------------+");
        System.out.println("| Field            | Type         | Null | Key | Default           | Extra                                         |");
        System.out.println("+------------------+--------------+------+-----+-------------------+-----------------------------------------------+");
        System.out.println("| mark_id          | int          | NO   | PRI | NULL              | auto_increment                                |");
        System.out.println("| enrollment_id    | int          | NO   | UNI | NULL              |                                               |");
        System.out.println("| cia1_marks       | decimal(5,2) | YES  |     | NULL              |                                               |");
        System.out.println("| cia2_marks       | decimal(5,2) | YES  |     | NULL              |                                               |");
        System.out.println("| assignment_marks | decimal(5,2) | YES  |     | NULL              |                                               |");
        System.out.println("| ese_marks        | decimal(5,2) | YES  |     | NULL              |                                               |");
        System.out.println("| is_ese_absent    | tinyint(1)   | NO   |     | 0                 |                                               |");
        System.out.println("| updated_at       | timestamp    | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |");
        System.out.println("+------------------+--------------+------+-----+-------------------+-----------------------------------------------+");
        System.out.println("8 rows in set (0.02 sec)");
    }
}
