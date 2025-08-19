package org.joget.gemini;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joget.commons.util.LogUtil;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class GeminiService {

    private final String apiKey;
    private final String baseUrl = "https://generativelanguage.googleapis.com/v1/models/";
    private final Gson gson;

    public GeminiService(String apiKey) {
        this.apiKey = apiKey;
        this.gson = new Gson();
    }

    /**
     * Generate content using Gemini API
     */
    public String generateContent(String model, String prompt, Map<String, Object> parameters) throws IOException {
        LogUtil.info("GeminiService", "Generating content with model: " + model);

        String url = baseUrl + model + ":generateContent?key=" + apiKey;
        LogUtil.info("GeminiService", "API URL: " + url);

        // Build request body
        JsonObject requestBody = new JsonObject();

        // Add generation config
        JsonObject generationConfig = new JsonObject();
        if (parameters != null) {
            LogUtil.info("GeminiService", "Adding parameters: " + parameters.toString());
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Double) {
                    generationConfig.addProperty(key, (Double) value);
                } else if (value instanceof Integer) {
                    generationConfig.addProperty(key, (Integer) value);
                } else if (value instanceof String) {
                    generationConfig.addProperty(key, (String) value);
                } else if (value instanceof Float) {
                    generationConfig.addProperty(key, (Float) value);
                }
            }
        }
        requestBody.add("generationConfig", generationConfig);

        // Add contents
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        LogUtil.info("GeminiService", "Request body: " + gson.toJson(requestBody));

        // Make HTTP request
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(gson.toJson(requestBody), "UTF-8"));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity, "UTF-8");

                int statusCode = response.getStatusLine().getStatusCode();
                LogUtil.info("GeminiService", "Response status: " + statusCode);
                LogUtil.info("GeminiService", "Response body: " + responseString);

                if (statusCode == 200) {
                    String parsedResponse = parseResponse(responseString);
                    LogUtil.info("GeminiService", "Parsed response: "
                            + parsedResponse.substring(0, Math.min(100, parsedResponse.length())) + "...");
                    return parsedResponse;
                } else {
                    LogUtil.error("GeminiService", null,
                            "API call failed with status " + statusCode + ": " + responseString);
                    throw new RuntimeException("API call failed (HTTP " + statusCode + "): " + responseString);
                }
            }
        } catch (IOException e) {
            LogUtil.error("GeminiService", e, "IOException during API call: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            LogUtil.error("GeminiService", e, "Unexpected error during API call: " + e.getMessage());
            throw new RuntimeException("Unexpected error during API call: " + e.getMessage(), e);
        }
    }

    /**
     * Parse the response from Gemini API
     */
    private String parseResponse(String responseString) {
        try {
            JsonObject responseJson = gson.fromJson(responseString, JsonObject.class);

            if (responseJson.has("candidates")) {
                JsonArray candidates = responseJson.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                JsonObject part = parts.get(0).getAsJsonObject();
                                if (part.has("text")) {
                                    return part.get("text").getAsString();
                                }
                            }
                        }
                    }
                }
            }

            // Check for error in response
            if (responseJson.has("error")) {
                JsonObject error = responseJson.getAsJsonObject("error");
                String errorMessage = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                LogUtil.error("GeminiService", null, "API returned error: " + errorMessage);
                return "API Error: " + errorMessage;
            }

            LogUtil.warn("GeminiService", "No valid content found in response: " + responseString);
            return "No response generated";

        } catch (Exception e) {
            LogUtil.error("GeminiService", e, "Error parsing response: " + e.getMessage());
            return "Error parsing response: " + e.getMessage();
        }
    }

    /**
     * Test the API connection with a simple request
     */
    public boolean testConnection() {
        LogUtil.info("GeminiService", "Testing Gemini API connection...");

        try {
            // Use a simple test prompt
            Map<String, Object> testParams = new HashMap<>();
            testParams.put("temperature", 0.1);
            testParams.put("maxOutputTokens", 50);

            String testResponse = generateContent("gemini-1.5-flash",
                    "Hello, this is a connection test. Please respond with 'Connection successful'.", testParams);

            boolean isSuccess = testResponse != null &&
                    !testResponse.trim().isEmpty() &&
                    !testResponse.startsWith("API Error:") &&
                    !testResponse.startsWith("Error parsing");

            if (isSuccess) {
                LogUtil.info("GeminiService", "✅ API connection test successful! Response: " + testResponse);
            } else {
                LogUtil.warn("GeminiService", "⚠️ API connection test failed. Response: " + testResponse);
            }

            return isSuccess;

        } catch (Exception e) {
            LogUtil.error("GeminiService", e, "❌ API connection test failed with exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Test the API connection with detailed diagnostics
     */
    public Map<String, Object> testConnectionDetailed() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());

        try {
            LogUtil.info("GeminiService", "Running detailed API connection test...");

            // Test parameters
            Map<String, Object> testParams = new HashMap<>();
            testParams.put("temperature", 0.1);
            testParams.put("maxOutputTokens", 100);

            long startTime = System.currentTimeMillis();
            String testResponse = generateContent("gemini-1.5-flash",
                    "Test connection. Respond with: API is working correctly.", testParams);
            long endTime = System.currentTimeMillis();

            result.put("responseTime", endTime - startTime);
            result.put("response", testResponse);

            boolean isSuccess = testResponse != null &&
                    !testResponse.trim().isEmpty() &&
                    !testResponse.startsWith("API Error:") &&
                    !testResponse.startsWith("Error parsing");

            result.put("success", isSuccess);
            result.put("status", isSuccess ? "connected" : "failed");

            if (isSuccess) {
                result.put("message", "API connection successful");
                LogUtil.info("GeminiService", "✅ Detailed API test successful in " + (endTime - startTime) + "ms");
            } else {
                result.put("message", "API connection failed: " + testResponse);
                LogUtil.warn("GeminiService", "⚠️ Detailed API test failed: " + testResponse);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("status", "error");
            result.put("message", "Exception during test: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            LogUtil.error("GeminiService", e, "❌ Detailed API test failed with exception: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get API key status (masked for security)
     */
    public String getApiKeyStatus() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "not_configured";
        } else if ("YOUR_API_KEY_HERE".equals(apiKey)) {
            return "placeholder";
        } else if (apiKey.length() < 10) {
            return "invalid_length";
        } else {
            // Return masked version for security
            return "configured (" + apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4) + ")";
        }
    }

    /**
     * Generate content with retry mechanism
     */
    public String generateContentWithRetry(String model, String prompt, Map<String, Object> parameters, int maxRetries)
            throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                LogUtil.info("GeminiService", "Attempt " + attempt + " of " + maxRetries);
                return generateContent(model, prompt, parameters);

            } catch (IOException e) {
                lastException = e;
                LogUtil.warn("GeminiService", "Attempt " + attempt + " failed: " + e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Wait before retry (exponential backoff)
                        long waitTime = 1000 * attempt;
                        LogUtil.info("GeminiService", "Waiting " + waitTime + "ms before retry...");
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry wait", ie);
                    }
                }
            }
        }

        // All attempts failed
        throw new IOException("All " + maxRetries + " attempts failed. Last error: " + lastException.getMessage(),
                lastException);
    }
}