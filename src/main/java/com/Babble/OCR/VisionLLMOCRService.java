package com.Babble.OCR;

import com.Babble.Processing.TranslationResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisionLLMOCRService implements OCRService {
    private final VisionClient visionClient;

    public VisionLLMOCRService(VisionClient client) {
        this.visionClient = client;
    }

    public VisionClient getClient() {
        return visionClient;
    }

    @Override
    public List<TranslationResult> performOCR(BufferedImage image, String targetLang, String model) {
        List<TranslationResult> results = new ArrayList<>();

        // prompt optimized with qwen
        String prompt =
            "Analyze this game UI. Find all visible text.\n" +
            "Return a raw JSON object with a 'regions' key. \n" +
            "Each region must have:\n" +
            "1. 'text': The exact text content.\n" +
            "2. 'box': The bounding box as [ymin, xmin, ymax, xmax] normalized to 0-1000.\n" +
            "Example: {\"regions\": [{\"text\": \"HP\", \"box\": [100, 100, 150, 300]}]}\n" +
            "Do not use markdown formatting. Return only JSON."
        ;

        try {
            String jsonRaw = visionClient.analyzeImage(image, prompt, model);
            System.out.println("Raw Vision Response: " + jsonRaw);

            String cleanedJson = extractJson(jsonRaw);
            System.out.println("Cleaned JSON: " + cleanedJson);

            JSONObject root = new JSONObject(cleanedJson);
            if (root.has("regions")) {
                JSONArray regions = root.getJSONArray("regions");
                int imgW = image.getWidth(),
                        imgH = image.getHeight();

                for (int i = 0; i < regions.length(); i++) {
                    JSONObject region = regions.getJSONObject(i);
                    String text = region.optString("text", "").trim();
                    if (text.isEmpty()) continue;

                    JSONArray box = region.optJSONArray("box");
                    int x = 0, y = 0, w = 0, h = 0;

                    if (box != null && box.length() >= 4) {
                        try {
                            double
                                n_xmin = box.optDouble(0, 0) / 1000.0,
                                n_ymin = box.optDouble(1, 0) / 1000.0,
                                n_xmax = box.optDouble(2, 0) / 1000.0,
                                n_ymax = box.optDouble(3, 0) / 1000.0
                            ;

                            // Handle cases where model treats empty/invalid as 0 or fails to parse
                            if (Double.isNaN(n_xmin)) n_xmin = 0;
                            if (Double.isNaN(n_ymin)) n_ymin = 0;
                            if (Double.isNaN(n_xmax)) n_xmax = 0;
                            if (Double.isNaN(n_ymax)) n_ymax = 0;

                            int
                                pxMin = (int) (n_xmin * imgW),
                                pyMin = (int) (n_ymin * imgH),
                                pxMax = (int) (n_xmax * imgW),
                                pyMax = (int) (n_ymax * imgH)
                            ;

                            x = pxMin;
                            y = pyMin;
                            w = Math.max(1, pxMax - pxMin);
                            h = Math.max(1, pyMax - pyMin);
                        } catch (Exception e) {
                            System.err.println("Failed to parse box for text '" + text + "': " + e.getMessage());
                        }
                    }

                    results.add(new TranslationResult(text, "", x, y, w, h));
                }
            }
        } catch (Exception e) {
            System.err.println("Vision OCR Failed: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private String extractJson(String raw) {
        Pattern boxPattern = Pattern.compile("<box>(.*?)</box>");
        Matcher matcher = boxPattern.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String content = matcher.group(1);
            matcher.appendReplacement(sb, "[" + content + "]");
        }
        matcher.appendTail(sb);
        raw = sb.toString();

        raw = raw.trim();
        int
            start = raw.indexOf('{'),
            end = raw.lastIndexOf('}')
        ;

        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }

        return raw;
    }
}
