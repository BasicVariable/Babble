package com.Babble.OCR;

import com.Babble.Processing.TranslationResult;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;

public class HybridOCRService implements OCRService {
    private final PaddleOCRService paddleService;
    private final VisionClient visionClient;

    public HybridOCRService(PaddleOCRService paddleService, VisionClient visionClient) {
        this.paddleService = paddleService;
        this.visionClient = visionClient;
    }

    private BufferedImage createCollageImage(BufferedImage src, List<int[]> boxes) {
        int
            padding = 10,
            labelWidth = 50,
            totalH = 0,
            maxW = 0
        ;

        List<BufferedImage> crops = new ArrayList<>();
        for (int[] box : boxes) {
            int
                x = box[0], y = box[1], w = box[2], h = box[3],
                sx = Math.max(0, x - 5),
                sy = Math.max(0, y - 5),
                sw = Math.min(src.getWidth(), x + w + 5) - sx,
                sh = Math.min(src.getHeight(), y + h + 5) - sy
            ;
            if (sw <= 0 || sh <= 0) continue;

            BufferedImage crop = src.getSubimage(sx, sy, sw, sh);

            // acc for small crops
            if (crop.getHeight() < 48) {
                double scale = 48.0 / crop.getHeight();
                int scaledW = (int) (crop.getWidth() * scale);
                int scaledH = 48;
                crop = com.Babble.ImageUtils.resize(crop, scaledW, scaledH);
            }

            crops.add(crop);

            totalH += crop.getHeight() + padding;
            if (crop.getWidth() > maxW) {
                maxW = crop.getWidth();
            }
        }

        int
            collageW = maxW + labelWidth + padding,
            collageH = totalH + padding
        ;

        BufferedImage collage = new BufferedImage(collageW, collageH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = collage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, collageW, collageH);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));

        int currentY = padding;
        for (int i = 0; i < crops.size(); i++) {
            BufferedImage crop = crops.get(i);

            g.drawString(String.valueOf(i), 10, currentY + (crop.getHeight() / 2) + 8);
            g.drawImage(crop, labelWidth, currentY, null);

            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(
                0,
                currentY + crop.getHeight() + (padding / 2), collageW,
                currentY + crop.getHeight() + (padding / 2)
            );
            g.setColor(Color.BLACK);

            currentY += crop.getHeight() + padding;
        }
        g.dispose();

        return collage;
    }

    @Override
    public List<TranslationResult> performOCR(BufferedImage image, String targetLang, String model) {
        List<TranslationResult> results = new ArrayList<>();

        try {
            System.out.println("Hybrid - Running Paddle Detection...");
            List<int[]> boxes = paddleService.detect(image);
            System.out.println("Hybrid - Found " + boxes.size() + " boxes.");

            if (boxes.isEmpty()) return results;

            /*
            boxes = mergeVerticalBoxes(boxes);
            System.out.println("Hybrid - Merged into " + boxes.size() + " boxes.");
             */

            // collages are better
            BufferedImage annotatedImage = createCollageImage(image, boxes);

            /* testing
                try {
                    javax.imageio.ImageIO.write(annotatedImage, "png", new java.io.File("debug_annotated.png"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
             */

            System.out.println("Hybrid - Sending Annotated Image to vision LLM");

            String prompt =
                "Transcribe the Japanese text in this collage.\n" +
                "Each text block is numbered on the left (e.g. 0, 1, 2).\n" +
                "Instructions:\n" +
                "- Transcribe the text corresponding to each number EXACTLY.\n" +
                "- Do NOT translate to English. Keep valid Japanese.\n" +
                "- Return a STRICT JSON object mapping ID to Text.\n" +
                "- Example: {\"0\": \"戦い方のヒント\", \"1\": \"清涼のバレル\"}\n" +
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
