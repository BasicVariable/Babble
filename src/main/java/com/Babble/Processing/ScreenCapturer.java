package com.Babble.Processing;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ScreenCapturer {
    private Robot robot;
    private final Pane transparentPane;

    private BufferedImage lastCapture;
    private int cacheWidth = 0;
    private int cacheHeight = 0;

    public ScreenCapturer(Pane transparentPane) {
        this.transparentPane = transparentPane;
    }

    public BufferedImage captureScreen() throws AWTException {
        Point2D panePos = transparentPane.localToScreen(0, 0);
        if (panePos == null)
            return null;

        double
            w = transparentPane.getWidth(),
            h = transparentPane.getHeight()
        ;

        if (robot == null)
            robot = new Robot();

        return robot.createScreenCapture(
            new Rectangle(
                (int) panePos.getX(),
                (int) panePos.getY(),
                (int) w,
                (int) h
            )
        );
    }

    public double imageDiffPercent(BufferedImage capture) {
        if (lastCapture == null || capture == null)
            return 100.0;

        int
            width = capture.getWidth(),
            height = capture.getHeight()
        ;
        if (width != cacheWidth || height != cacheHeight) {
            cacheWidth = width;
            cacheHeight = height;
            return 100.0;
        }

        long diffPixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (capture.getRGB(x, y) != lastCapture.getRGB(x, y)) {
                    diffPixels++;
                }
            }
        }

        return (double) diffPixels / (width * height) * 100.0;
    }

    public void updateLastCapture(BufferedImage capture) {
        this.lastCapture = capture;
        if (capture != null) {
            this.cacheWidth = capture.getWidth();
            this.cacheHeight = capture.getHeight();
        }
    }

    public boolean hasLastCapture() {
        return lastCapture != null;
    }

    public static BufferedImage shiftImage(BufferedImage src, int dx, int dy) {
        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, dx, dy, null);
        g.dispose();
        return dest;
    }
}
