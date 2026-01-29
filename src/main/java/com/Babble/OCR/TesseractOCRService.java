package com.Babble.OCR;

import com.Babble.Processing.TranslationResult;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class TesseractOCRService implements OCRService {
    private final Tesseract tesseract;

    public TesseractOCRService(String dataPath, String language) {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(dataPath);
        this.tesseract.setLanguage(language);
        this.tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);
    }

    public void setLanguage(String language) {
        this.tesseract.setLanguage(language);
    }

    @Override
    public List<TranslationResult> performOCR(BufferedImage image, String targetLang, String model) {
        List<TranslationResult> groups = new ArrayList<>();

        try {
            List<Word> lines = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
            lines.removeIf(w -> w.getText().trim().isEmpty());

            TranslationResult currentGroup = null;
            for (Word lineVal : lines) {
                String lineText = lineVal.getText().trim().replace("\n", " ");
                if (lineText.isEmpty()) continue;
                Rectangle r = lineVal.getBoundingBox();

                boolean merged = false;
                if (currentGroup != null) {
                    int currentBottom = currentGroup.y + currentGroup.h;
                    int vDist = r.y - currentBottom;
                    int refHeight = Math.max(10, r.height);

                    boolean isClose = vDist < (refHeight * 1.5);
                    if (isClose && r.y > (currentGroup.y + currentGroup.h * 0.1)) {
                        currentGroup.originalText += " " + lineText;

                        int
                            newX = Math.min(currentGroup.x, r.x),
                            newY = Math.min(currentGroup.y, r.y),
                            newW = Math.max(currentGroup.x + currentGroup.w, r.x + r.width) - newX,
                            newH = Math.max(currentGroup.y + currentGroup.h, r.y + r.height) - newY
                        ;

                        currentGroup.x = newX;
                        currentGroup.y = newY;
                        currentGroup.w = newW;
                        currentGroup.h = newH;

                        merged = true;
                    }
                }

                if (!merged) {
                    currentGroup = new TranslationResult(lineText, "", r.x, r.y, r.width, r.height);
                    groups.add(currentGroup);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return groups;
    }
}
