package com.Babble.Processing;

import java.util.List;

public interface TranslationCallback {
    void onProcessingStart();

    void onTranslationReceived(List<TranslationResult> results);

    void setOverlayVisible(boolean visible);
}
