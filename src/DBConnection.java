import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * DBConnection.java
 * Kongu Engineering College (Autonomous) - Regulation 2024
 * Manages JDBC connection lifecycle supporting both SQLite (zero-config default)
 * and MySQL database engines with automatic schema initialization.
 */
public class DBConnection {
    private static Connection connection = null;
    private static String dbType = "sqlite";
    private static String sqlitePath = "data/kec_academic.db";
    private static String mysqlUrl = "jdbc:mysql://localhost:3306/kec_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String mysqlUser = "root";
    private static String mysqlPassword = "";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                Properties props = new Properties();
                props.load(fis);
                dbType = props.getProperty("db.type", "sqlite").trim().toLowerCase();
                sqlitePath = props.getProperty("db.sqlite.path", "data/kec_academic.db").trim();
                mysqlUrl = props.getProperty("db.mysql.url", mysqlUrl).trim();
                mysqlUser = props.getProperty("db.mysql.user", mysqlUser).trim();
                mysqlPassword = props.getProperty("db.mysql.password", mysqlPassword).trim();
            } catch (Exception e) {
                System.err.println("[WARN] Could not load db.properties, using SQLite defaults: " + e.getMessage());
            }
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }

        try {
            if ("mysql".equalsIgnoreCase(dbType)) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword);
            } else {
                Class.forName("org.sqlite.JDBC");
                File dbFile = new File(sqlitePath);
                boolean isNew = !dbFile.exists() || dbFile.length() == 0;
                if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                String jdbcUrl = "jdbc:sqlite:" + sqlitePath;
                connection = DriverManager.getConnection(jdbcUrl);
                
                // Enable foreign key support in SQLite
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }

                if (isNew) {
                    initializeDatabase(connection);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver not found. Please ensure JDBC JAR is in classpath: " + e.getMessage(), e);
        }

        return connection;
    }

    public static void initializeDatabase(Connection conn) {
        System.out.println("[INFO] Initializing KEC R2024 database tables and initial seed data...");
        File schemaFile = new File("kec_schema.sql");
        if (!schemaFile.exists()) {
            schemaFile = new File("sql/kec_schema.sql");
        }

        if (schemaFile.exists()) {
            executeSqlScript(conn, schemaFile);
        } else {
            createDefaultSchema(conn);
        }
    }

    private static void executeSqlScript(Connection conn, File scriptFile) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            Statement stmt = conn.createStatement();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // Ignore comment lines
                if (line.startsWith("--") || line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                // Strip inline comments if any
                int commentIndex = line.indexOf("--");
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex).trim();
                }

                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = sb.toString().trim();
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }
                    if ("sqlite".equalsIgnoreCase(dbType)) {
                        sql = sql.replace("AUTO_INCREMENT", "AUTOINCREMENT");
                    }
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                        } catch (SQLException ex) {
                            if (!sql.toUpperCase().startsWith("DROP")) {
                                System.err.println("[SCHEMA NOTICE] " + ex.getMessage() + " for: " + sql);
                            }
                        }
                    }
                    sb.setLength(0);
                }
            }
            System.out.println("[INFO] Schema initialization completed successfully.");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to execute SQL script: " + e.getMessage());
        }
    }

    private static void createDefaultSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String autoInc = "sqlite".equalsIgnoreCase(dbType) ? "AUTOINCREMENT" : "AUTO_INCREMENT";

            stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                    "roll_no VARCHAR(20) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "department VARCHAR(50) NOT NULL, " +
                    "semester INT NOT NULL);");

            stmt.execute("CREATE TABLE IF NOT EXISTS courses (" +
                    "course_code VARCHAR(20) PRIMARY KEY, " +
                    "title VARCHAR(150) NOT NULL, " +
                    "credits INT NOT NULL, " +
                    "course_type VARCHAR(20) NOT NULL);");

            stmt.execute("CREATE TABLE IF NOT EXISTS enrollments (" +
                    "enrollment_id INTEGER PRIMARY KEY " + autoInc + ", " +
                    "roll_no VARCHAR(20) NOT NULL, " +
                    "course_code VARCHAR(20) NOT NULL, " +
                    "semester INT NOT NULL, " +
                    "total_classes INT DEFAULT 0, " +
                    "attended_classes INT DEFAULT 0, " +
                    "is_condoned BOOLEAN DEFAULT 0, " +
                    "FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE, " +
                    "FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE, " +
                    "UNIQUE(roll_no, course_code, semester));");

            stmt.execute("CREATE TABLE IF NOT EXISTS assessment_marks (" +
                    "mark_id INTEGER PRIMARY KEY " + autoInc + ", " +
                    "enrollment_id INT NOT NULL UNIQUE, " +
                    "cia1 DOUBLE DEFAULT NULL, " +
                    "cia2 DOUBLE DEFAULT NULL, " +
                    "assignment_lab DOUBLE DEFAULT NULL, " +
                    "ese_raw DOUBLE DEFAULT NULL, " +
                    "is_ese_absent BOOLEAN DEFAULT 0, " +
                    "FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE);");

            System.out.println("[INFO] Embedded tables verified and ready.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Error creating schema: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                System.err.println("[WARN] Error closing connection: " + e.getMessage());
            }
        }
    }

    public static String getDbType() {
        return dbType;
    }
}
