package com.Babble.OCR;

public enum OCRPreprocessingMode {
    NATIVE("Native (No Scaling)"),
    UPSCALED("Upscaled (Standard)"),
    LETTERBOXED("Letterboxed (Padding)"),
    AUTO("Auto (Smart Switch)");

    private final String displayName;

    OCRPreprocessingMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
