package org.joget.gemini;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.joget.commons.util.LogUtil;
import com.google.gson.Gson;

/**
 * Enhanced AI Auto-Grading Service for Student Assignments
 * ✅ NOW SUPPORTS: Dual file processing (Questions + Answers)
 * ✅ ENHANCED: Smart file detection and better AI context
 * ✅ IMPROVED: More accurate grading with question-answer matching
 */
public class AutoGradingService {

    private static final String UPLOAD_PATH = "wflow/app_formuploads/"; // Default Joget upload path
    private final GeminiService geminiService;
    private final Gson gson;

    public AutoGradingService(String apiKey) {
        this.geminiService = new GeminiService(apiKey);
        this.gson = new Gson();
    }

    /**
     * Grade an assignment automatically using AI with enhanced dual file support
     */
    public GradingResult gradeAssignment(String assignmentId) throws Exception {
        LogUtil.info("AutoGradingService", "Starting enhanced auto-grading for assignment: " + assignmentId);

        // 1. Get assignment details from database
        AssignmentSubmission submission = getAssignmentSubmission(assignmentId);
        if (submission == null) {
            throw new IllegalArgumentException("Assignment not found: " + assignmentId);
        }

        // 2. Extract content from both question file and answer file
        String questionsContent = "";
        String answersContent = "";

        // Extract questions file content (from teacher)
        if (submission.getQuestionsFile() != null && !submission.getQuestionsFile().trim().isEmpty()) {
            LogUtil.info("AutoGradingService", "Found questions file: " + submission.getQuestionsFile());
            questionsContent = extractFileContent(submission.getQuestionsFile(), assignmentId);
        } else {
            LogUtil.info("AutoGradingService", "No questions file found");
        }

        // Extract answers file content (from student)
        if (submission.getUploadedFile() != null && !submission.getUploadedFile().trim().isEmpty()) {
            LogUtil.info("AutoGradingService", "Found answers file: " + submission.getUploadedFile());
            answersContent = extractFileContent(submission.getUploadedFile(), assignmentId);
        } else {
            LogUtil.info("AutoGradingService", "No answers file found");
        }

        // 3. Prepare enhanced grading context with both files
        String gradingPrompt = buildGradingPrompt(submission, questionsContent, answersContent);

        // 4. Call Gemini AI for grading
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.3); // Lower temperature for consistent grading
        params.put("maxOutputTokens", 1500); // Increased for detailed feedback

        String aiResponse = geminiService.generateContent("gemini-1.5-flash", gradingPrompt, params);

        // 5. Parse AI response to extract grade and remarks
        GradingResult result = parseGradingResponse(aiResponse, submission);

        // 6. Save grading result to database (optional)
        saveGradingResult(assignmentId, result);

        LogUtil.info("AutoGradingService", "Enhanced auto-grading completed for assignment: " + assignmentId);
        return result;
    }

    /**
     * Get assignment submission details from database with enhanced file detection
     */
    private AssignmentSubmission getAssignmentSubmission(String assignmentId) throws SQLException {
        // ✅ ENHANCED: Start with known working columns, then try to get additional file
        // fields
        String sql = "SELECT id, c_assignment_title, c_course, c_due_date, c_student_name, " +
                "c_field3, c_answer, c_field2 as uploaded_file, c_assignment_completion, " +
                "c_assignment_grade, c_assignment_remarks_teacher " +
                "FROM app_fd_assignments WHERE id = ?";

        LogUtil.info("AutoGradingService", "Executing SQL: " + sql);
        List<Map<String, Object>> results = DatabaseService.executeQuery(sql, assignmentId);

        if (results.isEmpty()) {
            LogUtil.warn("AutoGradingService", "No assignment found with ID: " + assignmentId);
            return null;
        }

        Map<String, Object> row = results.get(0);
        AssignmentSubmission submission = new AssignmentSubmission();

        submission.setId(row.get("id").toString());
        submission.setTitle((String) row.get("c_assignment_title"));
        submission.setCourse((String) row.get("c_course"));
        submission.setStudentName((String) row.get("c_student_name"));

        // ✅ ENHANCED: Handle both answer fields - use c_field3 as primary, c_answer as
        // secondary
        String primaryAnswer = (String) row.get("c_field3");
        String secondaryAnswer = (String) row.get("c_answer");
        String combinedAnswer = "";

        if (primaryAnswer != null && !primaryAnswer.trim().isEmpty()) {
            combinedAnswer = primaryAnswer;
        }
        if (secondaryAnswer != null && !secondaryAnswer.trim().isEmpty()) {
            if (!combinedAnswer.isEmpty()) {
                combinedAnswer += "\n\nAdditional Answer: " + secondaryAnswer;
            } else {
                combinedAnswer = secondaryAnswer;
            }
        }
        submission.setAnswer(combinedAnswer);

        // ✅ ENHANCED: Try to get both question file and answer file with smart
        // detection
        String questionsFile = null;
        String answersFile = null;

        // First, get the file from c_field2 (we know this field exists)
        if (row.get("uploaded_file") != null && !row.get("uploaded_file").toString().trim().isEmpty()) {
            String filename = row.get("uploaded_file").toString();
            LogUtil.info("AutoGradingService", "Found file in c_field2: " + filename);

            // ✅ NEW: Smart file type detection based on filename
            String filenameLower = filename.toLowerCase();
            if (filenameLower.contains("question") || filenameLower.contains("assignment") ||
                    filenameLower.contains("problem") || filenameLower.contains("task")) {
                questionsFile = filename;
                LogUtil.info("AutoGradingService", "Identified as questions file based on filename: " + questionsFile);
            } else if (filenameLower.contains("answer") || filenameLower.contains("solution") ||
                    filenameLower.contains("response") || filenameLower.contains("submission")) {
                answersFile = filename;
                LogUtil.info("AutoGradingService", "Identified as answers file based on filename: " + answersFile);
            } else {
                // Default assumption: if unclear, assume it's the student's answer file
                answersFile = filename;
                LogUtil.info("AutoGradingService", "Defaulting to answers file: " + answersFile);
            }
        }

        // ✅ ENHANCED: Try additional query to discover more file fields
        try {
            String additionalSql = "SELECT * FROM app_fd_assignments WHERE id = ? LIMIT 1";
            List<Map<String, Object>> fullResults = DatabaseService.executeQuery(additionalSql, assignmentId);

            if (!fullResults.isEmpty()) {
                Map<String, Object> fullRow = fullResults.get(0);
                LogUtil.info("AutoGradingService", "Available columns: " + fullRow.keySet().toString());

                // Look for additional file fields in the full result
                for (String key : fullRow.keySet()) {
                    Object value = fullRow.get(key);
                    if (value != null && !value.toString().trim().isEmpty()) {
                        String valueStr = value.toString();

                        // Check if this looks like a filename
                        if (valueStr.endsWith(".docx") || valueStr.endsWith(".pdf") || valueStr.endsWith(".txt") ||
                                valueStr.endsWith(".doc") || valueStr.endsWith(".xlsx")) {
                            LogUtil.info("AutoGradingService", "Found file in column " + key + ": " + valueStr);

                            String valueStrLower = valueStr.toLowerCase();

                            // Use naming conventions to determine file type
                            if (valueStrLower.contains("question") || valueStrLower.contains("assignment") ||
                                    valueStrLower.contains("problem") || valueStrLower.contains("task")) {
                                if (questionsFile == null) {
                                    questionsFile = valueStr;
                                    LogUtil.info("AutoGradingService",
                                            "Identified as questions file: " + questionsFile);
                                }
                            } else if (valueStrLower.contains("answer") || valueStrLower.contains("solution") ||
                                    valueStrLower.contains("response") || valueStrLower.contains("submission")) {
                                if (answersFile == null) {
                                    answersFile = valueStr;
                                    LogUtil.info("AutoGradingService", "Identified as answers file: " + answersFile);
                                }
                            } else if (!key.equals("uploaded_file") && questionsFile == null
                                    && !valueStr.equals(answersFile)) {
                                // If we haven't identified this file yet and it's in a different field, it
                                // might be the questions
                                questionsFile = valueStr;
                                LogUtil.info("AutoGradingService",
                                        "Tentatively identified as questions file from field " + key + ": "
                                                + questionsFile);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.warn("AutoGradingService", "Could not query additional fields: " + e.getMessage());
        }

        submission.setUploadedFile(answersFile);
        submission.setQuestionsFile(questionsFile);
        submission.setQuestions("Assignment: " + row.get("c_assignment_title")); // Use title as context

        LogUtil.info("AutoGradingService", "Retrieved assignment: " + submission.getTitle() +
                " for student: " + (submission.getStudentName() != null ? submission.getStudentName() : "Unknown") +
                " with answer: "
                + (submission.getAnswer() != null && !submission.getAnswer().isEmpty()
                        ? submission.getAnswer().substring(0, Math.min(50, submission.getAnswer().length())) + "..."
                        : "No answer")
                +
                " questions file: "
                + (submission.getQuestionsFile() != null ? submission.getQuestionsFile() : "No file") +
                " and answers file: "
                + (submission.getUploadedFile() != null ? submission.getUploadedFile() : "No file"));

        return submission;
    }

    /**
     * Extract content from uploaded files (PDF, DOCX, TXT) with enhanced path
     * detection
     */
    private String extractFileContent(String filename, String assignmentId) {
        try {
            // ✅ ENHANCED: Joget file path structure with assignment ID subdirectories
            String[] possiblePaths = {
                    // Primary paths with assignment ID subdirectory (Joget's actual structure)
                    UPLOAD_PATH + "assignments/" + assignmentId + "/" + filename,
                    "./wflow/app_formuploads/assignments/" + assignmentId + "/" + filename,
                    System.getProperty("user.dir") + "/wflow/app_formuploads/assignments/" + assignmentId + "/"
                            + filename,

                    // Alternative paths if assignments subfolder doesn't exist
                    UPLOAD_PATH + assignmentId + "/" + filename,
                    "./wflow/app_formuploads/" + assignmentId + "/" + filename,
                    System.getProperty("user.dir") + "/wflow/app_formuploads/" + assignmentId + "/" + filename,

                    // Fallback to old paths (for backward compatibility)
                    UPLOAD_PATH + filename,
                    "uploads/" + filename,
                    "../uploads/" + filename,
                    "./wflow/app_formuploads/" + filename,
                    System.getProperty("user.dir") + "/wflow/app_formuploads/" + filename
            };

            Path filePath = null;
            for (String pathStr : possiblePaths) {
                Path testPath = Paths.get(pathStr);
                LogUtil.info("AutoGradingService", "Checking path: " + testPath.toString());
                if (Files.exists(testPath)) {
                    filePath = testPath;
                    LogUtil.info("AutoGradingService", "✅ Found file at: " + testPath.toString());
                    break;
                }
            }

            if (filePath == null) {
                LogUtil.warn("AutoGradingService", "File not found: " + filename + " for assignment: " + assignmentId);
                LogUtil.info("AutoGradingService", "Searched in paths:");
                for (String path : possiblePaths) {
                    LogUtil.info("AutoGradingService", "  - " + path);
                }

                // Additional diagnostic information
                LogUtil.info("AutoGradingService", "Working directory: " + System.getProperty("user.dir"));
                LogUtil.info("AutoGradingService", "Assignment ID: " + assignmentId);
                LogUtil.info("AutoGradingService", "Filename: " + filename);

                return "File not found: " + filename + " for assignment " + assignmentId
                        + ". Please ensure the file is uploaded correctly in Joget.";
            }

            LogUtil.info("AutoGradingService", "Extracting content from: " + filePath.toString());

            String extension = getFileExtension(filename).toLowerCase();

            switch (extension) {
                case "pdf":
                    return extractPDFContent(filePath);
                case "docx":
                    return extractDOCXContent(filePath);
                case "txt":
                    return extractTXTContent(filePath);
                default:
                    LogUtil.warn("AutoGradingService", "Unsupported file type: " + extension);
                    return "Unsupported file type: " + extension + ". Please use PDF, DOCX, or TXT files.";
            }

        } catch (Exception e) {
            LogUtil.error("AutoGradingService", e, "Error extracting file content: " + e.getMessage());
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * Extract content from PDF files
     */
    private String extractPDFContent(Path filePath) throws IOException {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);
            LogUtil.info("AutoGradingService", "Extracted PDF content: " + content.length() + " characters");
            return content;
        }
    }

    /**
     * Extract content from DOCX files
     */
    private String extractDOCXContent(Path filePath) throws IOException {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(filePath))) {
            StringBuilder content = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }

            String result = content.toString();
            LogUtil.info("AutoGradingService", "Extracted DOCX content: " + result.length() + " characters");
            return result;
        }
    }

    /**
     * Extract content from TXT files
     */
    private String extractTXTContent(Path filePath) throws IOException {
        String content = new String(Files.readAllBytes(filePath), "UTF-8");
        LogUtil.info("AutoGradingService", "Extracted TXT content: " + content.length() + " characters");
        return content;
    }

    /**
     * Build comprehensive grading prompt for AI with separate question and answer
     * files
     */
    private String buildGradingPrompt(AssignmentSubmission submission, String questionsContent, String answersContent) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an experienced teacher tasked with grading a student assignment. ");
        prompt.append("Please analyze the submission and provide a grade and detailed feedback.\n\n");

        prompt.append("=== ASSIGNMENT DETAILS ===\n");
        prompt.append("Title: ").append(submission.getTitle()).append("\n");
        prompt.append("Course: ").append(submission.getCourse()).append("\n");
        prompt.append("Student: ").append(submission.getStudentName()).append("\n");

        if (submission.getQuestions() != null && !submission.getQuestions().trim().isEmpty()) {
            prompt.append("Assignment Context: ").append(submission.getQuestions()).append("\n");
        }

        prompt.append("\n=== ASSIGNMENT QUESTIONS (FROM TEACHER) ===\n");
        if (questionsContent != null && !questionsContent.trim().isEmpty()
                && !questionsContent.startsWith("File not found:")) {
            prompt.append("Questions File Content:\n");
            prompt.append(questionsContent).append("\n\n");
        } else if (questionsContent != null && questionsContent.startsWith("File not found:")) {
            prompt.append("Questions File: ").append(questionsContent).append("\n\n");
        } else {
            prompt.append("Questions File: [No questions file provided]\n\n");
        }

        prompt.append("=== STUDENT SUBMISSION ===\n");

        // Text answer
        if (submission.getAnswer() != null && !submission.getAnswer().trim().isEmpty()) {
            prompt.append("Student Text Answer: ").append(submission.getAnswer()).append("\n\n");
        } else {
            prompt.append("Student Text Answer: [No text answer provided]\n\n");
        }

        // Student's answer file content
        if (answersContent != null && !answersContent.trim().isEmpty()
                && !answersContent.startsWith("File not found:")) {
            prompt.append("Student's Answer File Content:\n");
            prompt.append(answersContent).append("\n\n");
        } else if (answersContent != null && answersContent.startsWith("File not found:")) {
            prompt.append("Student's Answer File: ").append(answersContent).append("\n\n");
        } else {
            prompt.append("Student's Answer File: [No answer file uploaded]\n\n");
        }

        prompt.append("=== GRADING INSTRUCTIONS ===\n");
        prompt.append("Please evaluate this submission based on:\n");
        prompt.append("1. Correctness and accuracy of the answers compared to the questions asked\n");
        prompt.append("2. Completeness - did the student answer all questions?\n");
        prompt.append("3. Understanding of the topic demonstrated in the answers\n");
        prompt.append("4. Quality of explanation and reasoning\n");
        prompt.append("5. Following assignment requirements and format\n\n");

        // Enhanced instructions when both files are available
        if (questionsContent != null && !questionsContent.trim().isEmpty()
                && !questionsContent.startsWith("File not found:") &&
                answersContent != null && !answersContent.trim().isEmpty()
                && !answersContent.startsWith("File not found:")) {
            prompt.append("IMPORTANT: You have access to both the original questions and the student's answers. ");
            prompt.append(
                    "Please compare the student's responses directly against each question to evaluate accuracy and completeness. ");
            prompt.append("Provide specific feedback referencing individual questions and answers.\n\n");
        } else if (questionsContent != null && !questionsContent.trim().isEmpty()
                && !questionsContent.startsWith("File not found:")) {
            prompt.append(
                    "IMPORTANT: You have the original questions but the student may have provided answers in text form or no file was uploaded. ");
            prompt.append("Evaluate based on the questions provided and any available student responses.\n\n");
        }

        prompt.append("Provide your response in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"grade\": \"A/B/C/D/F\",\n");
        prompt.append("  \"percentage\": 85,\n");
        prompt.append("  \"remarks\": \"Detailed feedback explaining the grade...\",\n");
        prompt.append("  \"strengths\": [\"List of things done well\"],\n");
        prompt.append("  \"improvements\": [\"List of areas for improvement\"]\n");
        prompt.append("}\n\n");

        prompt.append("Be constructive, specific, and fair in your evaluation. ");
        prompt.append("Reference specific questions and answers when providing feedback.");

        return prompt.toString();
    }

    /**
     * Parse AI grading response
     */
    private GradingResult parseGradingResponse(String aiResponse, AssignmentSubmission submission) {
        try {
            // Try to extract JSON from the response
            String jsonStr = extractJSON(aiResponse);

            if (jsonStr != null) {
                Map<String, Object> responseMap = gson.fromJson(jsonStr, Map.class);

                GradingResult result = new GradingResult();
                result.setAssignmentId(submission.getId());
                result.setGrade((String) responseMap.get("grade"));
                result.setPercentage(((Number) responseMap.get("percentage")).intValue());
                result.setRemarks((String) responseMap.get("remarks"));
                result.setAiGenerated(true);
                result.setTimestamp(new java.util.Date());

                @SuppressWarnings("unchecked")
                List<String> strengths = (List<String>) responseMap.get("strengths");
                @SuppressWarnings("unchecked")
                List<String> improvements = (List<String>) responseMap.get("improvements");

                result.setStrengths(strengths);
                result.setImprovements(improvements);

                return result;
            }

        } catch (Exception e) {
            LogUtil.warn("AutoGradingService", "Failed to parse JSON response: " + e.getMessage());
        }

        // Fallback: create result from raw response
        GradingResult result = new GradingResult();
        result.setAssignmentId(submission.getId());
        result.setGrade("C"); // Default grade
        result.setPercentage(75); // Default percentage
        result.setRemarks("AI Analysis: " + aiResponse);
        result.setAiGenerated(true);
        result.setTimestamp(new java.util.Date());

        return result;
    }

    /**
     * Extract JSON from AI response
     */
    private String extractJSON(String response) {
        int startIndex = response.indexOf("{");
        int endIndex = response.lastIndexOf("}");

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        return null;
    }

    /**
     * Save grading result to database
     */
    private void saveGradingResult(String assignmentId, GradingResult result) throws SQLException {
        String updateSql = "UPDATE app_fd_assignments SET " +
                "c_assignment_grade = ?, " +
                "c_assignment_remarks_teacher = ?, " +
                "c_assignment_completion = 'yes', " +
                "dateModified = NOW() " +
                "WHERE id = ?";

        String remarks = result.getRemarks();
        if (result.getStrengths() != null && !result.getStrengths().isEmpty()) {
            remarks += "\n\nStrengths: " + String.join(", ", result.getStrengths());
        }
        if (result.getImprovements() != null && !result.getImprovements().isEmpty()) {
            remarks += "\n\nAreas for improvement: " + String.join(", ", result.getImprovements());
        }

        // Add timestamp to remarks
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        remarks += "\n\n[AI Auto-Graded on " + sdf.format(new java.util.Date()) + "]";

        DatabaseService.executeUpdate(updateSql, result.getGrade(), remarks, assignmentId);
        LogUtil.info("AutoGradingService", "Grading result saved to database for assignment: " + assignmentId);
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Get current timestamp as formatted string
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date());
    }

    // ===============================
    // ENHANCED DATA CLASSES
    // ===============================

    /**
     * Enhanced AssignmentSubmission class with dual file support
     */
    public static class AssignmentSubmission {
        private String id;
        private String title;
        private String course;
        private String studentName;
        private String answer;
        private String uploadedFile; // Student's answer file
        private String questionsFile; // Teacher's questions file
        private String questions;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCourse() {
            return course;
        }

        public void setCourse(String course) {
            this.course = course;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public String getUploadedFile() {
            return uploadedFile;
        }

        public void setUploadedFile(String uploadedFile) {
            this.uploadedFile = uploadedFile;
        }

        public String getQuestionsFile() {
            return questionsFile;
        }

        public void setQuestionsFile(String questionsFile) {
            this.questionsFile = questionsFile;
        }

        public String getQuestions() {
            return questions;
        }

        public void setQuestions(String questions) {
            this.questions = questions;
        }
    }

    /**
     * GradingResult class (unchanged)
     */
    public static class GradingResult {
        private String assignmentId;
        private String grade;
        private int percentage;
        private String remarks;
        private List<String> strengths;
        private List<String> improvements;
        private boolean aiGenerated;
        private java.util.Date timestamp;

        // Getters and setters
        public String getAssignmentId() {
            return assignmentId;
        }

        public void setAssignmentId(String assignmentId) {
            this.assignmentId = assignmentId;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getImprovements() {
            return improvements;
        }

        public void setImprovements(List<String> improvements) {
            this.improvements = improvements;
        }

        public boolean isAiGenerated() {
            return aiGenerated;
        }

        public void setAiGenerated(boolean aiGenerated) {
            this.aiGenerated = aiGenerated;
        }

        public java.util.Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(java.util.Date timestamp) {
            this.timestamp = timestamp;
        }
    }
}