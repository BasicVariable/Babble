package com.Babble.OCR;

import com.Babble.Processing.TranslationResult;
import java.awt.image.BufferedImage;
import java.util.List;

public interface OCRService {
    List<TranslationResult> performOCR(BufferedImage image, String targetLang, String model);
}
