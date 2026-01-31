package com.Babble.OCR;

public enum OCRMode {
    DOCUMENT_TESSERACT("Document Mode"),
    GAME_VISION_LLM("AI Vision"),
    GAME_PADDLE_OCR("Paddle"),
    GAME_HYBRID("Hybrid");

    private final String displayName;

    OCRMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
