package com.Babble.OCR;

import com.Babble.Processing.TranslationResult;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class HybridOCRService implements OCRService {
    private final PaddleOCRService paddleService;
    private final VisionClient visionClient;

    public HybridOCRService(PaddleOCRService paddleService, VisionClient visionClient) {
        this.paddleService = paddleService;
        this.visionClient = visionClient;
    }

    @Override
    public List<TranslationResult> performOCR(BufferedImage image, String targetLang, String model) {
        List<TranslationResult> results = new ArrayList<>();

        try {
            System.out.println("Hybrid - Running Paddle Detection...");
            List<int[]> boxes = paddleService.detect(image);
            System.out.println("Hybrid - Found " + boxes.size() + " boxes.");

            if (boxes.isEmpty()) return results;
            boxes = mergeVerticalBoxes(boxes);
            System.out.println("Hybrid - Merged into " + boxes.size() + " boxes.");

            // all of these llms are soooo dumb (even larger models), so I'm pointing out
            // what I want them to read.
            BufferedImage annotatedImage = createMaskedImage(image, boxes);

            /* testing
                try {
                    javax.imageio.ImageIO.write(annotatedImage, "png", new java.io.File("debug_annotated.png"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
             */

            System.out.println("Hybrid - Sending Annotated Image to vision LLM");

            String prompt =
                "Transcribe the Japanese text inside the Red Boxes.\n" +
                "Instructions:\n" +
                "- Transcribe EXACTLY character-by-character. Do NOT summarize.\n" +
                "- Output Japanese characters. Do NOT translate to English.\n" +
                "- Text is mostly VERTICAL. Read from top to bottom.\n" +
                "- Return a STRICT JSON object mapping ID to Text.\n" +
                "- Example: {\"0\": \"こんにちは\", \"1\": \"テスト\"}\n" +
                "- Do NOT include brackets like [ ] in the output.\n" +
                "- Return ONLY valid JSON."
            ;

            String jsonRaw = visionClient.analyzeImage(annotatedImage, prompt, model);
            System.out.println("Hybrid - Raw Response: " + jsonRaw);
            org.json.JSONObject mappedText = parseResponse(jsonRaw);

            for (int i = 0; i < boxes.size(); i++) {
                String id = String.valueOf(i);
                if (mappedText.has(id)) {
                    String text = mappedText.getString(id).trim();
                    // remove leading punctuation
                    text = text.replaceAll("^[:.\\- ]+", "");

                    if (!text.isEmpty() && !text.equals("[SKIP]")) {
                        int[] box = boxes.get(i);
                        results.add(new TranslationResult(text, "", box[0], box[1], box[2], box[3]));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            results.add(new TranslationResult("Hybrid Error: " + e.getMessage(), "", 10, 10, 200, 50));
        }

        return results;
    }

    private BufferedImage createMaskedImage(BufferedImage src, List<int[]> boxes) {
        int
            padLeft = 60,
            padTop = 40,
            newW = src.getWidth() + padLeft + 40,
            newH = src.getHeight() + (padTop * 2)
        ;

        BufferedImage dest = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = dest.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, newW, newH);

        g.setStroke(new java.awt.BasicStroke(2));
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24)); // Large font for AI readability

        for (int i = 0; i < boxes.size(); i++) {
            int[] box = boxes.get(i);
            int
                x = box[0] + padLeft,
                y = box[1] + padTop,
                w = box[2],
                h = box[3]
            ;

            int
                padding = 40,
                sx = Math.max(0, box[0] - padding),
                sy = Math.max(0, box[1] - padding),
                sw = Math.min(src.getWidth(), box[0] + w + padding) - sx,
                sh = Math.min(src.getHeight(), box[1] + h + padding) - sy
            ;

            g.drawImage(
                src,
                x - (box[0] - sx),
                y - (box[1] - sy),
                x - (box[0] - sx) + sw,
                y - (box[1] - sy) + sh,
                sx,
                sy,
                sx + sw,
                sy + sh,
                null
            );

            g.setColor(java.awt.Color.RED);
            g.setStroke(new java.awt.BasicStroke(3));

            int
                d = 25,
                dx = x - d,
                dy = y - d,
                dw = w + (d * 2),
                dh = h + (d * 2)
            ;
            g.drawRect(dx, dy, dw, dh);

            int idX = dx - 35;
            if (idX < 5) idX = 5;

            int midY = y + (h / 2);
            g.setStroke(new java.awt.BasicStroke(2));
            g.setColor(java.awt.Color.RED);
            int textWidth = 15;
            g.drawLine(idX + textWidth, midY, dx, midY);

            String id = String.valueOf(i);
            g.setColor(java.awt.Color.RED);
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
            g.drawString(id, idX, midY + 8);
        }
        g.dispose();
        return dest;
    }

    private org.json.JSONObject parseResponse(String raw) {
        if (raw == null) return new org.json.JSONObject();

        String jsonString = raw.trim();
        if (jsonString.contains("{")) {
            int start = jsonString.indexOf("{");
            int end = jsonString.lastIndexOf("}");
            if (end > start) {
                jsonString = jsonString.substring(start, end + 1);
            }
        }

        try {
            return new org.json.JSONObject(jsonString);
        } catch (Exception e) {
            System.err.println("Hybrid JSON Parse Error: " + e.getMessage());
            return new org.json.JSONObject();
        }
    }

    private List<int[]> mergeVerticalBoxes(List<int[]> raw) {
        if (raw.isEmpty()) return raw;

        List<int[]> boxes = new ArrayList<>(raw);
        boxes.sort((a, b) -> {
            int cxA = a[0] + a[2] / 2;
            int cxB = b[0] + b[2] / 2;
            int xDiff = Math.abs(cxA - cxB);

            if (xDiff < 30) { // x tolerance
                return Integer.compare(a[1], b[1]);
            }
            return Integer.compare(cxA, cxB);
        });

        List<int[]> grouped = new ArrayList<>();
        int[] current = boxes.get(0);

        for (int i = 1; i < boxes.size(); i++) {
            int[] next = boxes.get(i);

            int
                c1 = current[0] + (current[2] / 2),
                c2 = next[0] + (next[2] / 2)
            ;
            boolean centerAligned = Math.abs(c1 - c2) < 30;

            int
                currentBottom = current[1] + current[3],
                vGap = next[1] - currentBottom
            ;
            boolean closeVertically = vGap < 80 && vGap > -20;
            boolean similarWidth = Math.abs(current[2] - next[2]) < (Math.max(current[2], next[2]) * 0.7);

            if (centerAligned && closeVertically && similarWidth) {
                int
                    minX = Math.min(current[0], next[0]),
                    minY = Math.min(current[1], next[1]),
                    maxX = Math.max(current[0] + current[2], next[0] + next[2]),
                    maxY = Math.max(current[1] + current[3], next[1] + next[3])
                ;

                current = new int[] { minX, minY, maxX - minX, maxY - minY };
            } else {
                grouped.add(current);
                current = next;
            }
        }
        grouped.add(current);
        return grouped;
    }
}
