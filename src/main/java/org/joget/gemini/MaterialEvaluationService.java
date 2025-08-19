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
 * AI-Powered Material Evaluation Service for Course Content
 * ✅ SUPPORTS: Both pre-upload (browser file content) and post-upload (server
 * file) analysis
 * ✅ ANALYZES: Course materials for educational quality and suitability
 * ✅ PROVIDES: Percentage recommendations and improvement suggestions
 * ✅ VALIDATES: Content before final upload to ensure quality standards
 */
public class MaterialEvaluationService {

    private static final String UPLOAD_PATH = "wflow/app_formuploads/"; // Default Joget upload path
    private final GeminiService geminiService;
    private final Gson gson;

    public MaterialEvaluationService(String apiKey) {
        this.geminiService = new GeminiService(apiKey);
        this.gson = new Gson();
    }

    /**
     * Evaluate course material - supports both pre-upload and post-upload scenarios
     */
    public EvaluationResult evaluateMaterial(String materialId, String course, String description, String filename)
            throws Exception {
        return evaluateMaterial(materialId, course, description, filename, null, false);
    }

    /**
     * Enhanced evaluate method with direct file content support (for pre-upload
     * analysis)
     */
    public EvaluationResult evaluateMaterial(String materialId, String course, String description,
            String filename, String directFileContent, boolean isPreUpload) throws Exception {
        LogUtil.info("MaterialEvaluationService", "Starting material evaluation for: " + filename +
                (isPreUpload ? " (pre-upload)" : " (post-upload)"));

        // 1. Get file content - either from direct content or file extraction
        String fileContent = "";
        if (directFileContent != null && !directFileContent.trim().isEmpty()) {
            // Pre-upload: Use content sent directly from browser
            fileContent = directFileContent;
            LogUtil.info("MaterialEvaluationService",
                    "Using direct file content: " + fileContent.length() + " characters");
        } else if (filename != null && !filename.trim().isEmpty() && !isPreUpload) {
            // Post-upload: Extract from server file
            LogUtil.info("MaterialEvaluationService", "Extracting content from server file: " + filename);
            fileContent = extractFileContent(filename, materialId);
        } else {
            LogUtil.info("MaterialEvaluationService", "No file content available for analysis");
        }

        // 2. Get related course materials for context
        String courseContext = getCourseContext(course);

        // 3. Build evaluation prompt
        String evaluationPrompt = buildEvaluationPrompt(course, description, filename, fileContent, courseContext,
                isPreUpload);

        // 4. Call Gemini AI for evaluation
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.2); // Lower temperature for consistent evaluation
        params.put("maxOutputTokens", 2000); // Increased for detailed feedback

        String aiResponse = geminiService.generateContent("gemini-1.5-flash", evaluationPrompt, params);

        // 5. Parse AI response to extract evaluation results
        EvaluationResult result = parseEvaluationResponse(aiResponse, course, filename);

        LogUtil.info("MaterialEvaluationService", "Material evaluation completed. Recommendation: " +
                result.getRecommendationPercentage() + "%" + (isPreUpload ? " (pre-upload)" : ""));
        return result;
    }

    /**
     * Extract content from uploaded files (PDF, DOCX, TXT) - for post-upload
     * scenarios
     */
    private String extractFileContent(String filename, String materialId) {
        try {
            // Enhanced path detection for material uploads
            String[] possiblePaths = {
                    // Primary paths with material ID subdirectory
                    UPLOAD_PATH + "materials/" + materialId + "/" + filename,
                    "./wflow/app_formuploads/materials/" + materialId + "/" + filename,
                    System.getProperty("user.dir") + "/wflow/app_formuploads/materials/" + materialId + "/" + filename,

                    // Alternative paths
                    UPLOAD_PATH + materialId + "/" + filename,
                    "./wflow/app_formuploads/" + materialId + "/" + filename,
                    System.getProperty("user.dir") + "/wflow/app_formuploads/" + materialId + "/" + filename,

                    // Fallback paths
                    UPLOAD_PATH + filename,
                    "uploads/" + filename,
                    "../uploads/" + filename,
                    "./wflow/app_formuploads/" + filename,
                    System.getProperty("user.dir") + "/wflow/app_formuploads/" + filename
            };

            Path filePath = null;
            for (String pathStr : possiblePaths) {
                Path testPath = Paths.get(pathStr);
                LogUtil.info("MaterialEvaluationService", "Checking path: " + testPath.toString());
                if (Files.exists(testPath)) {
                    filePath = testPath;
                    LogUtil.info("MaterialEvaluationService", "✅ Found file at: " + testPath.toString());
                    break;
                }
            }

            if (filePath == null) {
                LogUtil.warn("MaterialEvaluationService", "File not found: " + filename);
                return "File not found: " + filename + ". Content evaluation will be based on description only.";
            }

            String extension = getFileExtension(filename).toLowerCase();

            switch (extension) {
                case "pdf":
                    return extractPDFContent(filePath);
                case "docx":
                    return extractDOCXContent(filePath);
                case "txt":
                    return extractTXTContent(filePath);
                default:
                    LogUtil.warn("MaterialEvaluationService", "Unsupported file type: " + extension);
                    return "Unsupported file type: " + extension + ". Please use PDF, DOCX, or TXT files.";
            }

        } catch (Exception e) {
            LogUtil.error("MaterialEvaluationService", e, "Error extracting file content: " + e.getMessage());
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
            LogUtil.info("MaterialEvaluationService", "Extracted PDF content: " + content.length() + " characters");
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
            LogUtil.info("MaterialEvaluationService", "Extracted DOCX content: " + result.length() + " characters");
            return result;
        }
    }

    /**
     * Extract content from TXT files
     */
    private String extractTXTContent(Path filePath) throws IOException {
        String content = new String(Files.readAllBytes(filePath), "UTF-8");
        LogUtil.info("MaterialEvaluationService", "Extracted TXT content: " + content.length() + " characters");
        return content;
    }

    /**
     * Get context about existing course materials
     */
    private String getCourseContext(String course) {
        try {
            List<Map<String, Object>> existingMaterials = DatabaseService.getMaterialsByCourse(course);

            if (existingMaterials.isEmpty()) {
                return "This is the first material for the course: " + course;
            }

            StringBuilder context = new StringBuilder();
            context.append("Existing materials in course '").append(course).append("':\n");

            for (int i = 0; i < Math.min(existingMaterials.size(), 5); i++) {
                Map<String, Object> material = existingMaterials.get(i);
                context.append("- ").append(material.get("c_course_fileupload"));
                if (material.get("c_course_information") != null) {
                    context.append(" (").append(material.get("c_course_information")).append(")");
                }
                context.append("\n");
            }

            return context.toString();

        } catch (Exception e) {
            LogUtil.error("MaterialEvaluationService", e, "Error getting course context: " + e.getMessage());
            return "Unable to retrieve existing course materials for context.";
        }
    }

    /**
     * Build comprehensive evaluation prompt for AI with enhanced pre-upload support
     */
    private String buildEvaluationPrompt(String course, String description, String filename,
            String fileContent, String courseContext, boolean isPreUpload) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(
                "You are an experienced educational content reviewer tasked with evaluating course materials for quality and suitability. ");
        if (isPreUpload) {
            prompt.append(
                    "This is a PRE-UPLOAD evaluation to help the teacher improve the material before students access it. ");
        }
        prompt.append("Please analyze the submitted material and provide a comprehensive evaluation.\n\n");

        prompt.append("=== MATERIAL DETAILS ===\n");
        prompt.append("Course: ").append(course != null ? course : "Not specified").append("\n");
        prompt.append("Filename: ").append(filename != null ? filename : "Not provided").append("\n");
        prompt.append("Description: ")
                .append(description != null && !description.trim().isEmpty() ? description : "No description provided")
                .append("\n");
        if (isPreUpload) {
            prompt.append("Evaluation Type: Pre-upload analysis (content read from browser)\n");
        }
        prompt.append("\n");

        prompt.append("=== COURSE CONTEXT ===\n");
        prompt.append(courseContext).append("\n\n");

        prompt.append("=== MATERIAL CONTENT ===\n");
        if (fileContent != null && !fileContent.trim().isEmpty() &&
                !fileContent.startsWith("File not found:") && !fileContent.startsWith("Error reading")) {

            if (fileContent.startsWith("[Binary file:")) {
                // Binary file information from browser
                prompt.append("File Information: ").append(fileContent).append("\n");
                prompt.append("Note: Binary file content cannot be fully analyzed in pre-upload mode. ");
                prompt.append("Evaluation based on filename, description, and file metadata.\n\n");
            } else {
                // Text content available
                prompt.append("File Content:\n");
                // Limit content length to avoid token limits
                String limitedContent = fileContent.length() > 3000
                        ? fileContent.substring(0, 3000) + "...[content truncated]"
                        : fileContent;
                prompt.append(limitedContent).append("\n\n");
            }
        } else if (fileContent != null
                && (fileContent.startsWith("File not found:") || fileContent.startsWith("Error reading"))) {
            prompt.append("File Status: ").append(fileContent).append("\n\n");
        } else {
            prompt.append(
                    "File Content: [No file content available - evaluation based on description and filename only]\n\n");
        }

        prompt.append("=== EVALUATION CRITERIA ===\n");
        prompt.append("Please evaluate this material based on:\n");
        prompt.append("1. Educational Value (25%) - Does it provide clear learning outcomes?\n");
        prompt.append("2. Content Quality (25%) - Is the content accurate, well-structured, and comprehensive?\n");
        prompt.append("3. Student Suitability (20%) - Is it appropriate for the target audience?\n");
        prompt.append("4. Clarity & Organization (15%) - Is the content well-organized and easy to understand?\n");
        prompt.append("5. Completeness (15%) - Does it cover the topic adequately?\n\n");

        // Enhanced guidance for pre-upload evaluation
        if (isPreUpload) {
            prompt.append("=== PRE-UPLOAD EVALUATION GUIDANCE ===\n");
            prompt.append("Since this is a pre-upload evaluation:\n");
            prompt.append("• Focus on helping the teacher improve the material before students see it\n");
            prompt.append("• Provide specific, actionable suggestions for enhancement\n");
            prompt.append("• Consider the educational context and course objectives\n");
            prompt.append("• Be constructive but thorough in identifying areas for improvement\n\n");
        }

        prompt.append("=== RECOMMENDATION GUIDELINES ===\n");
        prompt.append("• 90-100%: Excellent material, ready for immediate use\n");
        prompt.append("• 80-89%: Good material, minor improvements suggested\n");
        prompt.append("• 70-79%: Average material, some enhancements needed\n");
        prompt.append("• 60-69%: Below average, significant improvements required\n");
        prompt.append("• Below 60%: Poor quality, major revision needed\n\n");

        prompt.append(
                "IMPORTANT: Materials with less than 80% recommendation should be flagged as requiring enhancement before upload.\n\n");

        // Special instructions for IPv6 content (based on the example)
        if (filename != null && filename.toLowerCase().contains("ipv6") ||
                (description != null && description.toLowerCase().contains("ipv6"))) {
            prompt.append("=== NETWORKING CONTENT GUIDANCE ===\n");
            prompt.append("This appears to be networking content about IPv6. Consider:\n");
            prompt.append("• Technical accuracy and current standards\n");
            prompt.append("• Progression from basic concepts to advanced topics\n");
            prompt.append("• Practical examples and real-world applications\n");
            prompt.append("• Comparison with IPv4 where relevant\n");
            prompt.append("• Hands-on exercises or configuration examples\n\n");
        }

        prompt.append("Provide your response in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"recommendationPercentage\": 85,\n");
        prompt.append("  \"overallRating\": \"Good\",\n");
        prompt.append("  \"isRecommended\": true,\n");
        prompt.append("  \"evaluationSummary\": \"Brief summary of the evaluation...\",\n");
        prompt.append("  \"strengths\": [\"List of material strengths\"],\n");
        prompt.append("  \"improvements\": [\"List of suggested improvements\"],\n");
        prompt.append("  \"educationalValue\": 85,\n");
        prompt.append("  \"contentQuality\": 90,\n");
        prompt.append("  \"studentSuitability\": 80,\n");
        prompt.append("  \"clarityOrganization\": 85,\n");
        prompt.append("  \"completeness\": 75,\n");
        prompt.append("  \"recommendations\": \"Specific recommendations for improvement...\"\n");
        prompt.append("}\n\n");

        prompt.append("Be thorough, constructive, and specific in your evaluation. ");
        prompt.append("Focus on how this material will benefit students and what could make it even better.");

        return prompt.toString();
    }

    /**
     * Parse AI evaluation response
     */
    private EvaluationResult parseEvaluationResponse(String aiResponse, String course, String filename) {
        try {
            // Try to extract JSON from the response
            String jsonStr = extractJSON(aiResponse);

            if (jsonStr != null) {
                Map<String, Object> responseMap = gson.fromJson(jsonStr, Map.class);

                EvaluationResult result = new EvaluationResult();
                result.setCourse(course);
                result.setFilename(filename);
                result.setRecommendationPercentage(((Number) responseMap.get("recommendationPercentage")).intValue());
                result.setOverallRating((String) responseMap.get("overallRating"));
                result.setRecommended((Boolean) responseMap.get("isRecommended"));
                result.setEvaluationSummary((String) responseMap.get("evaluationSummary"));
                result.setRecommendations((String) responseMap.get("recommendations"));
                result.setTimestamp(new java.util.Date());

                // Detailed scores
                if (responseMap.get("educationalValue") != null) {
                    result.setEducationalValue(((Number) responseMap.get("educationalValue")).intValue());
                }
                if (responseMap.get("contentQuality") != null) {
                    result.setContentQuality(((Number) responseMap.get("contentQuality")).intValue());
                }
                if (responseMap.get("studentSuitability") != null) {
                    result.setStudentSuitability(((Number) responseMap.get("studentSuitability")).intValue());
                }
                if (responseMap.get("clarityOrganization") != null) {
                    result.setClarityOrganization(((Number) responseMap.get("clarityOrganization")).intValue());
                }
                if (responseMap.get("completeness") != null) {
                    result.setCompleteness(((Number) responseMap.get("completeness")).intValue());
                }

                @SuppressWarnings("unchecked")
                List<String> strengths = (List<String>) responseMap.get("strengths");
                @SuppressWarnings("unchecked")
                List<String> improvements = (List<String>) responseMap.get("improvements");

                result.setStrengths(strengths);
                result.setImprovements(improvements);

                return result;
            }

        } catch (Exception e) {
            LogUtil.warn("MaterialEvaluationService", "Failed to parse JSON response: " + e.getMessage());
        }

        // Fallback: create result from raw response
        EvaluationResult result = new EvaluationResult();
        result.setCourse(course);
        result.setFilename(filename);
        result.setRecommendationPercentage(75); // Default moderate score
        result.setOverallRating("Average");
        result.setRecommended(false); // Conservative default
        result.setEvaluationSummary("AI Analysis: " + aiResponse);
        result.setRecommendations("Please review the material manually for quality assessment.");
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
     * EvaluationResult class for material assessment
     */
    public static class EvaluationResult {
        private String course;
        private String filename;
        private int recommendationPercentage;
        private String overallRating;
        private boolean isRecommended;
        private String evaluationSummary;
        private String recommendations;
        private List<String> strengths;
        private List<String> improvements;
        private java.util.Date timestamp;

        // Detailed evaluation scores
        private int educationalValue;
        private int contentQuality;
        private int studentSuitability;
        private int clarityOrganization;
        private int completeness;

        // Getters and setters
        public String getCourse() {
            return course;
        }

        public void setCourse(String course) {
            this.course = course;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public int getRecommendationPercentage() {
            return recommendationPercentage;
        }

        public void setRecommendationPercentage(int recommendationPercentage) {
            this.recommendationPercentage = recommendationPercentage;
        }

        public String getOverallRating() {
            return overallRating;
        }

        public void setOverallRating(String overallRating) {
            this.overallRating = overallRating;
        }

        public boolean isRecommended() {
            return isRecommended;
        }

        public void setRecommended(boolean recommended) {
            isRecommended = recommended;
        }

        public String getEvaluationSummary() {
            return evaluationSummary;
        }

        public void setEvaluationSummary(String evaluationSummary) {
            this.evaluationSummary = evaluationSummary;
        }

        public String getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(String recommendations) {
            this.recommendations = recommendations;
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

        public java.util.Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(java.util.Date timestamp) {
            this.timestamp = timestamp;
        }

        public int getEducationalValue() {
            return educationalValue;
        }

        public void setEducationalValue(int educationalValue) {
            this.educationalValue = educationalValue;
        }

        public int getContentQuality() {
            return contentQuality;
        }

        public void setContentQuality(int contentQuality) {
            this.contentQuality = contentQuality;
        }

        public int getStudentSuitability() {
            return studentSuitability;
        }

        public void setStudentSuitability(int studentSuitability) {
            this.studentSuitability = studentSuitability;
        }

        public int getClarityOrganization() {
            return clarityOrganization;
        }

        public void setClarityOrganization(int clarityOrganization) {
            this.clarityOrganization = clarityOrganization;
        }

        public int getCompleteness() {
            return completeness;
        }

        public void setCompleteness(int completeness) {
            this.completeness = completeness;
        }
    }
}