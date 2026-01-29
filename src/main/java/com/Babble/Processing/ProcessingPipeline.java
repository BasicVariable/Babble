package com.Babble.Processing;

import com.Babble.ImageUtils;
import com.Babble.OCR.HybridOCRService;
import com.Babble.OCR.OCRMode;
import com.Babble.OCR.OCRService;
import com.Babble.OCR.PaddleOCRService;
import com.Babble.OCR.TesseractOCRService;
import com.Babble.OCR.VisionClient;
import com.Babble.OCR.VisionLLMOCRService;
import com.Babble.Translation.ApiClient;
import javafx.scene.layout.Pane;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessingPipeline {
    private boolean scanning = false;

    private final ScreenCapturer screenCapturer;
    private final TextGrouper textGrouper;

    private OCRService ocrService;
    private TesseractOCRService tesseractService;
    private VisionLLMOCRService visionService;
    private PaddleOCRService paddleService;
    private HybridOCRService hybridService;

    private final ApiClient apiClient;
    private final TranslationCallback callback;

    private String currentLang;
    private String currentModel = "local-model";
    private String currentVisionModel = "huihui-minicpm-v-4_5-abliterated";
    private OCRMode currentOCRMode = OCRMode.DOCUMENT_TESSERACT;
    private String translationPrimer = "";

    public ProcessingPipeline(
        String dataPath,
        String language,
        Pane transparentPane,
        ApiClient apiClient,
        TranslationCallback callback
    ) {
        System.setProperty("jna.encoding", "UTF-8");
        System.setProperty("file.encoding", "UTF-8");

        this.currentLang = language;
        this.apiClient = apiClient;
        this.callback = callback;

        this.screenCapturer = new ScreenCapturer(transparentPane);
        this.textGrouper = new TextGrouper();

        this.tesseractService = new TesseractOCRService(dataPath, language);
        this.visionService = new VisionLLMOCRService(new VisionClient("http://localhost:1234/"));
        this.paddleService = new PaddleOCRService();
        this.hybridService = new HybridOCRService(this.paddleService, this.visionService.getClient());

        this.ocrService = this.tesseractService;
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
    }

    public boolean isScanning() {
        return scanning;
    }

    // need later
    public void changeLang(String lang) {
        this.currentLang = lang;
        if (tesseractService != null) {
            tesseractService.setLanguage(lang);
        }
    }

    public void setOCRMode(OCRMode mode) {
        this.currentOCRMode = mode;
        switch (mode) {
            case GAME_VISION_LLM -> this.ocrService = visionService;
            case GAME_PADDLE_OCR -> this.ocrService = paddleService;
            case GAME_HYBRID -> this.ocrService = hybridService;
            case DOCUMENT_TESSERACT -> this.ocrService = tesseractService; // Default
            default -> this.ocrService = tesseractService;
        }
    }

    public void setModel(String model) {
        this.currentModel = model;
    }

    public void setVisionModel(String visionModel) {
        this.currentVisionModel = visionModel;
    }

    public void setPrimer(String primer) {
        this.translationPrimer = primer;
    }

    public void processLoopLogic() {
        processLoop(false);
    }

    public void processLoopLogic(boolean force) {
        processLoop(force);
    }

    private void processLoop(boolean force) {
        if (!scanning)
            return;

        try {
            if (callback != null) {
                callback.setOverlayVisible(false);
                try {
                    // there has to be a better way to wait for overlay elements to leave the screen....
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }

            BufferedImage capture = screenCapturer.captureScreen();

            if (callback != null) {
                callback.setOverlayVisible(true);
            }

            if (capture == null) {
                System.out.println("captureScreen returned null???");
                return;
            }

            if (!force && screenCapturer.hasLastCapture()) {
                double imageDiff = screenCapturer.imageDiffPercent(capture);
                System.out.println("Image diff = " + imageDiff + "% (threshold: 1.0%)");
                if (imageDiff < 1.0) {
                    System.out.println("Skipping - screen unchanged");
                    return;
                }
                System.out.println("Processing - screen changed!");
            } else {
                if (force) {
                    System.out.println("Forced Scan (Manual Mode)");
                } else {
                    System.out.println("First scan - no previous capture to compare");
                }
            }

            screenCapturer.updateLastCapture(capture);

            if (callback != null) {
                callback.onProcessingStart();
            }

            BufferedImage upscaledVer;
            if (currentOCRMode == OCRMode.GAME_VISION_LLM || currentOCRMode == OCRMode.GAME_HYBRID) {
                // Vision models (minicpm cough cough) hallucinate heavily with EXTREME upscaling, but need some size for small crops
                if (capture.getWidth() < 600 || capture.getHeight() < 600) {
                    upscaledVer = ImageUtils.upscaleImage(capture);
                    System.out.println(
                        "Upscaling small Vision input: " +
                        capture.getWidth() +
                        "x" +
                        capture.getHeight() +
                        " -> " +
                        upscaledVer.getWidth() +
                        "x" +
                        upscaledVer.getHeight()
                    );
                } else {
                    upscaledVer = capture;
                }

                /* for testing
                    try {
                        javax.imageio.ImageIO.write(upscaledVer, "png", new java.io.File("debug_vision_input.png"));
                    } catch (java.io.IOException ignored) {
                    }
                 */
            } else {
                upscaledVer = ImageUtils.upscaleImage(capture);
            }
            String modelToUse = (currentModel != null && !currentModel.isEmpty())?
                currentModel
                :
                "local-model"
            ;
            String visionModelToUse = (currentVisionModel != null && !currentVisionModel.isEmpty())?
                currentVisionModel
                :
                "minicpm-v-2.6"
            ;

            System.out.println("Starting OCR service...");

            List<TranslationResult> results;
            if (currentOCRMode == OCRMode.GAME_PADDLE_OCR) {
                results = performConsensusOCR(upscaledVer, modelToUse);
            } else {
                String ocrModel = (currentOCRMode == OCRMode.GAME_VISION_LLM || currentOCRMode == OCRMode.GAME_HYBRID)?
                    visionModelToUse
                    :
                    modelToUse
                ;
                results = ocrService.performOCR(upscaledVer, currentLang, ocrModel);
            }
            System.out.println("OCR complete, found " + results.size() + " texts");

            results = textGrouper.groupNearbyText(results);
            results = textGrouper.groupVerticalText(results);
            System.out.println("After grouping: " + results.size() + " texts");

            System.out.println("Translating groups...");
            batchTranslate(results, "English", modelToUse); // need setting for target lang later

            // Adjust coordinates back to original screen space
            double
                scaleX = (double) upscaledVer.getWidth() / capture.getWidth(),
                scaleY = (double) upscaledVer.getHeight() / capture.getHeight()
            ;
            for (TranslationResult res : results) {
                res.x = (int) (res.x / scaleX);
                res.y = (int) (res.y / scaleY);
                res.w = (int) (res.w / scaleX);
                res.h = (int) (res.h / scaleY);
            }

            if (callback != null) {
                callback.onTranslationReceived(results);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void batchTranslate(List<TranslationResult> groups, String targetLang, String model) {
        if (groups.isEmpty()) return;

        try {
            Map<String, String> textsToTranslate = new java.util.LinkedHashMap<>();
            for (int i = 0; i < groups.size(); i++) {
                textsToTranslate.put(String.valueOf(i), groups.get(i).originalText);
            }

            Map<String, String> translatedMap = apiClient.translateBatch(
                textsToTranslate,
                currentLang,
                targetLang,
                model,
                translationPrimer
            );

            for (int i = 0; i < groups.size(); i++) {
                String id = String.valueOf(i);
                TranslationResult res = groups.get(i);
                if (translatedMap.containsKey(id)) {
                    res.translatedText = translatedMap.get(id);
                } else {
                    // maybe append error from trans result to remaining obj keys later
                    res.translatedText = "[Error]";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // some scenarios benefit from this, but im not sure it's worth the time it
    // takes
    private List<TranslationResult> performConsensusOCR(BufferedImage original, String model) {
        System.out.println("Running 3x merge OCR...");

        BufferedImage
            imgA = original,
            imgB = ScreenCapturer.shiftImage(original, 2, 0),
            imgC = ScreenCapturer.shiftImage(original, 0, 2)
        ;

        List<TranslationResult>
            resA = ocrService.performOCR(imgA, currentLang, model),
            resB = ocrService.performOCR(imgB, currentLang, model),
            resC = ocrService.performOCR(imgC, currentLang, model)
        ;

        offsetResults(resB, -2, 0);
        offsetResults(resC, 0, -2);

        return mergeResults(resA, resB, resC);
    }

    private void offsetResults(List<TranslationResult> results, int dx, int dy) {
        for (TranslationResult r : results) {
            r.x += dx;
            r.y += dy;
        }
    }

    private List<TranslationResult> mergeResults(
        List<TranslationResult> listA,
        List<TranslationResult> listB,
        List<TranslationResult> listC
    ) {
        List<TranslationResult>
            finalResults = new ArrayList<>(),
            all = new ArrayList<>()
        ;
        all.addAll(listA);
        all.addAll(listB);
        all.addAll(listC);

        boolean[] merged = new boolean[all.size()];

        for (int i = 0; i < all.size(); i++) {
            if (merged[i]) continue;

            TranslationResult base = all.get(i);
            List<TranslationResult> cluster = new ArrayList<>();
            cluster.add(base);
            merged[i] = true;

            for (int j = i + 1; j < all.size(); j++) {
                if (merged[j]) continue;
                TranslationResult other = all.get(j);

                if (isSameBox(base, other)) {
                    cluster.add(other);
                    merged[j] = true;
                }
            }

            if (cluster.size() >= 2) {
                String bestText = getBestText(cluster);
                int avgX = 0, avgY = 0, avgW = 0, avgH = 0;
                for (TranslationResult r : cluster) {
                    avgX += r.x;
                    avgY += r.y;
                    avgW += r.w;
                    avgH += r.h;
                }
                finalResults.add(
                    new TranslationResult(
                        bestText,
                        "",
                        avgX / cluster.size(),
                        avgY / cluster.size(),
                        avgW / cluster.size(),
                        avgH / cluster.size()
                    )
                );
            }
        }
        return finalResults;
    }

    private boolean isSameBox(TranslationResult r1, TranslationResult r2) {
        int
            c1x = r1.x + r1.w / 2,
            c1y = r1.y + r1.h / 2,
            c2x = r2.x + r2.w / 2,
            c2y = r2.y + r2.h / 2
        ;
        double dist = Math.sqrt(Math.pow(c1x - c2x, 2) + Math.pow(c1y - c2y, 2));
        return dist < 20;
    }

    private String getBestText(List<TranslationResult> cluster) {
        Map<String, Integer> counts = new HashMap<>();
        for (TranslationResult r : cluster) {
            counts.put(r.originalText, counts.getOrDefault(r.originalText, 0) + 1);
        }

        String best = "";
        int max = -1;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            } else if (e.getValue() == max) {
                if (e.getKey().length() > best.length()) {
                    best = e.getKey();
                }
            }
        }
        return best;
    }
}
