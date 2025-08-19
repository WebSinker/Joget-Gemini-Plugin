package org.joget.gemini;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.commons.util.LogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class GeminiPlugin extends DefaultApplicationPlugin {

    // Embedded HTTP server
    private static HttpServer embeddedServer = null;
    private static final int EMBEDDED_PORT = 8081;

    @Override
    public String getName() {
        return "GeminiPlugin";
    }

    /**
     * Database Test Handler
     */

    @Override
    public String getVersion() {
        return "2.3.1"; // Fixed multipart parsing using proven approach
    }

    @Override
    public String getDescription() {
        return "Gemini AI plugin with embedded HTTP server, database integration, and fixed multipart support";
    }

    @Override
    public String getLabel() {
        return "Gemini AI Plugin (Database + Fixed Multipart)";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "[]";
    }

    /**
     * Start embedded HTTP server
     */
    public static void startEmbeddedServer() {
        if (embeddedServer != null) {
            LogUtil.info("GeminiPlugin", "Embedded server already running");
            return;
        }

        try {
            LogUtil.info("GeminiPlugin", "Starting enhanced HTTP server on port " + EMBEDDED_PORT);

            // Create HTTP server
            embeddedServer = HttpServer.create(new InetSocketAddress(EMBEDDED_PORT), 0);

            // ========================================
            // CORE AI CHAT ENDPOINTS
            // ========================================
            embeddedServer.createContext("/chat", new ChatHandler()); // Enhanced with database integration
            embeddedServer.createContext("/health", new HealthHandler());
            embeddedServer.createContext("/test", new TestHandler());

            // ========================================
            // ORIGINAL DATABASE ENDPOINTS
            // ========================================
            embeddedServer.createContext("/db/test", new DatabaseTestHandler());
            embeddedServer.createContext("/db/apps", new DatabaseAppsHandler());
            embeddedServer.createContext("/db/forms", new DatabaseFormsHandler());
            embeddedServer.createContext("/db/users", new DatabaseUsersHandler());
            embeddedServer.createContext("/db/chat-history", new ChatHistoryHandler());
            embeddedServer.createContext("/db/info", new DatabaseInfoHandler());

            // ========================================
            // NEW ENHANCED ENDPOINTS FOR REAL DATA
            // ========================================
            embeddedServer.createContext("/db/materials", new DatabaseMaterialsHandler()); // Course materials from
                                                                                           // app_fd_materials
            embeddedServer.createContext("/db/assignments", new DatabaseAssignmentsHandler()); // Assignments from
                                                                                               // app_fd_assignments
            embeddedServer.createContext("/db/statistics", new CourseStatisticsHandler()); // Combined statistics
            embeddedServer.createContext("/analyze", new ContentAnalysisHandler()); // AI content analysis

            // ========================================
            // DEBUG & DOCUMENTATION
            // ========================================
            embeddedServer.createContext("/debug", new DebugHandler());
            embeddedServer.createContext("/", new ApiDocsHandler()); // Enhanced with real examples

            // Start server in background
            embeddedServer.setExecutor(null);
            embeddedServer.start();

            // ========================================
            // Auto Grading Service
            // ========================================
            embeddedServer.createContext("/grade", new AutoGradingHandler()); // AI auto-grading endpoint
            embeddedServer.createContext("/grade/batch", new BatchGradingHandler()); // Batch grading endpoint

            // MATERIAL EVALUATION ENDPOINTS
            embeddedServer.createContext("/evaluate", new MaterialEvaluationHandler()); // Single material evaluation
            embeddedServer.createContext("/evaluate/batch", new BatchMaterialEvaluationHandler()); // Batch evaluation

            // ========================================
            // SUCCESS LOGGING WITH ENHANCED INFO
            // ========================================
            LogUtil.info("GeminiPlugin", "✅ Enhanced HTTP server started successfully!");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "🤖 SMART AI CHAT:");
            LogUtil.info("GeminiPlugin", "   • Enhanced Chat API: http://localhost:" + EMBEDDED_PORT + "/chat");
            LogUtil.info("GeminiPlugin", "   • Auto-detects course & assignment questions");
            LogUtil.info("GeminiPlugin", "   • Uses real database data for responses");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "📊 REAL DATABASE APIs:");
            LogUtil.info("GeminiPlugin", "   • Materials API: http://localhost:" + EMBEDDED_PORT + "/db/materials");
            LogUtil.info("GeminiPlugin", "   • Assignments API: http://localhost:" + EMBEDDED_PORT + "/db/assignments");
            LogUtil.info("GeminiPlugin", "   • Statistics API: http://localhost:" + EMBEDDED_PORT + "/db/statistics");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "🧠 ANALYSIS TOOLS:");
            LogUtil.info("GeminiPlugin", "   • Content Analysis: http://localhost:" + EMBEDDED_PORT + "/analyze");
            LogUtil.info("GeminiPlugin", "   • Debug Requests: http://localhost:" + EMBEDDED_PORT + "/debug");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "📖 DOCUMENTATION:");
            LogUtil.info("GeminiPlugin", "   • Interactive API Docs: http://localhost:" + EMBEDDED_PORT + "/");
            LogUtil.info("GeminiPlugin", "   • Real examples with your database structure");
            LogUtil.info("GeminiPlugin", "🎓 AUTO-GRADING APIs:");
            LogUtil.info("GeminiPlugin", "   • Grade Assignment: http://localhost:" + EMBEDDED_PORT + "/grade");
            LogUtil.info("GeminiPlugin", "   • Batch Grading: http://localhost:" + EMBEDDED_PORT + "/grade/batch");
            LogUtil.info("GeminiPlugin", "📊 MATERIAL EVALUATION APIs (Enhanced):");
            LogUtil.info("GeminiPlugin", "   • Evaluate Material: http://localhost:" + EMBEDDED_PORT + "/evaluate");
            LogUtil.info("GeminiPlugin", "   • Supports both JSON and form data (encoding fix)");
            LogUtil.info("GeminiPlugin", "   • Pre-upload analysis with browser file content");
            LogUtil.info("GeminiPlugin",
                    "   • Batch Evaluation: http://localhost:" + EMBEDDED_PORT + "/evaluate/batch");
            LogUtil.info("GeminiPlugin", "");
            // ========================================
            // DATABASE CONNECTION & TESTING
            // ========================================
            if (DatabaseService.testConnection()) {
                LogUtil.info("GeminiPlugin", "");
                LogUtil.info("GeminiPlugin", "✅ Database connection successful!");

                // Test content analyzer
                LogUtil.info("GeminiPlugin", "🧪 Testing content analyzer...");
                ContentAnalyzer.testAnalyzer();

                // Get some real data statistics
                try {
                    Map<String, Object> stats = DatabaseService.getCourseStatistics();
                    LogUtil.info("GeminiPlugin", "");
                    LogUtil.info("GeminiPlugin", "📊 Your Database Content:");
                    LogUtil.info("GeminiPlugin", "   • Materials: " + stats.get("totalMaterials"));
                    LogUtil.info("GeminiPlugin", "   • Assignments: " + stats.get("totalAssignments"));
                    LogUtil.info("GeminiPlugin", "   • Courses: " + stats.get("totalCourses"));
                    LogUtil.info("GeminiPlugin", "   • Completed Assignments: " + stats.get("completedAssignments"));
                    LogUtil.info("GeminiPlugin", "   • Graded Assignments: " + stats.get("gradedAssignments"));

                    @SuppressWarnings("unchecked")
                    List<String> courses = (List<String>) stats.get("coursesList");
                    LogUtil.info("GeminiPlugin", "   • Available Courses: " + String.join(", ", courses));

                } catch (Exception e) {
                    LogUtil.warn("GeminiPlugin", "Could not fetch database statistics: " + e.getMessage());
                }

            } else {
                LogUtil.warn("GeminiPlugin", "");
                LogUtil.warn("GeminiPlugin", "⚠️ Database connection failed - enhanced features may not work");
            }

            // ========================================
            // QUICK TEST EXAMPLES
            // ========================================
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "🧪 QUICK TESTS:");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "# Test smart course question:");
            LogUtil.info("GeminiPlugin", "curl -X POST \"http://localhost:" + EMBEDDED_PORT + "/chat\" \\");
            LogUtil.info("GeminiPlugin", "  -d \"userPrompt=what course do we actually have?&sessionId=test123\"");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "# Test assignment question:");
            LogUtil.info("GeminiPlugin", "curl -X POST \"http://localhost:" + EMBEDDED_PORT + "/chat\" \\");
            LogUtil.info("GeminiPlugin", "  -d \"userPrompt=show me all assignments&sessionId=test123\"");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "# Test search materials:");
            LogUtil.info("GeminiPlugin", "curl \"http://localhost:" + EMBEDDED_PORT + "/db/materials?search=network\"");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "# Test content analysis:");
            LogUtil.info("GeminiPlugin", "curl -X POST \"http://localhost:" + EMBEDDED_PORT + "/analyze\" \\");
            LogUtil.info("GeminiPlugin", "  -d \"message=find materials about networking\"");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "🎯 Your chat terminal is now a smart educational assistant!");
            LogUtil.info("GeminiPlugin", "   Students can ask natural questions and get accurate responses");
            LogUtil.info("GeminiPlugin", "   based on your actual course materials and assignments.");
            LogUtil.info("GeminiPlugin", "# Test auto-grading:");
            LogUtil.info("GeminiPlugin", "curl -X POST \"http://localhost:" + EMBEDDED_PORT + "/grade\" \\");
            LogUtil.info("GeminiPlugin", "  -d \"assignmentId=YOUR_ASSIGNMENT_ID&mode=preview\"");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "# Test batch grading:");
            LogUtil.info("GeminiPlugin",
                    "curl \"http://localhost:" + EMBEDDED_PORT + "/grade/batch?course=asd&limit=5\"");
            LogUtil.info("GeminiPlugin", "# Test material evaluation:");
            LogUtil.info("GeminiPlugin", "curl -X POST \"http://localhost:" + EMBEDDED_PORT + "/evaluate\" \\");
            LogUtil.info("GeminiPlugin",
                    "  -d \"course=asd&description=Java programming tutorial&filename=java_basics.pdf\"");
            LogUtil.info("GeminiPlugin", "");
            LogUtil.info("GeminiPlugin", "# Test batch material evaluation:");
            LogUtil.info("GeminiPlugin",
                    "curl \"http://localhost:" + EMBEDDED_PORT + "/evaluate/batch?course=asd&limit=5\"");
            LogUtil.info("GeminiPlugin", "");
        } catch (Exception e) {
            LogUtil.error("GeminiPlugin", e, "Failed to start enhanced HTTP server: " + e.getMessage());
        }
    }

    /**
     * Stop embedded HTTP server
     */
    public static void stopEmbeddedServer() {
        if (embeddedServer != null) {
            LogUtil.info("GeminiPlugin", "Stopping embedded HTTP server...");
            embeddedServer.stop(2); // 2 second grace period
            embeddedServer = null;
            LogUtil.info("GeminiPlugin", "✅ Embedded HTTP server stopped");
        }
    }

    /**
     * Check if embedded server is running
     */
    public static boolean isEmbeddedServerRunning() {
        return embeddedServer != null;
    }

    /**
     * Get embedded server port
     */
    public static int getEmbeddedServerPort() {
        return EMBEDDED_PORT;
    }

    // ===========================================
    // EMBEDDED SERVER HANDLERS
    // ===========================================

    /**
     * Enhanced Chat API Handler with intelligent database integration
     * Automatically detects when users ask about courses/assignments and enriches
     * responses
     */
    static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LogUtil.info("GeminiPlugin", "=== ENHANCED CHAT API CALLED ===");
            LogUtil.info("GeminiPlugin", "Method: " + exchange.getRequestMethod());
            LogUtil.info("GeminiPlugin", "URI: " + exchange.getRequestURI());

            try {
                // Set CORS headers first
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    LogUtil.info("GeminiPlugin", "Handled CORS preflight request");
                    return;
                }

                // Parse parameters
                Map<String, String> params = parseParametersImproved(exchange);
                String userPrompt = params.get("userPrompt");
                String chatHistory = params.get("chatHistory");
                String sessionId = params.get("sessionId");
                String saveToDb = params.get("saveToDb");

                LogUtil.info("GeminiPlugin", "User prompt: " + userPrompt);
                LogUtil.info("GeminiPlugin", "Session ID: " + sessionId);
                LogUtil.info("GeminiPlugin", "Save to DB: " + saveToDb);

                // Validate input
                if (userPrompt == null || userPrompt.trim().isEmpty()) {
                    LogUtil.warn("GeminiPlugin", "No user prompt provided - sending error response");
                    sendErrorResponse(exchange, "No userPrompt parameter provided", "MISSING_PROMPT", 400);
                    return;
                }

                // Get API key
                String apiKey = getConfiguredApiKey();
                if (apiKey == null || "YOUR_API_KEY_HERE".equals(apiKey)) {
                    LogUtil.warn("GeminiPlugin", "Gemini API key not configured");
                    sendErrorResponse(exchange, "Gemini API key not configured", "NO_API_KEY", 400);
                    return;
                }

                // ========================================
                // 🧠 INTELLIGENT CONTENT ANALYSIS
                // ========================================
                LogUtil.info("GeminiPlugin", "Analyzing user message for database integration...");
                ContentAnalyzer.AnalysisResult analysis = ContentAnalyzer.analyzeMessage(userPrompt);

                String databaseContext = "";
                boolean usedDatabase = false;

                // Retrieve database information if needed
                if (analysis.needsDatabaseData()) {
                    LogUtil.info("GeminiPlugin", "Fetching database information for: " + analysis.getContentType());
                    databaseContext = getDatabaseContext(analysis);
                    usedDatabase = true;
                }

                // ========================================
                // 🤖 BUILD ENHANCED PROMPT
                // ========================================
                String enhancedPrompt = buildEnhancedPrompt(userPrompt, chatHistory, databaseContext, analysis);
                LogUtil.info("GeminiPlugin", "Enhanced prompt built with " +
                        (usedDatabase ? "database context" : "no database context"));

                // ========================================
                // 🚀 CALL GEMINI API
                // ========================================
                LogUtil.info("GeminiPlugin", "Calling Gemini API...");
                GeminiService geminiService = new GeminiService(apiKey);

                Map<String, Object> apiParams = new HashMap<>();
                apiParams.put("temperature", 0.7);
                apiParams.put("maxOutputTokens", 1500); // Increased for database-enhanced responses

                String aiResponse = geminiService.generateContent("gemini-1.5-flash", enhancedPrompt, apiParams);
                LogUtil.info("GeminiPlugin", "AI response received: " +
                        aiResponse.substring(0, Math.min(100, aiResponse.length())) + "...");

                // ========================================
                // 💾 SAVE TO DATABASE IF REQUESTED
                // ========================================
                boolean savedToDb = false;
                if ("true".equals(saveToDb) && sessionId != null && !sessionId.trim().isEmpty()) {
                    try {
                        DatabaseService.saveChatConversation(sessionId, userPrompt, aiResponse, "gemini-1.5-flash");
                        savedToDb = true;
                        LogUtil.info("GeminiPlugin", "Chat conversation saved to database");
                    } catch (Exception dbError) {
                        LogUtil.error("GeminiPlugin", dbError,
                                "Failed to save chat to database: " + dbError.getMessage());
                    }
                }

                // ========================================
                // 📤 SEND ENHANCED RESPONSE
                // ========================================
                String jsonResponse = buildSuccessResponse(
                        aiResponse, sessionId, analysis, usedDatabase, savedToDb);

                exchange.sendResponseHeaders(200, jsonResponse.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes("UTF-8"));
                    os.flush();
                }

                LogUtil.info("GeminiPlugin", "✅ Enhanced response sent successfully!");

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in enhanced chat handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage(), "API_ERROR", 500);
            }
        }

        /**
         * Get database context based on content analysis
         */
        private String getDatabaseContext(ContentAnalyzer.AnalysisResult analysis) {
            try {
                StringBuilder context = new StringBuilder();

                switch (analysis.getContentType()) {
                    case MATERIALS:
                        LogUtil.info("GeminiPlugin", "Fetching course materials data...");

                        if (analysis.getQueryType() == ContentAnalyzer.QueryType.SEARCH &&
                                analysis.getSearchTerms() != null) {
                            // Search for specific materials
                            String materialsSummary = DatabaseService.getMaterialsSummary(analysis.getSearchTerms());
                            context.append("DATABASE CONTEXT - Course Materials (Search: ")
                                    .append(analysis.getSearchTerms()).append("):\n")
                                    .append(materialsSummary).append("\n");
                        } else {
                            // Get all materials or general list
                            String materialsSummary = DatabaseService.getMaterialsSummary(null);
                            context.append("DATABASE CONTEXT - All Course Materials:\n")
                                    .append(materialsSummary).append("\n");
                        }
                        break;

                    case ASSIGNMENTS:
                        LogUtil.info("GeminiPlugin", "Fetching assignments data...");

                        if (analysis.getQueryType() == ContentAnalyzer.QueryType.STATUS) {
                            // Get upcoming assignments
                            List<Map<String, Object>> upcoming = DatabaseService.getUpcomingAssignments();
                            context.append("DATABASE CONTEXT - Upcoming Assignments (Next 7 Days):\n");
                            if (upcoming.isEmpty()) {
                                context.append("No assignments due in the next 7 days.\n");
                            } else {
                                for (Map<String, Object> assignment : upcoming) {
                                    context.append("- ").append(assignment.get("title"))
                                            .append(" (Due: ").append(assignment.get("dueDate")).append(")\n");
                                }
                            }
                        } else if (analysis.getQueryType() == ContentAnalyzer.QueryType.SEARCH &&
                                analysis.getSearchTerms() != null) {
                            // Search for specific assignments
                            String assignmentsSummary = DatabaseService
                                    .getAssignmentsSummary(analysis.getSearchTerms());
                            context.append("DATABASE CONTEXT - Assignments (Search: ")
                                    .append(analysis.getSearchTerms()).append("):\n")
                                    .append(assignmentsSummary).append("\n");
                        } else {
                            // Get all assignments
                            String assignmentsSummary = DatabaseService.getAssignmentsSummary(null);
                            context.append("DATABASE CONTEXT - All Assignments:\n")
                                    .append(assignmentsSummary).append("\n");
                        }
                        break;

                    default:
                        return "";
                }

                return context.toString();

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error fetching database context: " + e.getMessage());
                return "DATABASE CONTEXT: Error retrieving data - " + e.getMessage() + "\n";
            }
        }

        /**
         * Build enhanced prompt with database context
         */
        private String buildEnhancedPrompt(String userPrompt, String chatHistory,
                String databaseContext, ContentAnalyzer.AnalysisResult analysis) {
            StringBuilder prompt = new StringBuilder();

            // System context
            prompt.append(
                    "You are an intelligent educational assistant with access to the Joget learning management system database. ");
            prompt.append(
                    "You help students and instructors with questions about courses, assignments, and materials.\n\n");

            // Database context (if available)
            if (!databaseContext.isEmpty()) {
                prompt.append("IMPORTANT: I have retrieved the following current information from the database:\n\n");
                prompt.append(databaseContext);
                prompt.append("\n");
                prompt.append("Please use this actual database information to answer the user's question accurately. ");
                prompt.append("Present the information in a helpful, organized way.\n\n");
            }

            // Chat history
            if (chatHistory != null && !chatHistory.trim().isEmpty() && !"[]".equals(chatHistory.trim())) {
                prompt.append("Previous conversation context:\n").append(chatHistory).append("\n\n");
            }

            // User question
            prompt.append("User's question: ").append(userPrompt).append("\n\n");

            // Instructions based on analysis
            switch (analysis.getContentType()) {
                case MATERIALS:
                    prompt.append("This question is about course materials. ");
                    if (!databaseContext.isEmpty()) {
                        prompt.append(
                                "Use the database information above to provide specific details about available materials.");
                    } else {
                        prompt.append(
                                "Provide general guidance about course materials and suggest checking the learning system.");
                    }
                    break;

                case ASSIGNMENTS:
                    prompt.append("This question is about assignments. ");
                    if (!databaseContext.isEmpty()) {
                        prompt.append(
                                "Use the database information above to provide specific details about assignments, including due dates and status.");
                    } else {
                        prompt.append(
                                "Provide general guidance about assignments and suggest checking the learning system.");
                    }
                    break;

                default:
                    prompt.append("Please provide a helpful response to this educational question.");
                    break;
            }

            return prompt.toString();
        }

        /**
         * Build success response JSON
         */
        private String buildSuccessResponse(String aiResponse, String sessionId,
                ContentAnalyzer.AnalysisResult analysis,
                boolean usedDatabase, boolean savedToDb) {
            return "{" +
                    "\"status\":\"success\"," +
                    "\"response\":\"" + escapeJsonString(aiResponse) + "\"," +
                    "\"sessionId\":\"" + (sessionId != null ? sessionId : "null") + "\"," +
                    "\"timestamp\":" + System.currentTimeMillis() + "," +
                    "\"model\":\"gemini-1.5-flash\"," +
                    "\"server\":\"embedded\"," +
                    "\"port\":" + EMBEDDED_PORT + "," +
                    "\"savedToDatabase\":" + savedToDb + "," +
                    "\"databaseEnhanced\":" + usedDatabase + "," +
                    "\"detectedContentType\":\"" + analysis.getContentType() + "\"," +
                    "\"detectedQueryType\":\"" + analysis.getQueryType() + "\"," +
                    "\"searchTerms\":\""
                    + (analysis.getSearchTerms() != null ? escapeJsonString(analysis.getSearchTerms()) : "") + "\"" +
                    "}";
        }

        /**
         * Send error response
         */
        private void sendErrorResponse(HttpExchange exchange, String message, String errorCode, int statusCode)
                throws IOException {
            String errorJson = "{\"status\":\"error\",\"message\":\"" + escapeJsonString(message) +
                    "\",\"errorCode\":\"" + errorCode + "\",\"timestamp\":" + System.currentTimeMillis() + "}";

            setCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, errorJson.getBytes("UTF-8").length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorJson.getBytes("UTF-8"));
            }
        }
    }

    /**
     * Debug Handler - Shows how requests are being parsed
     */
    static class DebugHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LogUtil.info("GeminiPlugin", "=== DEBUG ENDPOINT CALLED ===");

            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                // Parse parameters and show debugging info
                Map<String, String> params = parseParametersImproved(exchange);

                // Get request info
                String method = exchange.getRequestMethod();
                String uri = exchange.getRequestURI().toString();
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

                // Build debug response
                Gson gson = new Gson();
                Map<String, Object> debugInfo = new HashMap<>();
                debugInfo.put("method", method);
                debugInfo.put("uri", uri);
                debugInfo.put("contentType", contentType);
                debugInfo.put("parsedParameters", params);
                debugInfo.put("parameterCount", params.size());
                debugInfo.put("timestamp", System.currentTimeMillis());

                // Check for common issues
                Map<String, String> issues = new HashMap<>();
                if (params.isEmpty()) {
                    issues.put("empty_params", "No parameters were parsed from the request");
                }
                if (!params.containsKey("userPrompt")) {
                    issues.put("missing_userPrompt",
                            "userPrompt parameter not found (also checked for 'message' and 'text')");
                }
                debugInfo.put("potentialIssues", issues);

                String debugJson = "{" +
                        "\"status\":\"debug_success\"," +
                        "\"message\":\"Debug information collected\"," +
                        "\"data\":" + gson.toJson(debugInfo) + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, debugJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(debugJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in debug handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    static class DatabaseTestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LogUtil.info("GeminiPlugin", "=== DATABASE TEST ENDPOINT CALLED ===");

            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                boolean isConnected = DatabaseService.testConnection();

                String testJson = "{" +
                        "\"status\":\"" + (isConnected ? "success" : "error") + "\"," +
                        "\"message\":\"Database connection " + (isConnected ? "successful" : "failed") + "\"," +
                        "\"timestamp\":" + System.currentTimeMillis() + "," +
                        "\"database\":\"jwdb\"," +
                        "\"host\":\"localhost\"," +
                        "\"port\":3307" +
                        "}";

                exchange.sendResponseHeaders(200, testJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(testJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in database test handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Database Apps Handler
     */
    static class DatabaseAppsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                List<Map<String, Object>> apps = DatabaseService.getAllApps();

                Gson gson = new Gson();
                String appsJson = gson.toJson(apps);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"count\":" + apps.size() + "," +
                        "\"data\":" + appsJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in database apps handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Database Forms Handler
     */
    static class DatabaseFormsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                Map<String, String> params = parseParametersImproved(exchange);
                String appId = params.get("appId");
                String appVersion = params.get("appVersion");

                if (appId == null || appVersion == null) {
                    sendErrorResponse(exchange, "appId and appVersion parameters are required");
                    return;
                }

                List<Map<String, Object>> forms = DatabaseService.getAppForms(appId, appVersion);

                Gson gson = new Gson();
                String formsJson = gson.toJson(forms);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"appId\":\"" + appId + "\"," +
                        "\"appVersion\":\"" + appVersion + "\"," +
                        "\"count\":" + forms.size() + "," +
                        "\"data\":" + formsJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in database forms handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Database Users Handler
     */
    static class DatabaseUsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                Map<String, String> params = parseParametersImproved(exchange);
                String searchTerm = params.get("search");

                if (searchTerm == null || searchTerm.trim().isEmpty()) {
                    sendErrorResponse(exchange, "search parameter is required");
                    return;
                }

                List<Map<String, Object>> users = DatabaseService.searchUsers(searchTerm);

                Gson gson = new Gson();
                String usersJson = gson.toJson(users);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"searchTerm\":\"" + escapeJsonString(searchTerm) + "\"," +
                        "\"count\":" + users.size() + "," +
                        "\"data\":" + usersJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in database users handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Chat History Handler
     */
    static class ChatHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                Map<String, String> params = parseParametersImproved(exchange);
                String sessionId = params.get("sessionId");
                String limitStr = params.get("limit");

                if (sessionId == null || sessionId.trim().isEmpty()) {
                    sendErrorResponse(exchange, "sessionId parameter is required");
                    return;
                }

                int limit = 50; // default
                if (limitStr != null) {
                    try {
                        limit = Integer.parseInt(limitStr);
                    } catch (NumberFormatException e) {
                        // use default
                    }
                }

                List<Map<String, Object>> history = DatabaseService.getChatHistory(sessionId, limit);

                Gson gson = new Gson();
                String historyJson = gson.toJson(history);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"sessionId\":\"" + escapeJsonString(sessionId) + "\"," +
                        "\"count\":" + history.size() + "," +
                        "\"data\":" + historyJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in chat history handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Database Info Handler
     */
    static class DatabaseInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                Map<String, Object> dbInfo = DatabaseService.getDatabaseInfo();

                Gson gson = new Gson();
                String infoJson = gson.toJson(dbInfo);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"data\":" + infoJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in database info handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    // ... (keeping existing handlers: TestHandler, HealthHandler, ApiDocsHandler)

    /**
     * Simple Test Handler
     */
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LogUtil.info("GeminiPlugin", "=== TEST ENDPOINT CALLED ===");

            try {
                String testJson = "{" +
                        "\"status\":\"test_success\"," +
                        "\"message\":\"Simple test endpoint is working!\"," +
                        "\"timestamp\":" + System.currentTimeMillis() + "," +
                        "\"method\":\"" + exchange.getRequestMethod() + "\"," +
                        "\"uri\":\"" + exchange.getRequestURI().toString() + "\"" +
                        "}";

                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, testJson.getBytes("UTF-8").length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(testJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Health Check Handler
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String apiKeyStatus = getConfiguredApiKey() != null ? "configured" : "missing";
                boolean dbConnected = DatabaseService.testConnection();

                String healthJson = "{" +
                        "\"status\":\"healthy\"," +
                        "\"server\":\"embedded\"," +
                        "\"plugin\":\"GeminiPlugin\"," +
                        "\"version\":\"2.3.1\"," +
                        "\"timestamp\":" + System.currentTimeMillis() + "," +
                        "\"port\":" + EMBEDDED_PORT + "," +
                        "\"apiKey\":\"" + apiKeyStatus + "\"," +
                        "\"database\":\"" + (dbConnected ? "connected" : "disconnected") + "\"" +
                        "}";

                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, healthJson.getBytes("UTF-8").length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(healthJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Enhanced API Documentation Handler - Updated with real examples
     */
    static class ApiDocsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <title>Gemini Plugin - Smart Educational Assistant with Auto-Grading</title>\n" +
                    "    <style>\n" +
                    "        body { font-family: Arial, sans-serif; max-width: 1200px; margin: 40px auto; padding: 20px; line-height: 1.6; }\n"
                    +
                    "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; text-align: center; }\n"
                    +
                    "        .endpoint { background: #f8f9fa; padding: 20px; margin: 15px 0; border-radius: 8px; border-left: 4px solid #007bff; }\n"
                    +
                    "        .db-endpoint { border-left-color: #28a745; }\n" +
                    "        .ai-endpoint { border-left-color: #fd7e14; }\n" +
                    "        .grading-endpoint { border-left-color: #dc3545; }\n" +
                    "        .test-button { background: #007bff; color: white; padding: 8px 16px; border: none; border-radius: 5px; cursor: pointer; margin: 5px; font-size: 12px; }\n"
                    +
                    "        .db-button { background: #28a745; }\n" +
                    "        .ai-button { background: #fd7e14; }\n" +
                    "        .grading-button { background: #dc3545; }\n" +
                    "        .test-button:hover { opacity: 0.8; }\n" +
                    "        .results { background: #f1f3f4; padding: 20px; border-radius: 8px; margin-top: 20px; min-height: 100px; font-family: monospace; max-height: 400px; overflow-y: auto; }\n"
                    +
                    "        .success { color: #28a745; } .error { color: #dc3545; } .info { color: #17a2b8; }\n" +
                    "        .section { margin: 30px 0; }\n" +
                    "        .highlight { background: #fff3cd; padding: 10px; border-radius: 5px; margin: 10px 0; }\n" +
                    "        .sample-data { background: #e7f3ff; padding: 15px; border-radius: 5px; font-family: monospace; font-size: 12px; }\n"
                    +
                    "        .warning { background: #f8d7da; border: 1px solid #f5c6cb; padding: 10px; border-radius: 5px; margin: 10px 0; }\n"
                    +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"header\">\n" +
                    "        <h1>🤖 Smart Educational Assistant with AI Auto-Grading</h1>\n" +
                    "        <p>AI-Powered Chat with Real Database Integration + Automatic Assignment Grading | Port "
                    + EMBEDDED_PORT + " | Version 2.4.0</p>\n" +
                    "        <p>🧠 Automatically detects course materials & assignment questions + 🎓 AI-powered grading system</p>\n"
                    +
                    "    </div>\n" +
                    "\n" +
                    "    <div class=\"warning\">\n" +
                    "        <h3>📋 Prerequisites for Auto-Grading:</h3>\n" +
                    "        <p><strong>Required Dependencies:</strong> Add these to your project classpath:</p>\n" +
                    "        <ul>\n" +
                    "            <li>Apache POI (for DOCX): <code>poi-4.1.2.jar</code>, <code>poi-ooxml-4.1.2.jar</code></li>\n"
                    +
                    "            <li>Apache PDFBox (for PDF): <code>pdfbox-2.0.24.jar</code></li>\n" +
                    "        </ul>\n" +
                    "        <p><strong>File Upload Path:</strong> Ensure uploaded files are accessible at <code>wflow/app_formuploads/</code></p>\n"
                    +
                    "    </div>\n" +
                    "\n" +
                    "    <div class=\"highlight\">\n" +
                    "        <h3>🎯 Your Database Structure Detected:</h3>\n" +
                    "        <div class=\"sample-data\">\n" +
                    "📚 Materials Table: app_fd_materials\n" +
                    "├── c_select_course (Course Name)\n" +
                    "├── c_course_fileupload (File Name)\n" +
                    "├── c_course_information (Description)\n" +
                    "└── c_Uploaded_data (Upload Date)\n" +
                    "\n" +
                    "📝 Assignments Table: app_fd_assignments\n" +
                    "├── c_assignment_title (Title)\n" +
                    "├── c_course (Course)\n" +
                    "├── c_due_date (Due Date)\n" +
                    "├── c_assignment_completion (Status)\n" +
                    "├── c_assignment_grade (Grade)\n" +
                    "├── c_assignment_remarks_teacher (Teacher Remarks)\n" +
                    "├── c_field3 (Student Answer)\n" +
                    "└── c_file_upload (Uploaded File)\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <div class=\"section\">\n" +
                    "        <h2>🎓 AI Auto-Grading System</h2>\n" +
                    "        \n" +
                    "        <div class=\"endpoint grading-endpoint\">\n" +
                    "            <h3>📝 Grade Individual Assignment</h3>\n" +
                    "            <p><strong>URL:</strong> <code>POST /grade</code></p>\n" +
                    "            <p><strong>Parameters:</strong> assignmentId (required), mode (\"preview\" or \"save\")</p>\n"
                    +
                    "            <p><strong>Features:</strong> Analyzes text answers + uploaded files (PDF, DOCX, TXT)</p>\n"
                    +
                    "            <button class=\"test-button grading-button\" onclick=\"gradeAssignment('preview')\">🔍 Preview Grade</button>\n"
                    +
                    "            <button class=\"test-button grading-button\" onclick=\"gradeAssignment('save')\">💾 Grade & Save</button>\n"
                    +
                    "            <button class=\"test-button grading-button\" onclick=\"showAssignmentIds()\">📋 Get Assignment IDs</button>\n"
                    +
                    "        </div>\n" +
                    "        \n" +
                    "    <div class=\"section\">\n" +
                    "        <h2>📊 AI Material Evaluation System</h2>\n" +
                    "        \n" +
                    "        <div class=\"endpoint grading-endpoint\">\n" +
                    "            <h3>📋 Evaluate Course Material</h3>\n" +
                    "            <p><strong>URL:</strong> <code>POST /evaluate</code></p>\n" +
                    "            <p><strong>Parameters:</strong> course, description, filename (at least one required)</p>\n"
                    +
                    "            <p><strong>Features:</strong> Analyzes educational quality, provides 0-100% recommendation score</p>\n"
                    +
                    "            <button class=\"test-button grading-button\" onclick=\"evaluateMaterial()\">📊 Evaluate Material</button>\n"
                    +
                    "        </div>\n" +
                    "        \n" +
                    "        <div class=\"endpoint grading-endpoint\">\n" +
                    "            <h3>📚 Batch Evaluate Materials</h3>\n" +
                    "            <p><strong>URL:</strong> <code>GET /evaluate/batch</code></p>\n" +
                    "            <p><strong>Parameters:</strong> course (optional), limit (max 20)</p>\n" +
                    "            <button class=\"test-button grading-button\" onclick=\"batchEvaluateMaterials()\">🚀 Batch Evaluate</button>\n"
                    +
                    "        </div>\n" +
                    "    </div>\n" +
                    "        <div class=\"endpoint grading-endpoint\">\n" +
                    "            <h3>📚 Batch Grade Multiple Assignments</h3>\n" +
                    "            <p><strong>URL:</strong> <code>GET /grade/batch</code></p>\n" +
                    "            <p><strong>Parameters:</strong> course (optional), status (\"ungraded\"), limit (max 50)</p>\n"
                    +
                    "            <button class=\"test-button grading-button\" onclick=\"batchGrade()\">🚀 Grade All Ungraded</button>\n"
                    +
                    "            <button class=\"test-button grading-button\" onclick=\"batchGradeByCourse()\">📖 Grade by Course \"asd\"</button>\n"
                    +
                    "            <button class=\"test-button grading-button\" onclick=\"batchGradeLimit()\">🎯 Grade First 3</button>\n"
                    +
                    "        </div>\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <div class=\"section\">\n" +
                    "        <h2>🚀 Smart AI Chat (Database-Enhanced)</h2>\n" +
                    "        \n" +
                    "        <div class=\"endpoint ai-endpoint\">\n" +
                    "            <h3>🧠 Intelligent Chat API</h3>\n" +
                    "            <p><strong>URL:</strong> <code>POST /chat</code></p>\n" +
                    "            <p><strong>Features:</strong> Auto-detects course/assignment questions & uses real database data</p>\n"
                    +
                    "            <button class=\"test-button ai-button\" onclick=\"testCourseQuestion()\">📚 Ask About Courses</button>\n"
                    +
                    "            <button class=\"test-button ai-button\" onclick=\"testAssignmentQuestion()\">📝 Ask About Assignments</button>\n"
                    +
                    "            <button class=\"test-button ai-button\" onclick=\"testNetworkingQuestion()\">🔍 Search \"Networking\"</button>\n"
                    +
                    "            <button class=\"test-button ai-button\" onclick=\"testGeneralChat()\">💬 General Chat</button>\n"
                    +
                    "        </div>\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <div class=\"section\">\n" +
                    "        <h2>🗄️ Direct Database Access</h2>\n" +
                    "        \n" +
                    "        <div class=\"endpoint db-endpoint\">\n" +
                    "            <h3>📚 Course Materials API</h3>\n" +
                    "            <p><strong>URL:</strong> <code>GET /db/materials</code></p>\n" +
                    "            <p><strong>Parameters:</strong> search, course</p>\n" +
                    "            <button class=\"test-button db-button\" onclick=\"getAllMaterials()\">📚 Get All Materials</button>\n"
                    +
                    "            <button class=\"test-button db-button\" onclick=\"searchMaterials()\">🔍 Search \"Network\"</button>\n"
                    +
                    "            <button class=\"test-button db-button\" onclick=\"getMaterialsByCourse()\">📂 Get by Course \"asd\"</button>\n"
                    +
                    "        </div>\n" +
                    "        \n" +
                    "        <div class=\"endpoint db-endpoint\">\n" +
                    "            <h3>📝 Assignments API</h3>\n" +
                    "            <p><strong>URL:</strong> <code>GET /db/assignments</code></p>\n" +
                    "            <p><strong>Parameters:</strong> search, status, course, upcoming</p>\n" +
                    "            <button class=\"test-button db-button\" onclick=\"getAllAssignments()\">📝 Get All Assignments</button>\n"
                    +
                    "            <button class=\"test-button db-button\" onclick=\"searchAssignments()\">🔍 Search \"Networking\"</button>\n"
                    +
                    "            <button class=\"test-button db-button\" onclick=\"getCompletedAssignments()\">✅ Get Completed</button>\n"
                    +
                    "            <button class=\"test-button db-button\" onclick=\"getUpcomingAssignments()\">⏰ Get Upcoming</button>\n"
                    +
                    "        </div>\n" +
                    "        \n" +
                    "        <div class=\"endpoint db-endpoint\">\n" +
                    "            <h3>📊 Course Statistics</h3>\n" +
                    "            <p><strong>URL:</strong> <code>GET /db/statistics</code></p>\n" +
                    "            <button class=\"test-button db-button\" onclick=\"getCourseStats()\">📊 Get Statistics</button>\n"
                    +
                    "        </div>\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <div class=\"section\">\n" +
                    "        <h2>🧪 Analysis & Testing</h2>\n" +
                    "        \n" +
                    "        <div class=\"endpoint\">\n" +
                    "            <h3>🧠 Content Analysis</h3>\n" +
                    "            <p><strong>URL:</strong> <code>POST /analyze</code></p>\n" +
                    "            <p>See how the AI interprets different questions</p>\n" +
                    "            <button class=\"test-button\" onclick=\"analyzeMessage('what course do we have?')\">🔬 Analyze Course Question</button>\n"
                    +
                    "            <button class=\"test-button\" onclick=\"analyzeMessage('show assignments due this week')\">🔬 Analyze Assignment Question</button>\n"
                    +
                    "        </div>\n" +
                    "        \n" +
                    "        <div class=\"endpoint\">\n" +
                    "            <h3>🔧 System Status</h3>\n" +
                    "            <button class=\"test-button\" onclick=\"testHealth()\">🏥 Health Check</button>\n" +
                    "            <button class=\"test-button\" onclick=\"testDatabase()\">🔌 Database Test</button>\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <h2>📊 Test Results</h2>\n" +
                    "    <div id=\"results\" class=\"results\">\n" +
                    "        Ready to test! Click any button above to see results...\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <script>\n" +
                    "        function updateResults(content, type) {\n" +
                    "            type = type || 'info';\n" +
                    "            var resultsDiv = document.getElementById('results');\n" +
                    "            resultsDiv.innerHTML = '<div class=\"' + type + '\">' + content + '</div>';\n" +
                    "        }\n" +
                    "        \n" +
                    "        function makeRequest(url, method, body, successMessage) {\n" +
                    "            updateResults('Testing: ' + url + '...', 'info');\n" +
                    "            \n" +
                    "            var options = { method: method || 'GET' };\n" +
                    "            if (body) {\n" +
                    "                options.body = body;\n" +
                    "                options.headers = { 'Content-Type': 'application/x-www-form-urlencoded' };\n" +
                    "            }\n" +
                    "            \n" +
                    "            fetch(url, options)\n" +
                    "                .then(function(response) { return response.json(); })\n" +
                    "                .then(function(data) {\n" +
                    "                    var message = successMessage || 'Request successful';\n" +
                    "                    if (data.status === 'success' || data.status === 'test_success' || data.status === 'healthy') {\n"
                    +
                    "                        var enhanced = data.databaseEnhanced ? ' (🧠 AI + 🗄️ Database Enhanced)' : '';\n"
                    +
                    "                        var graded = data.result && data.result.grade ? ' (Grade: ' + data.result.grade + ')' : '';\n"
                    +
                    "                        updateResults('<h3>✅ ' + message + enhanced + graded + '</h3><pre>' + JSON.stringify(data, null, 2) + '</pre>', 'success');\n"
                    +
                    "                    } else {\n" +
                    "                        updateResults('<h3>⚠️ ' + message + '</h3><pre>' + JSON.stringify(data, null, 2) + '</pre>', 'error');\n"
                    +
                    "                    }\n" +
                    "                })\n" +
                    "                .catch(function(error) {\n" +
                    "                    updateResults('<h3>❌ Network Error</h3><p>' + error + '</p>', 'error');\n" +
                    "                });\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Auto-Grading Functions\n" +
                    "        function gradeAssignment(mode) {\n" +
                    "            var assignmentId = prompt('Enter Assignment ID (check database for valid IDs):');\n"
                    +
                    "            if (assignmentId) {\n" +
                    "                var body = 'assignmentId=' + encodeURIComponent(assignmentId) + '&mode=' + mode;\n"
                    +
                    "                makeRequest('/grade', 'POST', body, 'Auto-Grade Assignment (' + mode + ' mode)');\n"
                    +
                    "            }\n" +
                    "        }\n" +
                    "        \n" +
                    "        function batchGrade() {\n" +
                    "            makeRequest('/grade/batch?status=ungraded&limit=10', 'GET', null, 'Batch Grade Ungraded Assignments');\n"
                    +
                    "        }\n" +
                    "        \n" +
                    "        function batchGradeByCourse() {\n" +
                    "            makeRequest('/grade/batch?course=asd&limit=10', 'GET', null, 'Batch Grade Course \"asd\"');\n"
                    +
                    "        }\n" +
                    "        \n" +
                    "        function batchGradeLimit() {\n" +
                    "            makeRequest('/grade/batch?limit=3', 'GET', null, 'Batch Grade (First 3)');\n" +
                    "        }\n" +
                    "        \n" +
                    "        function showAssignmentIds() {\n" +
                    "            makeRequest('/db/assignments', 'GET', null, 'Get Assignment IDs');\n" +
                    "        }\n" +
                    "        \n" +
                    "        // Smart AI Chat Tests\n" +
                    "        function testCourseQuestion() {\n" +
                    "            var body = 'userPrompt=' + encodeURIComponent('what course do we actually have?') + '&sessionId=test-course-123&saveToDb=true';\n"
                    +
                    "            makeRequest('/chat', 'POST', body, 'Smart Course Question');\n" +
                    "        }\n" +
                    "        \n" +
                    "        function testAssignmentQuestion() {\n" +
                    "            var body = 'userPrompt=' + encodeURIComponent('show me all assignments') + '&sessionId=test-assignment-123';\n"
                    +
                    "            makeRequest('/chat', 'POST', body, 'Smart Assignment Question');\n" +
                    "        }\n" +
                    "        \n" +
                    "        function testNetworkingQuestion() {\n" +
                    "            var body = 'userPrompt=' + encodeURIComponent('find materials about networking') + '&sessionId=test-search-123';\n"
                    +
                    "            makeRequest('/chat', 'POST', body, 'Smart Search Question');\n" +
                    "        }\n" +
                    "        \n" +
                    "        function testGeneralChat() {\n" +
                    "            var body = 'userPrompt=' + encodeURIComponent('hello how are you?') + '&sessionId=test-general-123';\n"
                    +
                    "            makeRequest('/chat', 'POST', body, 'General Chat (No Database)');\n" +
                    "        }\n" +
                    "        \n" +
                    "        function evaluateMaterial() {\n" +
                    "            var course = prompt('Enter Course Name (e.g., \"asd\"):');\n" +
                    "            var description = prompt('Enter Material Description:');\n" +
                    "            var filename = prompt('Enter Filename (optional):');\n" +
                    "            \n" +
                    "            if (course || description || filename) {\n" +
                    "                var body = '';\n" +
                    "                if (course) body += 'course=' + encodeURIComponent(course) + '&';\n" +
                    "                if (description) body += 'description=' + encodeURIComponent(description) + '&';\n"
                    +
                    "                if (filename) body += 'filename=' + encodeURIComponent(filename) + '&';\n" +
                    "                body = body.slice(0, -1); // Remove trailing &\n" +
                    "                \n" +
                    "                makeRequest('/evaluate', 'POST', body, 'Material Evaluation');\n" +
                    "            }\n" +
                    "        }\n" +
                    "        \n" +
                    "        function batchEvaluateMaterials() {\n" +
                    "            makeRequest('/evaluate/batch?course=asd&limit=5', 'GET', null, 'Batch Material Evaluation');\n"
                    +
                    "        }\n" +
                    "       \n" +
                    "        // Database API Tests\n" +
                    "        function getAllMaterials() { makeRequest('/db/materials', 'GET', null, 'Get All Materials'); }\n"
                    +
                    "        function searchMaterials() { makeRequest('/db/materials?search=Network', 'GET', null, 'Search Materials'); }\n"
                    +
                    "        function getMaterialsByCourse() { makeRequest('/db/materials?course=asd', 'GET', null, 'Materials by Course'); }\n"
                    +
                    "        \n" +
                    "        function getAllAssignments() { makeRequest('/db/assignments', 'GET', null, 'Get All Assignments'); }\n"
                    +
                    "        function searchAssignments() { makeRequest('/db/assignments?search=Networking', 'GET', null, 'Search Assignments'); }\n"
                    +
                    "        function getCompletedAssignments() { makeRequest('/db/assignments?status=yes', 'GET', null, 'Completed Assignments'); }\n"
                    +
                    "        function getUpcomingAssignments() { makeRequest('/db/assignments?upcoming=true', 'GET', null, 'Upcoming Assignments'); }\n"
                    +
                    "        \n" +
                    "        function getCourseStats() { makeRequest('/db/statistics', 'GET', null, 'Course Statistics'); }\n"
                    +
                    "        \n" +
                    "        // Analysis Tests\n" +
                    "        function analyzeMessage(message) {\n" +
                    "            var body = 'message=' + encodeURIComponent(message);\n" +
                    "            makeRequest('/analyze', 'POST', body, 'Content Analysis for: \"' + message + '\"');\n"
                    +
                    "        }\n" +
                    "        \n" +
                    "        // System Tests\n" +
                    "        function testHealth() { makeRequest('/health', 'GET', null, 'Health Check'); }\n" +
                    "        function testDatabase() { makeRequest('/db/test', 'GET', null, 'Database Connection Test'); }\n"
                    +
                    "    </script>\n" +
                    "</body>\n" +
                    "</html>";

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.getBytes("UTF-8").length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes("UTF-8"));
            }
        }
    }
    // ========================================
    // NEW DATABASE ENDPOINTS FOR MATERIALS AND ASSIGNMENTS
    // ========================================

    /**
     * Course Materials Handler
     */
    static class DatabaseMaterialsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                Map<String, String> params = parseParametersImproved(exchange);
                String search = params.get("search");
                String course = params.get("course");

                List<Map<String, Object>> materials;
                String queryType;

                if (search != null && !search.trim().isEmpty()) {
                    materials = DatabaseService.searchMaterials(search);
                    queryType = "search";
                } else if (course != null && !course.trim().isEmpty()) {
                    materials = DatabaseService.getMaterialsByCourse(course);
                    queryType = "course";
                } else {
                    materials = DatabaseService.getAllMaterials();
                    queryType = "all";
                }

                Gson gson = new Gson();
                String materialsJson = gson.toJson(materials);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"queryType\":\"" + queryType + "\"," +
                        (search != null ? "\"searchTerm\":\"" + escapeJsonString(search) + "\"," : "") +
                        (course != null ? "\"course\":\"" + escapeJsonString(course) + "\"," : "") +
                        "\"count\":" + materials.size() + "," +
                        "\"data\":" + materialsJson + "," +
                        "\"summary\":\"" + escapeJsonString(DatabaseService.getMaterialsSummary(search)) + "\"," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in database materials handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Assignments Handler
     */
    static class DatabaseAssignmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                Map<String, String> params = parseParametersImproved(exchange);
                String search = params.get("search");
                String status = params.get("status");
                String course = params.get("course");
                String upcoming = params.get("upcoming");

                List<Map<String, Object>> assignments;
                String queryType;

                if ("true".equals(upcoming)) {
                    assignments = DatabaseService.getUpcomingAssignments();
                    queryType = "upcoming";
                } else if (search != null && !search.trim().isEmpty()) {
                    assignments = DatabaseService.searchAssignments(search);
                    queryType = "search";
                } else if (status != null && !status.trim().isEmpty()) {
                    assignments = DatabaseService.getAssignmentsByStatus(status);
                    queryType = "status";
                } else if (course != null && !course.trim().isEmpty()) {
                    assignments = DatabaseService.getAssignmentsByCourse(course);
                    queryType = "course";
                } else {
                    assignments = DatabaseService.getAllAssignments();
                    queryType = "all";
                }

                Gson gson = new Gson();
                String assignmentsJson = gson.toJson(assignments);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"queryType\":\"" + queryType + "\"," +
                        (search != null ? "\"searchTerm\":\"" + escapeJsonString(search) + "\"," : "") +
                        (status != null ? "\"status\":\"" + escapeJsonString(status) + "\"," : "") +
                        (course != null ? "\"course\":\"" + escapeJsonString(course) + "\"," : "") +
                        "\"count\":" + assignments.size() + "," +
                        "\"data\":" + assignmentsJson + "," +
                        "\"summary\":\"" + escapeJsonString(DatabaseService.getAssignmentsSummary(search)) + "\"," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in database assignments handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Course Statistics Handler
     */
    static class CourseStatisticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                Map<String, Object> stats = DatabaseService.getCourseStatistics();

                Gson gson = new Gson();
                String statsJson = gson.toJson(stats);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"data\":" + statsJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in course statistics handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    /**
     * Content Analysis Test Handler
     */
    static class ContentAnalysisHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                Map<String, String> params = parseParametersImproved(exchange);
                String message = params.get("message");

                if (message == null || message.trim().isEmpty()) {
                    sendErrorResponse(exchange, "message parameter is required");
                    return;
                }

                // Analyze the message
                ContentAnalyzer.AnalysisResult analysis = ContentAnalyzer.analyzeMessage(message);

                // Get database context if needed
                String databaseContext = "";
                if (analysis.needsDatabaseData()) {
                    try {
                        if (analysis.getContentType() == ContentAnalyzer.ContentType.MATERIALS) {
                            databaseContext = DatabaseService.getMaterialsSummary(analysis.getSearchTerms());
                        } else if (analysis.getContentType() == ContentAnalyzer.ContentType.ASSIGNMENTS) {
                            databaseContext = DatabaseService.getAssignmentsSummary(analysis.getSearchTerms());
                        }
                    } catch (Exception dbError) {
                        databaseContext = "Error retrieving database context: " + dbError.getMessage();
                    }
                }

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"message\":\"" + escapeJsonString(message) + "\"," +
                        "\"analysis\":{" +
                        "\"contentType\":\"" + analysis.getContentType() + "\"," +
                        "\"queryType\":\"" + analysis.getQueryType() + "\"," +
                        "\"searchTerms\":\""
                        + (analysis.getSearchTerms() != null ? escapeJsonString(analysis.getSearchTerms()) : "") + "\","
                        +
                        "\"needsDatabase\":" + analysis.needsDatabaseData() +
                        "}," +
                        "\"databaseContext\":\"" + escapeJsonString(databaseContext) + "\"," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in content analysis handler: " + e.getMessage());
                sendErrorResponse(exchange, e.getMessage());
            }
        }
    }

    // ===========================================
    // UTILITY METHODS
    // ===========================================

    /**
     * Send error response helper
     */
    private static void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
        String errorJson = "{\"status\":\"error\",\"message\":\"" + escapeJsonString(message) + "\",\"timestamp\":"
                + System.currentTimeMillis() + "}";
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(500, errorJson.getBytes("UTF-8").length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorJson.getBytes("UTF-8"));
        }
    }

    /**
     * Set CORS headers for all responses
     */
    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, X-Requested-With, Authorization, owasp_csrftoken, X-CSRF-Token, Accept, Origin, User-Agent, Cache-Control, Keep-Alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "false");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }

    // ... (keeping existing utility methods: parseParametersImproved,
    // escapeJsonString, buildPrompt, getConfiguredApiKey)

    private static Map<String, String> parseParametersImproved(HttpExchange exchange) throws IOException {
        Map<String, String> params = new HashMap<>();

        LogUtil.info("GeminiPlugin", "=== PARSING REQUEST PARAMETERS ===");
        LogUtil.info("GeminiPlugin", "Method: " + exchange.getRequestMethod());

        // Parse URL parameters (for GET requests)
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            LogUtil.info("GeminiPlugin", "URL query string: " + query);
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = URLDecoder.decode(pair[1], "UTF-8");
                    params.put(key, value);
                    LogUtil.info("GeminiPlugin", "URL param: " + key + " = " + value);
                }
            }
        }

        // Parse POST body parameters
        if ("POST".equals(exchange.getRequestMethod())) {
            LogUtil.info("GeminiPlugin", "Processing POST request...");

            // Get Content-Type header
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            LogUtil.info("GeminiPlugin", "Content-Type: " + contentType);

            try {
                StringBuilder bodyBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        bodyBuilder.append(line);
                        if (reader.ready()) {
                            bodyBuilder.append("\n");
                        }
                    }
                }

                String body = bodyBuilder.toString();
                LogUtil.info("GeminiPlugin", "POST body length: " + body.length());
                LogUtil.info("GeminiPlugin", "POST body content: " +
                        (body.length() > 500 ? body.substring(0, 500) + "..." : body));

                if (body != null && !body.trim().isEmpty()) {

                    // ✅ FIXED: Check for multipart form data FIRST
                    if (contentType != null && contentType.contains("multipart/form-data")) {
                        LogUtil.info("GeminiPlugin", "Parsing multipart form data...");
                        parseMultipartFormData(body, contentType, params);
                    } else if (contentType != null && contentType.contains("application/json")) {
                        LogUtil.info("GeminiPlugin", "Parsing JSON request body...");
                        parseJsonData(body, params);
                    } else {
                        // Default to URL-encoded parsing
                        LogUtil.info("GeminiPlugin", "Parsing URL-encoded form data...");
                        parseUrlEncodedData(body, params);
                    }
                } else {
                    LogUtil.warn("GeminiPlugin", "POST body is empty or null");
                }
            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error reading POST body: " + e.getMessage());
            }
        }

        LogUtil.info("GeminiPlugin", "Final parsed parameters: " + params.keySet());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("chatHistory".equals(key)) {
                LogUtil.info("GeminiPlugin", "Param: " + key + " = [chat history data]");
            } else {
                LogUtil.info("GeminiPlugin", "Param: " + key + " = " + value);
            }
        }

        return params;
    }

    /**
     * Parse multipart form data (exactly as in the working previous version)
     */
    private static void parseMultipartFormData(String body, String contentType, Map<String, String> params) {
        try {
            // Extract boundary from Content-Type
            String boundary = null;
            String[] parts = contentType.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("boundary=")) {
                    boundary = part.substring(9);
                    break;
                }
            }

            if (boundary == null) {
                LogUtil.warn("GeminiPlugin", "No boundary found in multipart data");
                return;
            }

            LogUtil.info("GeminiPlugin", "Multipart boundary: " + boundary);

            // Split by boundary
            String[] sections = body.split("--" + boundary);

            for (String section : sections) {
                if (section.trim().isEmpty() || section.trim().equals("--")) {
                    continue;
                }

                LogUtil.info("GeminiPlugin", "Processing section: " +
                        (section.length() > 100 ? section.substring(0, 100) + "..." : section));

                // Look for Content-Disposition header
                String[] lines = section.split("\n");
                String fieldName = null;
                String fieldValue = null;
                boolean foundDisposition = false;
                int valueStartIndex = -1;

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();

                    if (line.startsWith("Content-Disposition:") && line.contains("form-data")) {
                        foundDisposition = true;
                        // Extract field name
                        int nameStart = line.indexOf("name=\"");
                        if (nameStart != -1) {
                            nameStart += 6; // length of "name=\""
                            int nameEnd = line.indexOf("\"", nameStart);
                            if (nameEnd != -1) {
                                fieldName = line.substring(nameStart, nameEnd);
                                LogUtil.info("GeminiPlugin", "Found field name: " + fieldName);
                            }
                        }
                    } else if (foundDisposition && line.isEmpty()) {
                        // Empty line after headers means value starts next
                        valueStartIndex = i + 1;
                        break;
                    }
                }

                // Extract field value
                if (fieldName != null && valueStartIndex != -1 && valueStartIndex < lines.length) {
                    StringBuilder valueBuilder = new StringBuilder();
                    for (int i = valueStartIndex; i < lines.length; i++) {
                        if (i > valueStartIndex) {
                            valueBuilder.append("\n");
                        }
                        valueBuilder.append(lines[i]);
                    }
                    fieldValue = valueBuilder.toString().trim();

                    // Remove trailing boundary markers
                    if (fieldValue.endsWith("--")) {
                        fieldValue = fieldValue.substring(0, fieldValue.length() - 2).trim();
                    }

                    params.put(fieldName, fieldValue);
                    LogUtil.info("GeminiPlugin", "Multipart param: " + fieldName + " = " +
                            (fieldName.equals("chatHistory") ? "[chat history data]" : fieldValue));
                }
            }

        } catch (Exception e) {
            LogUtil.error("GeminiPlugin", e, "Error parsing multipart form data: " + e.getMessage());
        }
    }

    /**
     * Parse JSON request body
     */
    private static void parseJsonData(String body, Map<String, String> params) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(body, JsonObject.class);

            // Extract common parameters
            if (jsonObject.has("userPrompt") && !jsonObject.get("userPrompt").isJsonNull()) {
                params.put("userPrompt", jsonObject.get("userPrompt").getAsString());
                LogUtil.info("GeminiPlugin", "JSON param: userPrompt = " + jsonObject.get("userPrompt").getAsString());
            }

            if (jsonObject.has("sessionId") && !jsonObject.get("sessionId").isJsonNull()) {
                params.put("sessionId", jsonObject.get("sessionId").getAsString());
                LogUtil.info("GeminiPlugin", "JSON param: sessionId = " + jsonObject.get("sessionId").getAsString());
            }

            if (jsonObject.has("chatHistory") && !jsonObject.get("chatHistory").isJsonNull()) {
                params.put("chatHistory", jsonObject.get("chatHistory").getAsString());
                LogUtil.info("GeminiPlugin", "JSON param: chatHistory = [chat history data]");
            }

            if (jsonObject.has("saveToDb") && !jsonObject.get("saveToDb").isJsonNull()) {
                params.put("saveToDb", jsonObject.get("saveToDb").getAsString());
                LogUtil.info("GeminiPlugin", "JSON param: saveToDb = " + jsonObject.get("saveToDb").getAsString());
            }

            if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                // Some chat interfaces use "message" instead of "userPrompt"
                params.put("userPrompt", jsonObject.get("message").getAsString());
                LogUtil.info("GeminiPlugin",
                        "JSON param: message (mapped to userPrompt) = " + jsonObject.get("message").getAsString());
            }

            if (jsonObject.has("text") && !jsonObject.get("text").isJsonNull()) {
                // Some chat interfaces use "text" instead of "userPrompt"
                params.put("userPrompt", jsonObject.get("text").getAsString());
                LogUtil.info("GeminiPlugin",
                        "JSON param: text (mapped to userPrompt) = " + jsonObject.get("text").getAsString());
            }

            // Extract any other string properties
            for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                com.google.gson.JsonElement value = entry.getValue();

                if (!params.containsKey(key) && !value.isJsonNull() && value.isJsonPrimitive()) {
                    params.put(key, value.getAsString());
                    LogUtil.info("GeminiPlugin", "JSON param: " + key + " = " + value.getAsString());
                }
            }

        } catch (Exception e) {
            LogUtil.error("GeminiPlugin", e, "Error parsing JSON data: " + e.getMessage());
            LogUtil.info("GeminiPlugin", "JSON parsing failed, body was: " + body);
        }
    }

    /**
     * Parse URL-encoded form data
     */
    private static void parseUrlEncodedData(String body, Map<String, String> params) {
        try {
            for (String param : body.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = URLDecoder.decode(pair[1], "UTF-8");
                    params.put(key, value);
                    LogUtil.info("GeminiPlugin", "URL-encoded param: " + key + " = " +
                            (key.equals("chatHistory") ? "[chat history data]" : value));
                }
            }
        } catch (Exception e) {
            LogUtil.error("GeminiPlugin", e, "Error parsing URL-encoded data: " + e.getMessage());
        }
    }

    // ========================================
    // AUTO-GRADING HANDLERS (Add these classes to GeminiPlugin.java)
    // ========================================

    /**
     * Auto-Grading Handler - Grade individual assignments using AI
     */
    static class AutoGradingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LogUtil.info("GeminiPlugin", "=== AUTO-GRADING API CALLED ===");

            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                // Parse parameters
                Map<String, String> params = parseParametersImproved(exchange);
                String assignmentId = params.get("assignmentId");
                String mode = params.get("mode"); // "preview" or "save"

                if (assignmentId == null || assignmentId.trim().isEmpty()) {
                    sendGradingErrorResponse(exchange, "assignmentId parameter is required", "MISSING_ASSIGNMENT_ID",
                            400);
                    return;
                }

                // Get API key
                String apiKey = getConfiguredApiKey();
                if (apiKey == null || "YOUR_API_KEY_HERE".equals(apiKey)) {
                    sendGradingErrorResponse(exchange, "Gemini API key not configured", "NO_API_KEY", 400);
                    return;
                }

                LogUtil.info("GeminiPlugin", "Grading assignment: " + assignmentId + " (mode: " + mode + ")");

                // Initialize auto-grading service
                AutoGradingService gradingService = new AutoGradingService(apiKey);

                // Grade the assignment
                AutoGradingService.GradingResult result = gradingService.gradeAssignment(assignmentId);

                // If mode is not "save", don't save to database (preview mode)
                boolean saved = !"preview".equals(mode);

                // Build response
                Gson gson = new Gson();
                String gradingJson = gson.toJson(result);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"message\":\"Assignment graded successfully\"," +
                        "\"assignmentId\":\"" + escapeJsonString(assignmentId) + "\"," +
                        "\"mode\":\"" + (saved ? "saved" : "preview") + "\"," +
                        "\"result\":" + gradingJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

                LogUtil.info("GeminiPlugin", "✅ Auto-grading completed successfully");

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in auto-grading handler: " + e.getMessage());
                sendGradingErrorResponse(exchange, e.getMessage(), "GRADING_ERROR", 500);
            }
        }
    }

    /**
     * Batch Grading Handler - Grade multiple assignments at once
     */
    static class BatchGradingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LogUtil.info("GeminiPlugin", "=== BATCH GRADING API CALLED ===");

            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                // Parse parameters
                Map<String, String> params = parseParametersImproved(exchange);
                String course = params.get("course");
                String status = params.get("status"); // "ungraded" by default
                String limit = params.get("limit"); // "10" by default

                // Get API key
                String apiKey = getConfiguredApiKey();
                if (apiKey == null || "YOUR_API_KEY_HERE".equals(apiKey)) {
                    sendGradingErrorResponse(exchange, "Gemini API key not configured", "NO_API_KEY", 400);
                    return;
                }

                // Get ungraded assignments
                List<Map<String, Object>> assignments = getUngradedAssignments(course, status, limit);

                if (assignments.isEmpty()) {
                    String responseJson = "{" +
                            "\"status\":\"success\"," +
                            "\"message\":\"No ungraded assignments found\"," +
                            "\"results\":[]," +
                            "\"timestamp\":" + System.currentTimeMillis() +
                            "}";

                    exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseJson.getBytes("UTF-8"));
                    }
                    return;
                }

                // Initialize auto-grading service
                AutoGradingService gradingService = new AutoGradingService(apiKey);

                // Grade each assignment
                List<Map<String, Object>> results = new ArrayList<>();
                int successCount = 0;
                int errorCount = 0;

                for (Map<String, Object> assignment : assignments) {
                    String assignmentId = assignment.get("id").toString();

                    try {
                        LogUtil.info("GeminiPlugin", "Batch grading assignment: " + assignmentId);

                        AutoGradingService.GradingResult gradingResult = gradingService.gradeAssignment(assignmentId);

                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("assignmentId", assignmentId);
                        resultMap.put("studentName", assignment.get("c_student_name"));
                        resultMap.put("title", assignment.get("c_assignment_title"));
                        resultMap.put("status", "success");
                        resultMap.put("grade", gradingResult.getGrade());
                        resultMap.put("percentage", gradingResult.getPercentage());

                        results.add(resultMap);
                        successCount++;

                        // Small delay to avoid rate limiting
                        Thread.sleep(1000);

                    } catch (Exception e) {
                        LogUtil.error("GeminiPlugin", e,
                                "Error grading assignment " + assignmentId + ": " + e.getMessage());

                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("assignmentId", assignmentId);
                        resultMap.put("studentName", assignment.get("c_student_name"));
                        resultMap.put("title", assignment.get("c_assignment_title"));
                        resultMap.put("status", "error");
                        resultMap.put("error", e.getMessage());

                        results.add(resultMap);
                        errorCount++;
                    }
                }

                // Build response
                Gson gson = new Gson();
                String resultsJson = gson.toJson(results);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"message\":\"Batch grading completed\"," +
                        "\"totalAssignments\":" + assignments.size() + "," +
                        "\"successCount\":" + successCount + "," +
                        "\"errorCount\":" + errorCount + "," +
                        "\"results\":" + resultsJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

                LogUtil.info("GeminiPlugin",
                        "✅ Batch grading completed: " + successCount + " success, " + errorCount + " errors");

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in batch grading handler: " + e.getMessage());
                sendGradingErrorResponse(exchange, e.getMessage(), "BATCH_GRADING_ERROR", 500);
            }
        }
    }

    /**
     * Get ungraded assignments from database
     */
    private static List<Map<String, Object>> getUngradedAssignments(String course, String status, String limitStr)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, c_assignment_title, c_student_name, c_course FROM app_fd_assignments WHERE ");

        List<Object> params = new ArrayList<>();

        // Default: get assignments without grades
        sql.append("(c_assignment_grade IS NULL OR c_assignment_grade = '')");

        // Filter by course if specified
        if (course != null && !course.trim().isEmpty()) {
            sql.append(" AND c_course = ?");
            params.add(course);
        }

        // Filter by completion status if specified
        if (status != null && !status.trim().isEmpty()) {
            if ("ungraded".equals(status)) {
                // Already handled above
            } else if ("completed".equals(status)) {
                sql.append(" AND c_assignment_completion = 'yes'");
            } else if ("submitted".equals(status)) {
                sql.append(" AND (c_field3 IS NOT NULL OR c_file_upload IS NOT NULL)");
            }
        }

        sql.append(" ORDER BY dateCreated DESC");

        // Apply limit
        int limit = 10; // default
        if (limitStr != null && !limitStr.trim().isEmpty()) {
            try {
                limit = Integer.parseInt(limitStr);
                if (limit > 50)
                    limit = 50; // Max 50 at once
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        sql.append(" LIMIT ?");
        params.add(limit);

        return DatabaseService.executeQuery(sql.toString(), params.toArray());
    }

    /**
     * Send grading error response
     */
    private static void sendGradingErrorResponse(HttpExchange exchange, String message, String errorCode,
            int statusCode) throws IOException {
        String errorJson = "{" +
                "\"status\":\"error\"," +
                "\"message\":\"" + escapeJsonString(message) + "\"," +
                "\"errorCode\":\"" + errorCode + "\"," +
                "\"timestamp\":" + System.currentTimeMillis() +
                "}";

        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorJson.getBytes("UTF-8").length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorJson.getBytes("UTF-8"));
        }
    }

    /**
     * Material Evaluation Handler - Evaluate individual course materials using AI
     */
    static class MaterialEvaluationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LogUtil.info("GeminiPlugin", "=== MATERIAL EVALUATION API CALLED ===");

            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                // Parse parameters - support both JSON and form data
                Map<String, String> params = parseParametersEnhanced(exchange);
                String materialId = params.get("materialId");
                String course = params.get("course");
                String description = params.get("description");
                String filename = params.get("filename");
                String fileContent = params.get("fileContent"); // Direct file content from browser
                String preUpload = params.get("preUpload"); // Flag for pre-upload evaluation
                String mode = params.get("mode");

                boolean isPreUpload = "true".equals(preUpload);

                LogUtil.info("GeminiPlugin", "Evaluation mode: " + (isPreUpload ? "Pre-upload" : "Post-upload"));
                LogUtil.info("GeminiPlugin", "Parameters - Course: " + course + ", Filename: " + filename +
                        ", Has file content: " + (fileContent != null && !fileContent.trim().isEmpty()) +
                        ", Content type: " + exchange.getRequestHeaders().getFirst("Content-Type"));

                // Validate required parameters
                if ((materialId == null || materialId.trim().isEmpty()) &&
                        (filename == null || filename.trim().isEmpty()) &&
                        (description == null || description.trim().isEmpty())) {
                    sendMaterialErrorResponse(exchange,
                            "At least one of materialId, filename, or description is required", "MISSING_PARAMETERS",
                            400);
                    return;
                }

                // Get API key
                String apiKey = getConfiguredApiKey();
                if (apiKey == null || "YOUR_API_KEY_HERE".equals(apiKey)) {
                    sendMaterialErrorResponse(exchange, "Gemini API key not configured", "NO_API_KEY", 400);
                    return;
                }

                // Initialize material evaluation service
                MaterialEvaluationService evaluationService = new MaterialEvaluationService(apiKey);

                // Evaluate the material with enhanced method
                MaterialEvaluationService.EvaluationResult result = evaluationService.evaluateMaterial(
                        materialId != null ? materialId : "temp",
                        course,
                        description,
                        filename,
                        fileContent, // Pass direct file content
                        isPreUpload // Pass pre-upload flag
                );

                // Build response
                Gson gson = new Gson();
                String evaluationJson = gson.toJson(result);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"message\":\"Material evaluation completed" + (isPreUpload ? " (pre-upload)" : "") + "\"," +
                        "\"recommendation\":" + result.getRecommendationPercentage() + "," +
                        "\"isRecommended\":" + result.isRecommended() + "," +
                        "\"requiresEnhancement\":" + (result.getRecommendationPercentage() < 80) + "," +
                        "\"isPreUpload\":" + isPreUpload + "," +
                        "\"result\":" + evaluationJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

                LogUtil.info("GeminiPlugin", "✅ Material evaluation completed successfully. " +
                        "Recommendation: " + result.getRecommendationPercentage() + "%" +
                        (isPreUpload ? " (pre-upload analysis)" : ""));

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in material evaluation handler: " + e.getMessage());
                sendMaterialErrorResponse(exchange, e.getMessage(), "EVALUATION_ERROR", 500);
            }
        }
    }

    /**
     * Enhanced parameter parsing that supports both JSON and form-encoded data
     * This fixes URI encoding issues by accepting JSON when form encoding fails
     */
    private static Map<String, String> parseParametersEnhanced(HttpExchange exchange) throws IOException {
        Map<String, String> params = new HashMap<>();

        LogUtil.info("GeminiPlugin", "=== ENHANCED PARAMETER PARSING ===");
        LogUtil.info("GeminiPlugin", "Method: " + exchange.getRequestMethod());

        // Parse URL parameters (for GET requests)
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            LogUtil.info("GeminiPlugin", "URL query string: " + query);
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    try {
                        String key = URLDecoder.decode(pair[0], "UTF-8");
                        String value = URLDecoder.decode(pair[1], "UTF-8");
                        params.put(key, value);
                        LogUtil.info("GeminiPlugin", "URL param: " + key + " = " + value);
                    } catch (Exception e) {
                        LogUtil.warn("GeminiPlugin", "Error decoding URL parameter: " + pair[0] + "=" + pair[1]);
                    }
                }
            }
        }

        // Parse POST body parameters
        if ("POST".equals(exchange.getRequestMethod())) {
            LogUtil.info("GeminiPlugin", "Processing POST request...");

            // Get Content-Type header
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            LogUtil.info("GeminiPlugin", "Content-Type: " + contentType);

            try {
                StringBuilder bodyBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        bodyBuilder.append(line);
                        if (reader.ready()) {
                            bodyBuilder.append("\n");
                        }
                    }
                }

                String body = bodyBuilder.toString();
                LogUtil.info("GeminiPlugin", "POST body length: " + body.length());
                LogUtil.info("GeminiPlugin", "POST body preview: " +
                        (body.length() > 200 ? body.substring(0, 200) + "..." : body));

                if (body != null && !body.trim().isEmpty()) {

                    // Check for JSON content type first
                    if (contentType != null && contentType.contains("application/json")) {
                        LogUtil.info("GeminiPlugin", "Parsing JSON request body...");
                        parseJsonDataEnhanced(body, params);
                    } else if (contentType != null && contentType.contains("multipart/form-data")) {
                        LogUtil.info("GeminiPlugin", "Parsing multipart form data...");
                        parseMultipartFormData(body, contentType, params);
                    } else {
                        // Try URL-encoded first, fall back to JSON if it fails
                        LogUtil.info("GeminiPlugin", "Trying URL-encoded parsing first...");
                        try {
                            parseUrlEncodedDataSafe(body, params);
                        } catch (Exception e) {
                            LogUtil.warn("GeminiPlugin", "URL-encoded parsing failed, trying JSON: " + e.getMessage());
                            // If URL encoding fails, try JSON
                            parseJsonDataEnhanced(body, params);
                        }
                    }
                } else {
                    LogUtil.warn("GeminiPlugin", "POST body is empty or null");
                }
            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error reading POST body: " + e.getMessage());
            }
        }

        LogUtil.info("GeminiPlugin", "Final parsed parameters: " + params.keySet());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("fileContent".equals(key) || "chatHistory".equals(key)) {
                LogUtil.info("GeminiPlugin", "Param: " + key + " = [content data - " + value.length() + " chars]");
            } else {
                LogUtil.info("GeminiPlugin", "Param: " + key + " = " + value);
            }
        }

        return params;
    }

    /**
     * Enhanced JSON parsing with better error handling
     */
    private static void parseJsonDataEnhanced(String body, Map<String, String> params) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(body, JsonObject.class);

            // Extract all string properties from JSON
            for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                com.google.gson.JsonElement value = entry.getValue();

                if (!value.isJsonNull()) {
                    if (value.isJsonPrimitive()) {
                        params.put(key, value.getAsString());
                        LogUtil.info("GeminiPlugin", "JSON param: " + key + " = " +
                                (key.equals("fileContent")
                                        ? "[file content - " + value.getAsString().length() + " chars]"
                                        : value.getAsString()));
                    } else {
                        // Convert complex objects to string
                        params.put(key, value.toString());
                        LogUtil.info("GeminiPlugin", "JSON param (complex): " + key + " = " + value.toString());
                    }
                }
            }

        } catch (Exception e) {
            LogUtil.error("GeminiPlugin", e, "Error parsing JSON data: " + e.getMessage());
            LogUtil.info("GeminiPlugin",
                    "JSON parsing failed, body was: " + body.substring(0, Math.min(500, body.length())));
        }
    }

    /**
     * Safe URL-encoded data parsing with better error handling
     */
    private static void parseUrlEncodedDataSafe(String body, Map<String, String> params) {
        try {
            for (String param : body.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    try {
                        String key = URLDecoder.decode(pair[0], "UTF-8");
                        String value = URLDecoder.decode(pair[1], "UTF-8");
                        params.put(key, value);
                        LogUtil.info("GeminiPlugin", "URL-encoded param: " + key + " = " +
                                (key.equals("fileContent") ? "[file content - " + value.length() + " chars]" : value));
                    } catch (Exception decodeError) {
                        LogUtil.warn("GeminiPlugin", "Error decoding parameter: " + pair[0] + "=" +
                                pair[1].substring(0, Math.min(100, pair[1].length())) + "...");
                        // Try to use the raw value if decoding fails
                        params.put(pair[0], pair[1]);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error("GeminiPlugin", e, "Error parsing URL-encoded data: " + e.getMessage());
            throw e; // Re-throw to trigger JSON fallback
        }
    }

    /**
     * Batch Material Evaluation Handler - Evaluate multiple materials at once
     */
    static class BatchMaterialEvaluationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LogUtil.info("GeminiPlugin", "=== BATCH MATERIAL EVALUATION API CALLED ===");

            try {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                // Handle CORS preflight
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }

                // Parse parameters
                Map<String, String> params = parseParametersImproved(exchange);
                String course = params.get("course");
                String limit = params.get("limit");

                // Get API key
                String apiKey = getConfiguredApiKey();
                if (apiKey == null || "YOUR_API_KEY_HERE".equals(apiKey)) {
                    sendMaterialErrorResponse(exchange, "Gemini API key not configured", "NO_API_KEY", 400);
                    return;
                }

                // Get materials to evaluate
                List<Map<String, Object>> materials = getUnevaluatedMaterials(course, limit);

                if (materials.isEmpty()) {
                    String responseJson = "{" +
                            "\"status\":\"success\"," +
                            "\"message\":\"No materials found for evaluation\"," +
                            "\"results\":[]," +
                            "\"timestamp\":" + System.currentTimeMillis() +
                            "}";

                    exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseJson.getBytes("UTF-8"));
                    }
                    return;
                }

                // Initialize evaluation service
                MaterialEvaluationService evaluationService = new MaterialEvaluationService(apiKey);

                // Evaluate each material
                List<Map<String, Object>> results = new ArrayList<>();
                int successCount = 0;
                int errorCount = 0;
                int recommendedCount = 0;
                int needsEnhancementCount = 0;

                for (Map<String, Object> material : materials) {
                    String materialId = material.get("id").toString();
                    String materialCourse = (String) material.get("c_select_course");
                    String materialDesc = (String) material.get("c_course_information");
                    String materialFile = (String) material.get("c_course_fileupload");

                    try {
                        LogUtil.info("GeminiPlugin", "Batch evaluating material: " + materialFile);

                        MaterialEvaluationService.EvaluationResult evalResult = evaluationService.evaluateMaterial(
                                materialId, materialCourse, materialDesc, materialFile);

                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("materialId", materialId);
                        resultMap.put("filename", materialFile);
                        resultMap.put("course", materialCourse);
                        resultMap.put("status", "success");
                        resultMap.put("recommendation", evalResult.getRecommendationPercentage());
                        resultMap.put("isRecommended", evalResult.isRecommended());
                        resultMap.put("requiresEnhancement", evalResult.getRecommendationPercentage() < 80);
                        resultMap.put("overallRating", evalResult.getOverallRating());

                        results.add(resultMap);
                        successCount++;

                        if (evalResult.getRecommendationPercentage() >= 80) {
                            recommendedCount++;
                        } else {
                            needsEnhancementCount++;
                        }

                        // Small delay to avoid rate limiting
                        Thread.sleep(1000);

                    } catch (Exception e) {
                        LogUtil.error("GeminiPlugin", e,
                                "Error evaluating material " + materialId + ": " + e.getMessage());

                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("materialId", materialId);
                        resultMap.put("filename", materialFile);
                        resultMap.put("course", materialCourse);
                        resultMap.put("status", "error");
                        resultMap.put("error", e.getMessage());

                        results.add(resultMap);
                        errorCount++;
                    }
                }

                // Build response
                Gson gson = new Gson();
                String resultsJson = gson.toJson(results);

                String responseJson = "{" +
                        "\"status\":\"success\"," +
                        "\"message\":\"Batch material evaluation completed\"," +
                        "\"totalMaterials\":" + materials.size() + "," +
                        "\"successCount\":" + successCount + "," +
                        "\"errorCount\":" + errorCount + "," +
                        "\"recommendedCount\":" + recommendedCount + "," +
                        "\"needsEnhancementCount\":" + needsEnhancementCount + "," +
                        "\"results\":" + resultsJson + "," +
                        "\"timestamp\":" + System.currentTimeMillis() +
                        "}";

                exchange.sendResponseHeaders(200, responseJson.getBytes("UTF-8").length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseJson.getBytes("UTF-8"));
                }

                LogUtil.info("GeminiPlugin", "✅ Batch evaluation completed: " + successCount + " success, " + errorCount
                        + " errors, " + recommendedCount + " recommended");

            } catch (Exception e) {
                LogUtil.error("GeminiPlugin", e, "Error in batch material evaluation handler: " + e.getMessage());
                sendMaterialErrorResponse(exchange, e.getMessage(), "BATCH_EVALUATION_ERROR", 500);
            }
        }
    }

    /**
     * Get unevaluated materials from database
     */
    private static List<Map<String, Object>> getUnevaluatedMaterials(String course, String limitStr)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append(
                "SELECT id, c_select_course, c_course_fileupload, c_course_information FROM app_fd_materials WHERE 1=1");

        List<Object> params = new ArrayList<>();

        // Filter by course if specified
        if (course != null && !course.trim().isEmpty()) {
            sql.append(" AND c_select_course = ?");
            params.add(course);
        }

        sql.append(" ORDER BY dateCreated DESC");

        // Apply limit
        int limit = 5; // default
        if (limitStr != null && !limitStr.trim().isEmpty()) {
            try {
                limit = Integer.parseInt(limitStr);
                if (limit > 20)
                    limit = 20; // Max 20 at once
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        sql.append(" LIMIT ?");
        params.add(limit);

        return DatabaseService.executeQuery(sql.toString(), params.toArray());
    }

    /**
     * Send material evaluation error response
     */
    private static void sendMaterialErrorResponse(HttpExchange exchange, String message, String errorCode,
            int statusCode) throws IOException {
        String errorJson = "{" +
                "\"status\":\"error\"," +
                "\"message\":\"" + escapeJsonString(message) + "\"," +
                "\"errorCode\":\"" + errorCode + "\"," +
                "\"timestamp\":" + System.currentTimeMillis() +
                "}";

        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorJson.getBytes("UTF-8").length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorJson.getBytes("UTF-8"));
        }
    }

    private static String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String buildPrompt(String userPrompt, String chatHistory) {
        StringBuilder prompt = new StringBuilder();

        if (chatHistory != null && !chatHistory.trim().isEmpty() && !"[]".equals(chatHistory.trim())) {
            prompt.append("Chat History: ").append(chatHistory).append("\n\n");
        } else {
            prompt.append("Chat History: No previous conversation.\n\n");
        }

        prompt.append("User: ").append(userPrompt).append("\n\n");
        prompt.append("Please respond as a helpful AI assistant:");

        return prompt.toString();
    }

    private static String getConfiguredApiKey() {
        // System property
        String apiKey = System.getProperty("gemini.api.key");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey;
        }

        // Environment variable
        apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey;
        }

        // Hardcoded for testing (replace with your actual API key)
        return "AIzaSyDQ-Nk6oiJAaHkcWM4V20EXUbOtUXH4T3U";
    }

    // ===========================================
    // PROCESS TOOL METHODS (for workflow support)
    // ===========================================

    @Override
    public Object execute(Map properties) {
        LogUtil.info(getClassName(), "=== PROCESS TOOL EXECUTE ===");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Process tool executed successfully");
        result.put("timestamp", System.currentTimeMillis());
        result.put("embeddedServerRunning", isEmbeddedServerRunning());
        result.put("embeddedServerPort", getEmbeddedServerPort());
        result.put("databaseConnected", DatabaseService.testConnection());

        return result;
    }
}