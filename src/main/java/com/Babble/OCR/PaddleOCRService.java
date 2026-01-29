package com.Babble.OCR;

import com.Babble.Processing.TranslationResult;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.util.*;

public class PaddleOCRService implements OCRService {
    private OrtEnvironment env;
    private OrtSession detSession, recSession;
    private List<String> vocabulary;
    private boolean isReady = false;

    private static final String
        MODEL_DIR = "models",
        DET_MODEL = "det.onnx",
        REC_MODEL = "rec.onnx",
        KEYS_FILE = "keys.txt"
    ;
    private static final float DET_THRESHOLD = 0.3f;

    public PaddleOCRService() {
        try {
            System.out.println("Initializing PaddleOCR Service...");
            this.env = OrtEnvironment.getEnvironment();

            File detFile = new File(MODEL_DIR, DET_MODEL);
            File recFile = new File(MODEL_DIR, REC_MODEL);
            File keysFile = new File(MODEL_DIR, KEYS_FILE);

            if (detFile.exists() && recFile.exists() && keysFile.exists()) {
                this.detSession = env.createSession(detFile.getPath(), new OrtSession.SessionOptions());
                this.recSession = env.createSession(recFile.getPath(), new OrtSession.SessionOptions());
                this.vocabulary = loadKeys(keysFile);
                this.isReady = true;
                System.out.println("PaddleOCR models loaded successfully");
            } else {
                System.err.println("PaddleOCR models not found");
                System.err.println("Checked path: " + detFile.getAbsolutePath());
                System.err.println(
                    "Exists? det: " +
                    detFile.exists() +
                    ", rec: " +
                    recFile.exists() +
                    ", keys: " +
                    keysFile.exists()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> loadKeys(File file) {
        List<String> keys = new ArrayList<>();
        keys.add("blank");
        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                keys.add(line.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        keys.add(" ");
        return keys;
    }

    @Override
    public List<TranslationResult> performOCR(BufferedImage image, String targetLang, String model) {
        List<TranslationResult> results = new ArrayList<>();

        if (!isReady) {
            results.add(
                new TranslationResult(
                    "Error: PaddleOCR models missing. Please download det.onnx, rec.onnx, keys.txt to /models/ folder.",
                    "",
                    10,
                    10,
                    400,
                    50
                )
            );
            return results;
        }

        try {
            List<int[]> boxes = detect(image);

            for (int[] box : boxes) {
                int
                    x = box[0],
                    y = box[1],
                    w = box[2],
                    h = box[3],
                    padRecX = 20,
                    padRecY = 10
                ;

                int
                    cropX = Math.max(0, x - padRecX),
                    cropY = Math.max(0, y - padRecY),
                    cropW = Math.min(image.getWidth() - cropX, w + (x - cropX) + padRecX),
                    cropH = Math.min(image.getHeight() - cropY, h + (y - cropY) + padRecY)
                ;
                if (cropW <= 0 || cropH <= 0) continue;

                BufferedImage crop = image.getSubimage(cropX, cropY, cropW, cropH);

                if (h > w * 1.5) {
                    crop = rotateCounterClockwise(crop);
                }
                String text = recognize(crop);
                System.out.println("Paddle Rec for Box [" + x + "," + y + ",w=" + w + ",h=" + h + "]: '" + text + "'");

                if (text != null && !text.isEmpty()) {
                    results.add(new TranslationResult(text, "", x, y, w, h));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            results.add(new TranslationResult("OCR Error: " + e.getMessage(), "", 10, 10, 200, 50));
        }
        return results;
    }

    private String recognize(BufferedImage image) throws OrtException {
        int
            h = 48,
            w = (int) ((float) image.getWidth() / image.getHeight() * h)
        ;
        BufferedImage resized = resize(image, w, h);

        float[][][][] input = normalizeImage(resized);
        OnnxTensor tensor = OnnxTensor.createTensor(env, input);
        OrtSession.Result result = recSession
                .run(Collections.singletonMap(recSession.getInputNames().iterator().next(), tensor));

        float[][][] output = (float[][][]) result.get(0).getValue();

        result.close();
        tensor.close();

        return decodeCTC(output[0]);
    }

    public List<int[]> detect(BufferedImage image) throws OrtException {
        int
            w = image.getWidth(),
            h = image.getHeight(),
            maxSide = 960
        ;
        float ratio = 1.0f;
        if (Math.max(h, w) > maxSide) {
            if (h > w)
                ratio = (float) maxSide / h;
            else
                ratio = (float) maxSide / w;
        }
        int
            resizeH = (int) (h * ratio),
            resizeW = (int) (w * ratio)
        ;
        resizeH = Math.max(32, resizeH - (resizeH % 32));
        resizeW = Math.max(32, resizeW - (resizeW % 32));

        float
            ratioH = (float) resizeH / h,
            ratioW = (float) resizeW / w
        ;

        BufferedImage resized = resize(image, resizeW, resizeH);
        float[][][][] input = normalizeImage(resized);

        OnnxTensor tensor = OnnxTensor.createTensor(env, input);
        OrtSession.Result result = detSession
            .run(Collections.singletonMap(detSession.getInputNames().iterator().next(), tensor))
        ;

        float[][][][] output = (float[][][][]) result.get(0).getValue();
        result.close();
        tensor.close();

        float[][] map = output[0][0];
        List<int[]> boxes = findBoxes(map, ratioW, ratioH);

        return expandBoxes(boxes, image.getWidth(), image.getHeight(), 5, 5);
    }

    private List<int[]> expandBoxes(List<int[]> boxes, int imgW, int imgH, int padX, int padY) {
        List<int[]> expanded = new ArrayList<>();
        for (int[] box : boxes) {
            int
                x = box[0],
                y = box[1],
                w = box[2],
                h = box[3]
            ;

            int
                nx = Math.max(0, x - padX),
                ny = Math.max(0, y - padY),
                nw = w + (x - nx) + padX,
                nh = h + (y - ny) + padY
            ;

            nw = Math.min(imgW - nx, nw);
            nh = Math.min(imgH - ny, nh);

            if (nw > 0 && nh > 0) {
                expanded.add(new int[] { nx, ny, nw, nh });
            }
        }
        return expanded;
    }

    private List<int[]> findBoxes(float[][] map, float rW, float rH) {
        int
            h = map.length,
            w = map[0].length
        ;
        boolean[][] visited = new boolean[h][w];
        List<int[]> boxes = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (map[y][x] > DET_THRESHOLD && !visited[y][x]) {
                    int minX = x, maxX = x, minY = y, maxY = y;
                    Queue<int[]> queue = new LinkedList<>();
                    queue.add(new int[] { x, y });
                    visited[y][x] = true;

                    while (!queue.isEmpty()) {
                        int[] p = queue.poll();
                        int cx = p[0], cy = p[1];
                        minX = Math.min(minX, cx);
                        maxX = Math.max(maxX, cx);
                        minY = Math.min(minY, cy);
                        maxY = Math.max(maxY, cy);

                        int[]
                            dx = { 1, -1, 0, 0 },
                            dy = { 0, 0, 1, -1 }
                        ;
                        for (int k = 0; k < 4; k++) {
                            int nx = cx + dx[k];
                            int ny = cy + dy[k];
                            if (
                                nx >= 0 &&
                                nx < w &&
                                ny >= 0 &&
                                ny < h &&
                                !visited[ny][nx] &&
                                map[ny][nx] > DET_THRESHOLD
                            ) {
                                visited[ny][nx] = true;
                                queue.add(new int[] { nx, ny });
                            }
                        }
                    }

                    int bw = maxX - minX + 1;
                    int bh = maxY - minY + 1;
                    if (bw > 5 && bh > 5) {
                        boxes.add(new int[] {
                            Math.max(0, (int) (minX / rW)),
                            Math.max(0, (int) (minY / rH)),
                            (int) (bw / rW),
                            (int) (bh / rH)
                        });
                    }
                }
            }
        }
        return boxes;
    }

    private String decodeCTC(float[][] seq) {
        StringBuilder sb = new StringBuilder();
        int lastIdx = -1;

        for (float[] floats : seq) {
            int maxIdx = 0;
            float maxVal = floats[0];
            for (int j = 1; j < floats.length; j++) {
                if (floats[j] > maxVal) {
                    maxVal = floats[j];
                    maxIdx = j;
                }
            }

            if (maxIdx != 0 && maxIdx != lastIdx) {
                if (maxIdx < vocabulary.size()) {
                    sb.append(vocabulary.get(maxIdx));
                } else {
                    System.err.println("MaxIdx " + maxIdx + " out of vocab bounds (" + vocabulary.size() + ")");
                }
            }
            lastIdx = maxIdx;
        }

        return sb.toString();
    }

    private BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return resized;
    }

    private BufferedImage rotateCounterClockwise(BufferedImage src) {
        int
            w = src.getWidth(),
            h = src.getHeight()
        ;
        BufferedImage dest = new BufferedImage(h, w, src.getType());
        Graphics2D g2 = dest.createGraphics();
        g2.rotate(Math.toRadians(-90), h / 2.0, h / 2.0);
        g2.translate((h - w) / 2.0, (h - w) / 2.0);
        g2.dispose();

        BufferedImage manualDest = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                manualDest.setRGB(y, w - 1 - x, src.getRGB(x, y));
            }
        }
        return manualDest;
    }

    private float[][][][] normalizeImage(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        float[][][][] data = new float[1][3][h][w];

        float[]
            mean = { 0.485f, 0.456f, 0.406f },
            std = { 0.229f, 0.224f, 0.225f }
        ;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                float
                    r = ((rgb >> 16) & 0xFF) / 255.0f,
                    g = ((rgb >> 8) & 0xFF) / 255.0f,
                    b = (rgb & 0xFF) / 255.0f
                ;

                // r, g,b
                data[0][0][y][x] = (r - mean[0]) / std[0];
                data[0][1][y][x] = (g - mean[1]) / std[1];
                data[0][2][y][x] = (b - mean[2]) / std[2];
            }
        }
        return data;
    }
}
