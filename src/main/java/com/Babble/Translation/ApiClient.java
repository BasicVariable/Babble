package com.Babble.Translation;

import java.io.IOException;

public interface ApiClient {
    java.util.Map<String, String> translateBatch(
        java.util.Map<String,
        String> texts,
        String sourceLang,
        String targetLang,
        String model,
        String primer
    ) throws IOException, InterruptedException;
}
