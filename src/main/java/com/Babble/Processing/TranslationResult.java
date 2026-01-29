package com.Babble.Processing;

public class TranslationResult {
    public String originalText;
    public String translatedText;
    public int x, y, w, h;

    public TranslationResult(String originalText, String translatedText, int x, int y, int w, int h) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
