package com.Babble.OCR;

public enum OCRPreprocessingMode {
    NATIVE("Native"),
    UPSCALED("Upscaled"),
    LETTERBOXED("Letterboxed"),
    AUTO("Auto");

    private final String displayName;

    OCRPreprocessingMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
