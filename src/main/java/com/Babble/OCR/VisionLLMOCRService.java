package com.Babble.OCR;

import com.Babble.Processing.TranslationResult;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class VisionLLMOCRService implements OCRService {
    private final VisionClient visionClient;

    public VisionLLMOCRService(VisionClient client) {
        this.visionClient = client;
    }

    public VisionClient getClient() {
        return visionClient;
    }

    // vision shouldn't be outputting json, makes worse results
    @Override
    public List<TranslationResult> performOCR(BufferedImage image, String targetLang, String model) {
        List<TranslationResult> results = new ArrayList<>();

        String prompt =
            "Analyze this game UI. Transcribe all visible text in the image. \n" +
            "Order text blocks from Right-to-Left, Top-to-Bottom. \n" +
            "Output ONLY the transcribed text. Do not provide bounding boxes or JSON.\n" +
            "Do NOT translate. Output the text in its original language (e.g. Japanese)."
        ;

        try {
            String rawResponse = visionClient.analyzeImage(image, prompt, model);
            System.out.println("Raw Vision Response: " + rawResponse);

            String text = rawResponse.replaceAll("(?s)<think>.*?</think>", "").trim();
            // PLEASE  stop adding think tags
            if (text.contains("<think>")) {
                text = text.replaceAll("(?s)<think>.*", "").trim();
            }

            if (!text.isEmpty()) {
                results.add(
                    new TranslationResult(
                        text,
                        "",
                        0,
                        0,
                        image.getWidth(),
                        image.getHeight()
                    )
                );
            }

        } catch (Exception e) {
            System.err.println("Vision OCR Failed: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }
}
