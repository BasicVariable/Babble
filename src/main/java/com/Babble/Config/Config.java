package com.Babble.Config;

import com.Babble.DragBarPosition;
import com.Babble.OCR.OCRMode;

// settings.yml
public class Config {
    public String
        apiUrl = "http://localhost:1234",
        modelName = "local-model",
        visionModelName = "minicpm-v-2.6",
        ocrMode = "DOCUMENT_TESSERACT",
        targetLanguage = "jpn",
        dragPosition = "TOP",
        translationPrimer = ""
    ;
    public boolean windowsConstantScan = false;

    public Config() {}

    public OCRMode getOCRModeEnum() {
        try {
            return OCRMode.valueOf(ocrMode);
        } catch (Exception e) {
            return OCRMode.DOCUMENT_TESSERACT;
        }
    }

    public DragBarPosition getDragPositionEnum() {
        try {
            return DragBarPosition.valueOf(dragPosition);
        } catch (Exception e) {
            return DragBarPosition.TOP;
        }
    }
}
