package org.joget.gemini;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import org.joget.commons.util.LogUtil;

public class DatabaseService {

    // Database connection parameters
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3307";
    private static final String DB_NAME = "jwdb";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";

    // ... (keeping existing connection methods)

    /**
     * Get database connection using Joget's built-in database access
     */
    public static Connection getConnection() throws SQLException {
        try {
            LogUtil.info("DatabaseService", "Attempting to get database connection via Joget's infrastructure...");

            Class<?> appUtilClass = Class.forName("org.joget.apps.app.service.AppUtil");
            Object appContext = appUtilClass.getMethod("getApplicationContext").invoke(null);
            Object dataSource = appContext.getClass().getMethod("getBean", String.class, Class.class)
                    .invoke(appContext, "setupDataSource", javax.sql.DataSource.class);

            Connection conn = ((javax.sql.DataSource) dataSource).getConnection();

            if (conn != null && !conn.isClosed()) {
                LogUtil.info("DatabaseService", "✅ Successfully obtained database connection via Joget's DataSource");
                return conn;
            } else {
                LogUtil.error("DatabaseService", null, "Joget DataSource connection is null or closed");
                throw new SQLException("Joget database connection is null or closed");
            }

        } catch (Exception e) {
            LogUtil.warn("DatabaseService", "Failed to get connection via Joget infrastructure: " + e.getMessage());
            LogUtil.info("DatabaseService", "Falling back to direct connection attempt...");
            return getDirectConnection();
        }
    }

    /**
     * Fallback method to try direct database connection
     */
    private static Connection getDirectConnection() throws SQLException {
        LogUtil.info("DatabaseService", "Attempting direct database connection (fallback)...");

        String[] drivers = {
                "com.mysql.cj.jdbc.Driver",
                "com.mysql.jdbc.Driver",
                "org.mariadb.jdbc.Driver"
        };

        Properties dbProps = loadJogetDatabaseProperties();
        String url = DB_URL;
        String user = DB_USER;
        String password = DB_PASSWORD;

        if (dbProps != null && !dbProps.isEmpty()) {
            url = dbProps.getProperty("workflowUrl", DB_URL).replace("\\:", ":");
            user = dbProps.getProperty("workflowUser", DB_USER);
            password = dbProps.getProperty("workflowPassword", DB_PASSWORD);
        }

        SQLException lastException = null;

        for (String driver : drivers) {
            try {
                LogUtil.info("DatabaseService", "Trying driver: " + driver);
                Class.forName(driver);
                Connection conn = DriverManager.getConnection(url, user, password);
                LogUtil.info("DatabaseService", "✅ Direct connection successful with driver: " + driver);
                return conn;

            } catch (ClassNotFoundException e) {
                LogUtil.warn("DatabaseService", "Driver not found: " + driver);
            } catch (SQLException e) {
                LogUtil.warn("DatabaseService", "Connection failed with driver " + driver + ": " + e.getMessage());
                lastException = e;
            }
        }

        throw new SQLException("All database connection attempts failed. Last error: " +
                (lastException != null ? lastException.getMessage() : "Unknown"), lastException);
    }

    /**
     * Load database configuration from Joget's properties file
     */
    private static Properties loadJogetDatabaseProperties() {
        Properties props = new Properties();

        String[] possiblePaths = {
                "wflow/app_datasource-default.properties",
                "../wflow/app_datasource-default.properties",
                "../../wflow/app_datasource-default.properties",
                "./app_datasource-default.properties",
                System.getProperty("user.dir") + "/wflow/app_datasource-default.properties",
                System.getProperty("joget.home", "") + "/wflow/app_datasource-default.properties"
        };

        for (String path : possiblePaths) {
            try {
                Path filePath = Paths.get(path);
                if (Files.exists(filePath)) {
                    try (InputStream input = Files.newInputStream(filePath)) {
                        props.load(input);
                        LogUtil.info("DatabaseService", "✅ Loaded Joget database config from: " + path);
                        return props;
                    }
                }
            } catch (Exception e) {
                LogUtil.warn("DatabaseService", "Could not load properties from " + path + ": " + e.getMessage());
            }
        }

        LogUtil.warn("DatabaseService", "⚠️ Could not find Joget database properties file, using hardcoded values");
        return props;
    }

    /**
     * Test database connection
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean isValid = conn != null && !conn.isClosed() && conn.isValid(5);
            if (isValid) {
                LogUtil.info("DatabaseService", "✅ Database connection test successful");
            } else {
                LogUtil.warn("DatabaseService", "⚠️ Database connection test failed - connection not valid");
            }
            return isValid;
        } catch (SQLException e) {
            LogUtil.error("DatabaseService", e, "❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Execute a SELECT query and return results as List of Maps
     */
    public static List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
        }

        return results;
    }

    /**
     * Execute INSERT, UPDATE, DELETE queries
     */
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeUpdate();
        }
    }

    // ========================================
    // COURSE MATERIALS METHODS
    // ========================================

    /**
     * Get all course materials (updated for real column names)
     */
    public static List<Map<String, Object>> getAllMaterials() throws SQLException {
        String sql = "SELECT id, dateCreated, dateModified, createdBy, createdByName, " +
                "c_select_course, c_course_fileupload, c_Uploaded_data, c_course_information " +
                "FROM app_fd_materials ORDER BY dateCreated DESC";
        LogUtil.info("DatabaseService", "Fetching all course materials...");
        return executeQuery(sql);
    }

    /**
     * Search course materials by keyword (updated for real columns)
     */
    public static List<Map<String, Object>> searchMaterials(String keyword) throws SQLException {
        String sql = "SELECT id, dateCreated, dateModified, createdBy, createdByName, " +
                "c_select_course, c_course_fileupload, c_Uploaded_data, c_course_information " +
                "FROM app_fd_materials WHERE " +
                "c_select_course LIKE ? OR c_course_information LIKE ? OR c_course_fileupload LIKE ? " +
                "ORDER BY dateCreated DESC LIMIT 20";
        String searchPattern = "%" + keyword + "%";

        LogUtil.info("DatabaseService", "Searching materials with keyword: " + keyword);
        return executeQuery(sql, searchPattern, searchPattern, searchPattern);
    }

    /**
     * Get materials by course (updated for real columns)
     */
    public static List<Map<String, Object>> getMaterialsByCourse(String course) throws SQLException {
        String sql = "SELECT id, dateCreated, dateModified, createdBy, createdByName, " +
                "c_select_course, c_course_fileupload, c_Uploaded_data, c_course_information " +
                "FROM app_fd_materials WHERE c_select_course = ? ORDER BY dateCreated DESC";
        LogUtil.info("DatabaseService", "Fetching materials for course: " + course);
        return executeQuery(sql, course);
    }

    /**
     * Get materials summary for AI context (updated for real columns)
     */
    public static String getMaterialsSummary(String searchTerm) throws SQLException {
        List<Map<String, Object>> materials;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            materials = searchMaterials(searchTerm);
        } else {
            materials = getAllMaterials();
        }

        if (materials.isEmpty()) {
            return "No course materials found" + (searchTerm != null ? " for: " + searchTerm : "") + ".";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Available Course Materials");
        if (searchTerm != null) {
            summary.append(" (matching '").append(searchTerm).append("')");
        }
        summary.append(":\n\n");

        for (int i = 0; i < Math.min(materials.size(), 10); i++) {
            Map<String, Object> material = materials.get(i);

            summary.append(i + 1).append(". ");

            // Course name
            if (material.get("c_select_course") != null) {
                summary.append("Course: ").append(material.get("c_select_course")).append("\n");
            }

            // File/material name
            if (material.get("c_course_fileupload") != null) {
                summary.append("   Material: ").append(material.get("c_course_fileupload")).append("\n");
            }

            // Description/information
            if (material.get("c_course_information") != null) {
                summary.append("   Description: ").append(material.get("c_course_information")).append("\n");
            }

            // Upload date
            if (material.get("c_Uploaded_data") != null) {
                summary.append("   Uploaded: ").append(material.get("c_Uploaded_data")).append("\n");
            }

            // Created by
            if (material.get("createdByName") != null) {
                summary.append("   Created by: ").append(material.get("createdByName")).append("\n");
            }

            summary.append("\n");
        }

        if (materials.size() > 10) {
            summary.append("... and ").append(materials.size() - 10).append(" more materials.\n");
        }

        return summary.toString();
    }

    // ========================================
    // ASSIGNMENTS METHODS (updated for real columns)
    // ========================================

    /**
     * Get all assignments (updated for real column names)
     */
    public static List<Map<String, Object>> getAllAssignments() throws SQLException {
        String sql = "SELECT id, dateCreated, dateModified, createdBy, createdByName, " +
                "c_assignment_title, c_due_date, c_course, c_assignment_remarks_teacher, " +
                "c_assignment_grade, c_assignment_completion, c_student_name, c_field3 " +
                "FROM app_fd_assignments ORDER BY c_due_date ASC, dateCreated DESC";
        LogUtil.info("DatabaseService", "Fetching all assignments...");
        return executeQuery(sql);
    }

    /**
     * Search assignments by keyword (updated for real columns)
     */
    public static List<Map<String, Object>> searchAssignments(String keyword) throws SQLException {
        String sql = "SELECT id, dateCreated, dateModified, createdBy, createdByName, " +
                "c_assignment_title, c_due_date, c_course, c_assignment_remarks_teacher, " +
                "c_assignment_grade, c_assignment_completion, c_student_name, c_field3 " +
                "FROM app_fd_assignments WHERE " +
                "c_assignment_title LIKE ? OR c_course LIKE ? OR c_assignment_remarks_teacher LIKE ? OR c_field3 LIKE ? "
                +
                "ORDER BY c_due_date ASC LIMIT 20";
        String searchPattern = "%" + keyword + "%";

        LogUtil.info("DatabaseService", "Searching assignments with keyword: " + keyword);
        return executeQuery(sql, searchPattern, searchPattern, searchPattern, searchPattern);
    }

    /**
     * Get assignments by completion status (updated for real columns)
     */
    public static List<Map<String, Object>> getAssignmentsByStatus(String status) throws SQLException {
        String sql = "SELECT id, dateCreated, dateModified, createdBy, createdByName, " +
                "c_assignment_title, c_due_date, c_course, c_assignment_remarks_teacher, " +
                "c_assignment_grade, c_assignment_completion, c_student_name, c_field3 " +
                "FROM app_fd_assignments WHERE c_assignment_completion = ? ORDER BY c_due_date ASC";
        LogUtil.info("DatabaseService", "Fetching assignments with status: " + status);
        return executeQuery(sql, status);
    }

    /**
     * Get assignments by course (updated for real columns)
     */
    public static List<Map<String, Object>> getAssignmentsByCourse(String course) throws SQLException {
        String sql = "SELECT id, dateCreated, dateModified, createdBy, createdByName, " +
                "c_assignment_title, c_due_date, c_course, c_assignment_remarks_teacher, " +
                "c_assignment_grade, c_assignment_completion, c_student_name, c_field3 " +
                "FROM app_fd_assignments WHERE c_course = ? ORDER BY c_due_date ASC";
        LogUtil.info("DatabaseService", "Fetching assignments for course: " + course);
        return executeQuery(sql, course);
    }

    /**
     * Get upcoming assignments - need to handle the date format (updated for real
     * columns)
     */
    public static List<Map<String, Object>> getUpcomingAssignments() throws SQLException {
        // Note: The due date format appears to be 'dd-mm-yyyy' based on your data
        // This query attempts to handle various date formats
        String sql = "SELECT id, dateCreated, dateModified, createdBy, createdByName, " +
                "c_assignment_title, c_due_date, c_course, c_assignment_remarks_teacher, " +
                "c_assignment_grade, c_assignment_completion, c_student_name, c_field3 " +
                "FROM app_fd_assignments WHERE " +
                "c_due_date IS NOT NULL AND c_due_date != '' " +
                "ORDER BY c_due_date ASC LIMIT 10";
        LogUtil.info("DatabaseService", "Fetching upcoming assignments...");
        return executeQuery(sql);
    }

    /**
     * Get assignments summary for AI context (updated for real columns)
     */
    public static String getAssignmentsSummary(String searchTerm) throws SQLException {
        List<Map<String, Object>> assignments;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            assignments = searchAssignments(searchTerm);
        } else {
            assignments = getAllAssignments();
        }

        if (assignments.isEmpty()) {
            return "No assignments found" + (searchTerm != null ? " for: " + searchTerm : "") + ".";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Available Assignments");
        if (searchTerm != null) {
            summary.append(" (matching '").append(searchTerm).append("')");
        }
        summary.append(":\n\n");

        for (int i = 0; i < Math.min(assignments.size(), 10); i++) {
            Map<String, Object> assignment = assignments.get(i);

            summary.append(i + 1).append(". ");

            // Assignment title
            if (assignment.get("c_assignment_title") != null) {
                summary.append("Title: ").append(assignment.get("c_assignment_title")).append("\n");
            }

            // Course
            if (assignment.get("c_course") != null) {
                summary.append("   Course: ").append(assignment.get("c_course")).append("\n");
            }

            // Due date
            if (assignment.get("c_due_date") != null) {
                summary.append("   Due Date: ").append(assignment.get("c_due_date")).append("\n");
            }

            // Completion status
            if (assignment.get("c_assignment_completion") != null) {
                summary.append("   Status: ").append(assignment.get("c_assignment_completion")).append("\n");
            }

            // Grade
            if (assignment.get("c_assignment_grade") != null) {
                summary.append("   Grade: ").append(assignment.get("c_assignment_grade")).append("\n");
            }

            // Additional info (field3)
            if (assignment.get("c_field3") != null && !assignment.get("c_field3").toString().trim().isEmpty()) {
                summary.append("   Info: ").append(assignment.get("c_field3")).append("\n");
            }

            // Teacher remarks
            if (assignment.get("c_assignment_remarks_teacher") != null) {
                summary.append("   Teacher Remarks: ").append(assignment.get("c_assignment_remarks_teacher"))
                        .append("\n");
            }

            // Created by
            if (assignment.get("createdByName") != null) {
                summary.append("   Created by: ").append(assignment.get("createdByName")).append("\n");
            }

            summary.append("\n");
        }

        if (assignments.size() > 10) {
            summary.append("... and ").append(assignments.size() - 10).append(" more assignments.\n");
        }

        return summary.toString();
    }

    /**
     * Get unique courses from both tables
     */
    public static List<String> getAllCourses() throws SQLException {
        Set<String> courses = new HashSet<>();

        // Get courses from materials
        String materialsSql = "SELECT DISTINCT c_select_course FROM app_fd_materials WHERE c_select_course IS NOT NULL AND c_select_course != ''";
        List<Map<String, Object>> materialsCourses = executeQuery(materialsSql);
        for (Map<String, Object> row : materialsCourses) {
            courses.add(row.get("c_select_course").toString());
        }

        // Get courses from assignments
        String assignmentsSql = "SELECT DISTINCT c_course FROM app_fd_assignments WHERE c_course IS NOT NULL AND c_course != ''";
        List<Map<String, Object>> assignmentsCourses = executeQuery(assignmentsSql);
        for (Map<String, Object> row : assignmentsCourses) {
            courses.add(row.get("c_course").toString());
        }

        return new ArrayList<>(courses);
    }

    /**
     * Get course statistics
     */
    public static Map<String, Object> getCourseStatistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        // Count materials
        String materialsCountSql = "SELECT COUNT(*) as count FROM app_fd_materials";
        List<Map<String, Object>> materialsCount = executeQuery(materialsCountSql);
        stats.put("totalMaterials", materialsCount.get(0).get("count"));

        // Count assignments
        String assignmentsCountSql = "SELECT COUNT(*) as count FROM app_fd_assignments";
        List<Map<String, Object>> assignmentsCount = executeQuery(assignmentsCountSql);
        stats.put("totalAssignments", assignmentsCount.get(0).get("count"));

        // Count unique courses
        List<String> courses = getAllCourses();
        stats.put("totalCourses", courses.size());
        stats.put("coursesList", courses);

        // Count completed assignments
        String completedSql = "SELECT COUNT(*) as count FROM app_fd_assignments WHERE c_assignment_completion = 'yes'";
        List<Map<String, Object>> completedCount = executeQuery(completedSql);
        stats.put("completedAssignments", completedCount.get(0).get("count"));

        // Count graded assignments
        String gradedSql = "SELECT COUNT(*) as count FROM app_fd_assignments WHERE c_assignment_grade IS NOT NULL AND c_assignment_grade != ''";
        List<Map<String, Object>> gradedCount = executeQuery(gradedSql);
        stats.put("gradedAssignments", gradedCount.get(0).get("count"));

        return stats;
    }

    // ========================================
    // EXISTING METHODS (keeping them)
    // ========================================

    /**
     * Get all Joget apps
     */
    public static List<Map<String, Object>> getAllApps() throws SQLException {
        String sql = "SELECT appId, appVersion, name, description, dateCreated, dateModified FROM app_app ORDER BY dateModified DESC";
        return executeQuery(sql);
    }

    /**
     * Get all forms for a specific app
     */
    public static List<Map<String, Object>> getAppForms(String appId, String appVersion) throws SQLException {
        String sql = "SELECT id, name, description, dateCreated, dateModified FROM app_form WHERE appId = ? AND appVersion = ? ORDER BY name";
        return executeQuery(sql, appId, appVersion);
    }

    /**
     * Search users
     */
    public static List<Map<String, Object>> searchUsers(String searchTerm) throws SQLException {
        String sql = "SELECT username, firstName, lastName, email, active FROM dir_user WHERE username LIKE ? OR firstName LIKE ? OR lastName LIKE ? OR email LIKE ? LIMIT 20";
        String searchPattern = "%" + searchTerm + "%";
        return executeQuery(sql, searchPattern, searchPattern, searchPattern, searchPattern);
    }

    /**
     * Save chat conversation to database
     */
    public static void saveChatConversation(String sessionId, String userPrompt, String aiResponse, String model)
            throws SQLException {
        createChatTableIfNotExists();
        String sql = "INSERT INTO gemini_chat_history (sessionId, userPrompt, aiResponse, model, timestamp) VALUES (?, ?, ?, ?, NOW())";
        executeUpdate(sql, sessionId, userPrompt, aiResponse, model);
    }

    /**
     * Get chat history for a session
     */
    public static List<Map<String, Object>> getChatHistory(String sessionId, int limit) throws SQLException {
        String sql = "SELECT * FROM gemini_chat_history WHERE sessionId = ? ORDER BY timestamp DESC LIMIT ?";
        return executeQuery(sql, sessionId, limit);
    }

    /**
     * Create chat history table if it doesn't exist
     */
    private static void createChatTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS gemini_chat_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "sessionId VARCHAR(255), " +
                "userPrompt TEXT, " +
                "aiResponse TEXT, " +
                "model VARCHAR(100), " +
                "timestamp DATETIME, " +
                "INDEX idx_session (sessionId), " +
                "INDEX idx_timestamp (timestamp)" +
                ")";
        executeUpdate(sql);
    }

    /**
     * Get database info
     */
    public static Map<String, Object> getDatabaseInfo() throws SQLException {
        Map<String, Object> info = new HashMap<>();

        String connectionMethod = "unknown";
        try {
            Class<?> appUtilClass = Class.forName("org.joget.apps.app.service.AppUtil");
            Object appContext = appUtilClass.getMethod("getApplicationContext").invoke(null);
            Object dataSource = appContext.getClass().getMethod("getBean", String.class, Class.class)
                    .invoke(appContext, "setupDataSource", javax.sql.DataSource.class);

            Connection testConn = ((javax.sql.DataSource) dataSource).getConnection();
            if (testConn != null && !testConn.isClosed()) {
                connectionMethod = "joget_datasource";
                testConn.close();
            }
        } catch (Exception e) {
            connectionMethod = "direct_connection_fallback";
        }

        info.put("connectionMethod", connectionMethod);

        Properties dbProps = loadJogetDatabaseProperties();
        boolean usingJogetConfig = dbProps != null && !dbProps.isEmpty() && dbProps.containsKey("workflowUrl");

        info.put("configurationSource", usingJogetConfig ? "joget_properties" : "hardcoded");

        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("connectionValid", conn.isValid(5));

            try (ResultSet tables = metaData.getTables(null, null, "%", new String[] { "TABLE" })) {
                int tableCount = 0;
                while (tables.next()) {
                    tableCount++;
                }
                info.put("tableCount", tableCount);
            }

            String[] jogetTables = { "app_app", "dir_user", "app_form", "wf_process", "app_fd_materials",
                    "app_fd_assignments" };
            Map<String, Boolean> tableExists = new HashMap<>();
            for (String tableName : jogetTables) {
                try (ResultSet rs = metaData.getTables(null, null, tableName, new String[] { "TABLE" })) {
                    tableExists.put(tableName, rs.next());
                }
            }
            info.put("jogetTablesExist", tableExists);

        }

        return info;
    }
}