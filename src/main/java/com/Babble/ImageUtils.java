package com.Babble;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {

    public static BufferedImage upscaleImage(BufferedImage capture) {
        if (capture == null)
            return null;

        // Upscale 2x
        int newW = capture.getWidth() * 2;
        int newH = capture.getHeight() * 2;

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();

        // Bicubic seems to be better for game text?
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(capture, 0, 0, newW, newH, null);
        g.dispose();

        return resized;
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
