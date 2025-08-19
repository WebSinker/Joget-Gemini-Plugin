package org.joget.gemini;

import java.util.*;
import java.util.regex.Pattern;
import org.joget.commons.util.LogUtil;

/**
 * Analyzes user messages to determine intent and extract relevant keywords
 * for database query enhancement
 */
public class ContentAnalyzer {

    // Keywords that indicate course material queries
    private static final String[] MATERIAL_KEYWORDS = {
            "course", "courses", "material", "materials", "content", "lesson", "lessons",
            "tutorial", "tutorials", "study", "learning", "module", "modules",
            "chapter", "chapters", "textbook", "textbooks", "resource", "resources",
            "notes", "lecture", "lectures", "reading", "readings", "document", "documents"
    };

    // Keywords that indicate assignment queries
    private static final String[] ASSIGNMENT_KEYWORDS = {
            "assignment", "assignments", "homework", "task", "tasks", "project", "projects",
            "exercise", "exercises", "quiz", "quizzes", "exam", "exams", "test", "tests",
            "due", "deadline", "submit", "submission", "submissions", "work", "activity"
    };

    // Question patterns that indicate listing/browsing intent
    private static final String[] LIST_PATTERNS = {
            "what.*do.*have", "what.*are.*available", "show.*me", "list.*", "give.*me.*list",
            "what.*courses", "what.*assignments", "what.*materials", "all.*", "any.*"
    };

    // Question patterns that indicate search intent
    private static final String[] SEARCH_PATTERNS = {
            "find.*", "search.*", "look.*for", "about.*", "related.*to", "concerning.*"
    };

    // Question patterns that indicate status/upcoming intent
    private static final String[] STATUS_PATTERNS = {
            "upcoming.*", "due.*", "pending.*", "active.*", "current.*", "next.*",
            "this.*week", "today.*", "tomorrow.*", "soon.*"
    };

    /**
     * Analyze user message and determine content type and search terms
     */
    public static AnalysisResult analyzeMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return new AnalysisResult(ContentType.GENERAL, null, QueryType.GENERAL);
        }

        String message = userMessage.toLowerCase().trim();
        LogUtil.info("ContentAnalyzer", "Analyzing message: " + message);

        // Determine content type
        ContentType contentType = determineContentType(message);

        // Determine query type
        QueryType queryType = determineQueryType(message);

        // Extract search terms
        String searchTerms = extractSearchTerms(message, contentType);

        AnalysisResult result = new AnalysisResult(contentType, searchTerms, queryType);

        LogUtil.info("ContentAnalyzer", "Analysis result: " + result.toString());
        return result;
    }

    /**
     * Determine if the message is about materials, assignments, or general
     */
    private static ContentType determineContentType(String message) {
        int materialScore = 0;
        int assignmentScore = 0;

        // Count material keywords
        for (String keyword : MATERIAL_KEYWORDS) {
            if (message.contains(keyword)) {
                materialScore++;
            }
        }

        // Count assignment keywords
        for (String keyword : ASSIGNMENT_KEYWORDS) {
            if (message.contains(keyword)) {
                assignmentScore++;
            }
        }

        // Return the type with higher score
        if (materialScore > assignmentScore && materialScore > 0) {
            return ContentType.MATERIALS;
        } else if (assignmentScore > materialScore && assignmentScore > 0) {
            return ContentType.ASSIGNMENTS;
        } else if (materialScore > 0 || assignmentScore > 0) {
            // If tied or both present, check for more specific indicators
            if (message.contains("due") || message.contains("submit") || message.contains("homework")) {
                return ContentType.ASSIGNMENTS;
            } else if (message.contains("study") || message.contains("learn") || message.contains("read")) {
                return ContentType.MATERIALS;
            }
        }

        return ContentType.GENERAL;
    }

    /**
     * Determine the type of query (list, search, status, etc.)
     */
    private static QueryType determineQueryType(String message) {
        // Check for status/upcoming patterns first
        for (String pattern : STATUS_PATTERNS) {
            if (Pattern.compile(pattern).matcher(message).find()) {
                return QueryType.STATUS;
            }
        }

        // Check for list patterns
        for (String pattern : LIST_PATTERNS) {
            if (Pattern.compile(pattern).matcher(message).find()) {
                return QueryType.LIST;
            }
        }

        // Check for search patterns
        for (String pattern : SEARCH_PATTERNS) {
            if (Pattern.compile(pattern).matcher(message).find()) {
                return QueryType.SEARCH;
            }
        }

        return QueryType.GENERAL;
    }

    /**
     * Extract search terms from the message
     */
    private static String extractSearchTerms(String message, ContentType contentType) {
        // Remove common stop words and question words
        String[] stopWords = {
                "what", "where", "when", "how", "why", "who", "which", "are", "is", "do", "does",
                "can", "could", "would", "should", "will", "the", "a", "an", "and", "or", "but",
                "in", "on", "at", "to", "for", "of", "with", "by", "about", "we", "have", "actually"
        };

        String[] words = message.split("\\s+");
        List<String> meaningfulWords = new ArrayList<>();

        for (String word : words) {
            word = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

            if (word.length() > 2 && !Arrays.asList(stopWords).contains(word)) {
                // Include the word if it's not a common keyword we already know about
                if (contentType == ContentType.MATERIALS && !Arrays.asList(MATERIAL_KEYWORDS).contains(word)) {
                    meaningfulWords.add(word);
                } else if (contentType == ContentType.ASSIGNMENTS
                        && !Arrays.asList(ASSIGNMENT_KEYWORDS).contains(word)) {
                    meaningfulWords.add(word);
                } else if (contentType == ContentType.GENERAL) {
                    meaningfulWords.add(word);
                }
            }
        }

        // Join meaningful words
        String searchTerms = String.join(" ", meaningfulWords);

        // If no meaningful terms found, return null
        return searchTerms.trim().isEmpty() ? null : searchTerms.trim();
    }

    /**
     * Content types that can be detected
     */
    public enum ContentType {
        MATERIALS,
        ASSIGNMENTS,
        GENERAL
    }

    /**
     * Query types that can be detected
     */
    public enum QueryType {
        LIST, // "what courses do we have?"
        SEARCH, // "find courses about java"
        STATUS, // "upcoming assignments"
        GENERAL // general questions
    }

    /**
     * Result of content analysis
     */
    public static class AnalysisResult {
        private final ContentType contentType;
        private final String searchTerms;
        private final QueryType queryType;

        public AnalysisResult(ContentType contentType, String searchTerms, QueryType queryType) {
            this.contentType = contentType;
            this.searchTerms = searchTerms;
            this.queryType = queryType;
        }

        public ContentType getContentType() {
            return contentType;
        }

        public String getSearchTerms() {
            return searchTerms;
        }

        public QueryType getQueryType() {
            return queryType;
        }

        public boolean needsDatabaseData() {
            return contentType != ContentType.GENERAL;
        }

        @Override
        public String toString() {
            return String.format("AnalysisResult{contentType=%s, searchTerms='%s', queryType=%s}",
                    contentType, searchTerms, queryType);
        }
    }

    /**
     * Test the analyzer with sample messages
     */
    public static void testAnalyzer() {
        String[] testMessages = {
                "what course do we actually have?",
                "show me all assignments",
                "find materials about java programming",
                "what assignments are due this week?",
                "list all available courses",
                "search for homework about databases",
                "upcoming projects",
                "hello how are you?",
                "what materials can I study?",
                "do we have any programming assignments?"
        };

        LogUtil.info("ContentAnalyzer", "=== TESTING CONTENT ANALYZER ===");

        for (String message : testMessages) {
            AnalysisResult result = analyzeMessage(message);
            LogUtil.info("ContentAnalyzer",
                    String.format("'%s' -> %s", message, result.toString()));
        }
    }
}