package com.Babble.OCR;

import com.Babble.ImageUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class VisionClient {
    private final HttpClient httpClient;
    private final String baseUrl;

    public VisionClient(String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build()
        ;
        this.baseUrl = baseUrl;
    }

    public String analyzeImage(BufferedImage image, String prompt, String model) throws IOException, InterruptedException {
        String base64Image = ImageUtils.imageToBase64(image);
        JSONObject requestBody = getJsonObject(prompt, model, base64Image);

        String finalUrl = (baseUrl.endsWith("/")?
            baseUrl
            :
            baseUrl + "/"
        ) + "v1/chat/completions";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(finalUrl))
            .version(HttpClient.Version.HTTP_1_1)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .timeout(java.time.Duration.ofSeconds(600))
            .build()
        ;

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8)
        );

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            if (jsonResponse.has("choices") && !jsonResponse.getJSONArray("choices").isEmpty()) {
                return jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                ;
            }
        }
        throw new IOException("Vision API request failed: " + response.statusCode() + " " + response.body());
    }

    private static JSONObject getJsonObject(String prompt, String model, String base64Image) throws IOException {
        if (base64Image.isEmpty())
            throw new IOException("Failed to convert image to Base64");

        JSONObject textContent = new JSONObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/png;base64," + base64Image);
        imageContent.put("image_url", imageUrl);

        JSONArray contentArray = new JSONArray();
        contentArray.put(textContent);
        contentArray.put(imageContent);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", contentArray);

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are a professional OCR engine. Your task is to extract text from the image as requested by the user. Do not include markdown formatting unless requested."
        );
        messages.put(systemMessage);

        messages.put(message);

        JSONObject requestBody = new JSONObject();
        requestBody.put("messages", messages);
        requestBody.put("model", model);
        requestBody.put("temperature", 0.1);
        requestBody.put("top_p", 0.9);
        requestBody.put("repeat_penalty", 1.15);
        requestBody.put("max_tokens", 800);
        requestBody.put("stream", false);
        return requestBody;
    }
}
