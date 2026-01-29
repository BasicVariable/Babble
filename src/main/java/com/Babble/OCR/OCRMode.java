package com.Babble.OCR;

public enum OCRMode {
    DOCUMENT_TESSERACT("Document Mode (Standard)"),
    GAME_VISION_LLM("Game Mode (AI Vision)"),
    GAME_PADDLE_OCR("Game Mode (Paddle)"),
    GAME_HYBRID("Game Mode (Hybrid)");

    private final String displayName;

    OCRMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
