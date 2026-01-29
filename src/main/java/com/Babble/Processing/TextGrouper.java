package com.Babble.Processing;

import java.util.ArrayList;
import java.util.List;

public class TextGrouper {

    public List<TranslationResult> groupNearbyText(List<TranslationResult> raw) {
        if (raw.isEmpty()) return raw;

        raw.sort((a, b) -> {
            int yDiff = Math.abs(a.y - b.y);
            if (yDiff < 8) return Integer.compare(a.x, b.x);
            return Integer.compare(a.y, b.y);
        });

        List<TranslationResult> grouped = new ArrayList<>();
        TranslationResult current = raw.get(0);

        for (int i = 1; i < raw.size(); i++) {
            TranslationResult next = raw.get(i);

            boolean sameLine = Math.abs(current.y - next.y) < 8;
            int hGap = next.x - (current.x + current.w);
            boolean closeEnough = hGap < 50;

            if (sameLine && closeEnough) {
                String newText = current.originalText + " " + next.originalText;

                int
                    minX = Math.min(current.x, next.x),
                    minY = Math.min(current.y, next.y),
                    maxX = Math.max(current.x + current.w, next.x + next.w),
                    maxY = Math.max(current.y + current.h, next.y + next.h)
                ;

                current = new TranslationResult(newText, "", minX, minY, maxX - minX, maxY - minY);
            } else {
                grouped.add(current);
                current = next;
            }
        }
        grouped.add(current);

        return grouped;
    }

    public List<TranslationResult> groupVerticalText(List<TranslationResult> raw) {
        if (raw.isEmpty()) return raw;

        raw.sort((a, b) -> {
            int xDiff = Math.abs(a.x - b.x);
            if (xDiff < 10) return Integer.compare(a.y, b.y);
            return Integer.compare(a.x, b.x);
        });

        List<TranslationResult> grouped = new ArrayList<>();
        TranslationResult current = raw.get(0);

        for (int i = 1; i < raw.size(); i++) {
            TranslationResult next = raw.get(i);

            int
                c1 = current.x + (current.w / 2),
                c2 = next.x + (next.w / 2)
            ;
            boolean centerAligned = Math.abs(c1 - c2) < 10;

            int vGap = next.y - (current.y + current.h);
            boolean closeVertically = vGap < 15 && vGap >= -5;

            boolean similarWidth = Math.abs(current.w - next.w) < (Math.max(current.w, next.w) * 0.5);

            if (centerAligned && closeVertically && similarWidth) {
                String newText = current.originalText + next.originalText;

                int
                    minX = Math.min(current.x, next.x),
                    minY = Math.min(current.y, next.y),
                    maxX = Math.max(current.x + current.w, next.x + next.w),
                    maxY = Math.max(current.y + current.h, next.y + next.h)
                ;

                current = new TranslationResult(newText, "", minX, minY, maxX - minX, maxY - minY);
            } else {
                grouped.add(current);
                current = next;
            }
        }
        grouped.add(current);
        return grouped;
    }
}
