package com.Babble.Translation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LMStudioClient implements ApiClient {
    private final HttpClient httpClient;
    private final String baseUrl;

    public LMStudioClient(String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(2))
            .build()
        ;
        this.baseUrl = baseUrl;
    }

    @Override
    public java.util.Map<String, String> translateBatch(
        java.util.Map<String, String> texts,
        String sourceLang,
        String targetLang,
        String model,
        String primer
    ) throws IOException, InterruptedException {
        JSONObject inputJson = new JSONObject();
        for (java.util.Map.Entry<String, String> entry : texts.entrySet()) {
            inputJson.put(entry.getKey(), entry.getValue());
        }

        String context = (primer == null || primer.trim().isEmpty()) ? "" : " " + primer;

        // gemini was used here to format the prompt for the llm
        String systemPrompt = String.format(
            "You are a professional translator. Translate the values in the provided JSON object from %s to %s.%s " +
            "Return ONLY a valid JSON object where the keys are identical to the input and the values are the translations. " +
            "Do not add any Markdown formatting, no code blocks, no explanations. " +
            "If a value has multiple meanings, pick the best one in context. " +
            "Keep the structure exactly: {\"key1\": \"translated_value1\", ...}",
        sourceLang, targetLang, context);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", systemPrompt + "\n\nInput JSON:\n" + inputJson);

        JSONArray messages = new JSONArray();
        messages.put(message);

        JSONObject requestBody = new JSONObject();
        requestBody.put("messages", messages);
        requestBody.put("model", model);
        requestBody.put("temperature", 0.7);
        requestBody.put("top_p", 0.95);
        requestBody.put("repeat_penalty", 1.0);
        requestBody.put("stream", false);

        String finalUrl = (
            baseUrl.endsWith("/")?
                baseUrl
                :
                baseUrl + "/"
        ) + "v1/chat/completions";

        System.out.println("Batch Request Body: " + requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(finalUrl))
            .version(HttpClient.Version.HTTP_1_1)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .timeout(java.time.Duration.ofSeconds(90)) // so I can test with 24b param models
            .build()
        ;

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            String content = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            ;
            System.out.println("Batch Response Raw: " + content);

            // remove markdown code blocks if the LLM adds them
            String cleanedJson = content.trim();
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7);
            } else if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substring(3);
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim();

            java.util.Map<String, String> results = new java.util.HashMap<>();
            try {
                JSONObject resultObj = new JSONObject(cleanedJson);
                for (String key : resultObj.keySet()) {
                    results.put(key, resultObj.getString(key));
                }
            } catch (Exception e) {
                System.err.println("Failed to parse batch JSON response: " + e.getMessage());
            }
            return results;
        } else {
            throw new IOException("API batch request failed with status code: " + response.statusCode());
        }
    }
}
