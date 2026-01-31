package com.Babble;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {

    public static BufferedImage upscaleImage(BufferedImage capture) {
        if (capture == null) return null;

        // Upscale 2x
        int newW = capture.getWidth() * 2;
        int newH = capture.getHeight() * 2;

        return resize(capture, newW, newH);
    }

    public static BufferedImage resize(BufferedImage original, int newW, int newH) {
        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();

        return resized;
    }

    public static BufferedImage letterboxImage(BufferedImage original, int targetMultiple) {
        if (original == null) return null;
        int
            w = original.getWidth(),
            h = original.getHeight(),
            MAX_DIM = 960
        ;
        double scale = 1.0;

        if (w > MAX_DIM || h > MAX_DIM) {
            scale = (double) MAX_DIM / Math.max(w, h);
        }

        int
            scaledW = (int) (w * scale),
            scaledH = (int) (h * scale),
            targetW = ((scaledW + targetMultiple - 1) / targetMultiple) * targetMultiple,
            targetH = ((scaledH + targetMultiple - 1) / targetMultiple) * targetMultiple
        ;

        BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, targetW, targetH);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, scaledW, scaledH, null);
        g2d.dispose();

        return resized;
    }

    public static BufferedImage enhanceContrast(BufferedImage image, double brightnessFactor, double contrastFactor) {
        if (image == null) return null;

        int
            width = image.getWidth(),
            height = image.getHeight()
        ;

        BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int
                    rgb = image.getRGB(x, y),
                    r = (rgb >> 16) & 0xFF,
                    g = (rgb >> 8) & 0xFF,
                    b = rgb & 0xFF
                ;

                // brightness
                r = clamp((int) (r * brightnessFactor));
                g = clamp((int) (g * brightnessFactor));
                b = clamp((int) (b * brightnessFactor));
                // contrast
                r = clamp((int) (((r - 128) * contrastFactor) + 128));
                g = clamp((int) (((g - 128) * contrastFactor) + 128));
                b = clamp((int) (((b - 128) * contrastFactor) + 128));
                enhanced.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return enhanced;
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }

    public static BufferedImage toGrayscale(BufferedImage src) {
        int
            w = src.getWidth(),
            h = src.getHeight()
        ;
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int
                    rgb = src.getRGB(x, y),
                    r = (rgb >> 16) & 0xFF,
                    g = (rgb >> 8) & 0xFF,
                    b = rgb & 0xFF  ,
                    val = (r + g + b) / 3,
                    grayPixel = (val << 16) | (val << 8) | val
                ;

                gray.setRGB(x, y, grayPixel);
            }
        }
        return gray;
    }

    public static String imageToBase64(BufferedImage image) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
