package com.Babble;

import com.Babble.Config.Config;
import com.Babble.Config.ConfigManager;
import com.Babble.OCR.OCRMode;
import com.Babble.Translation.ApiClient;
import com.Babble.Translation.LMStudioClient;
import com.Babble.Window.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.Babble.Processing.ProcessingPipeline;
import com.Babble.Processing.TranslationResult;
import com.Babble.Processing.TranslationCallback;

public class MenuController {
    @FXML
    private BorderPane rootPane;
    @FXML
    private Pane transparentPane;
    @FXML
    private Button startButton, stopButton, settingsButton, closeButton;

    private Label floatingLabel;
    private Stage overlayStage, settingsStage;

    private ScheduledExecutorService scheduler;
    private ProcessingPipeline processing;

    private String currentPrimer = "";
    private Config config = new Config();

    private javafx.scene.layout.Region controlBar;
    private WindowManager windowManager;

    private void updateWDA() {
        if (windowManager != null) {
            windowManager.setWDA(isWCSMode());
        }
    }

    public void setConfig(Config config) {
        this.config = config;
        this.currentPrimer = config.translationPrimer;
        if (this.currentPrimer == null)
            this.currentPrimer = "";

        updateDragBar(config.getDragPositionEnum());
        if (windowManager != null) {
            Platform.runLater(this::updateWDA);
        }
    }

    public void initClickThrough() {
        if (windowManager != null) {
            windowManager.updateWindowRegion();
        }
    }

    private void updateDragBar(DragBarPosition pos) {
        rootPane.setTop(null);
        rootPane.setBottom(null);
        rootPane.setLeft(null);
        rootPane.setRight(null);

        boolean vertical = (pos == DragBarPosition.LEFT || pos == DragBarPosition.RIGHT);

        // Create the control bar wrapper
        javafx.scene.layout.Pane box = vertical?
            new javafx.scene.layout.VBox(5)
            :
            new javafx.scene.layout.HBox(5)
        ;
        box.setPadding(new javafx.geometry.Insets(2));
        box.getStyleClass().add("control-bar");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        if (vertical) {
            ((javafx.scene.layout.VBox) box).setAlignment(javafx.geometry.Pos.TOP_CENTER);
            javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        } else {
            ((javafx.scene.layout.HBox) box).setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        }

        box.getChildren().addAll(startButton, stopButton, spacer, settingsButton, closeButton);

        javafx.scene.layout.Pane wrapper = vertical ? new javafx.scene.layout.VBox() : new javafx.scene.layout.HBox();
        wrapper.setPickOnBounds(false);

        if (vertical) {
            javafx.scene.layout.VBox vWrap = (javafx.scene.layout.VBox) wrapper;
            vWrap.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            javafx.scene.layout.VBox.setVgrow(box, javafx.scene.layout.Priority.ALWAYS);
        } else {
            javafx.scene.layout.HBox hWrap = (javafx.scene.layout.HBox) wrapper;
            hWrap.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            javafx.scene.layout.HBox.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);
        }

        wrapper.getChildren().add(box);
        this.controlBar = wrapper;

        switch (pos) {
            case TOP -> {
                rootPane.setTop(wrapper);
                ((javafx.scene.layout.HBox) wrapper).setFillHeight(true);
            }
            case BOTTOM -> {
                rootPane.setBottom(wrapper);
                ((javafx.scene.layout.HBox) wrapper).setFillHeight(true);
            }
            case LEFT -> {
                rootPane.setLeft(wrapper);
                ((javafx.scene.layout.VBox) wrapper).setFillWidth(true);
            }
            case RIGHT -> {
                rootPane.setRight(wrapper);
                ((javafx.scene.layout.VBox) wrapper).setFillWidth(true);
            }
        }

        if (windowManager != null) {
            Platform.runLater(windowManager::updateWindowRegion);
        }
    }

    private TranslationCallback initTranslationCallback() {
        return new TranslationCallback() {
            @Override
            public void onTranslationReceived(java.util.List<TranslationResult> results) {
                MenuController.this.onTranslationReceived(results);
            }

            @Override
            public void onProcessingStart() {
                MenuController.this.onProcessingStart();
            }

            @Override
            public void setOverlayVisible(boolean visible) {
                if (isWCSMode())
                    return;

                if (overlayStage != null) {
                    Platform.runLater(() -> overlayStage.setOpacity(
                        visible?1.0:0.0
                    ));
                }
            }
        };
    }

    public void setStage(Stage stage) {
        this.windowManager = new WindowManager(
            stage,
            rootPane,
            transparentPane
        );

        stage.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.H && controlBar != null) {
                controlBar.setVisible(!controlBar.isVisible());
                controlBar.setManaged(controlBar.isVisible());
            }
        });

        if (config.getDragPositionEnum() != null) {
            updateDragBar(config.getDragPositionEnum());
        }

        windowManager.initOverlayStage();
        this.overlayStage = windowManager.getOverlayStage();
        this.floatingLabel = windowManager.getFloatingLabel();

        ApiClient client = new LMStudioClient(config.apiUrl);

        processing = new ProcessingPipeline(
            "tessData",
            config.targetLanguage,
            transparentPane,
            client,
            initTranslationCallback()
        );
        processing.setModel(config.modelName);
        processing.setPrimer(currentPrimer);
        processing.setVisionModel(config.visionModelName);
        processing.setOCRMode(config.getOCRModeEnum());
    }

    public void updateSettings(
        String url,
        String model,
        String visionModel,
        OCRMode mode,
        String lang,
        DragBarPosition pos,
        boolean wcs,
        String primer
    ) {
        this.currentPrimer = primer;

        config.apiUrl = url;
        config.modelName = model;
        config.visionModelName = visionModel;
        config.ocrMode = mode.name();
        config.targetLanguage = lang;
        config.dragPosition = pos.name();
        config.windowsConstantScan = wcs;
        config.translationPrimer = primer;
        ConfigManager.save(config);

        updateDragBar(pos);
        updateWDA();

        ApiClient client = new LMStudioClient(url);
        boolean wasScanning = processing != null && processing.isScanning();

        processing = new ProcessingPipeline(
            "tessData",
            lang,
            transparentPane,
            client,
            initTranslationCallback()
        );
        processing.setModel(model);
        processing.setVisionModel(visionModel);
        processing.setOCRMode(mode);
        processing.setScanning(wasScanning);
        processing.setPrimer(primer);
    }

    private void onTranslationReceived(java.util.List<TranslationResult> results) {
        System.out.println("MenuController received " + results.size() + " results.");

        Platform.runLater(() -> {
            if (!processing.isScanning() && isWCSMode())
                return;

            if (!isWCSMode()) {
                startButton.setDisable(false);
                processing.setScanning(false);
            }

            if (overlayStage == null || overlayStage.getScene() == null)
                return;

            floatingLabel.setText("Translating...");
            floatingLabel.setVisible(false);

            Pane root = (Pane) overlayStage.getScene().getRoot();
            root.getChildren().removeIf(node -> node != floatingLabel);

            for (TranslationResult res : results) {
                String textToShow = (res.translatedText == null || res.translatedText.trim().isEmpty())
                        ? res.originalText + " (?)"
                        : res.translatedText;

                if (isWCSMode() && !processing.isScanning())
                    break;
                if (textToShow.trim().isEmpty())
                    continue;

                textToShow = textToShow.replace("\n", " ").replace("\r", " ").replace("\u00A0", " ");

                Label label = new Label(textToShow);

                double heightBasedSize = Math.max(12, Math.min(res.h * 0.7, 60));
                double textLength = Math.max(1, textToShow.length());
                double area = res.w * res.h;
                double densityBasedSize = Math.sqrt((area * 0.5) / textLength);
                double fontSize = Math.min(heightBasedSize, densityBasedSize);
                fontSize = Math.max(12, Math.min(fontSize, 60));
                boolean isVertical = res.h > (res.w * 1.1);

                String baseStyle = String.format("-fx-font-size: %.1fpx;", fontSize);

                label.getStyleClass().add("overlay-label");

                label.setStyle(baseStyle);
                label.setLayoutX(res.x);
                label.setLayoutY(res.y);

                if (isVertical) {
                    label.setWrapText(false);
                    label.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    label.setMaxWidth(600);
                } else {
                    label.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                    label.setPrefWidth(res.w);
                    label.setMaxWidth(res.w * 1.1);
                    label.setWrapText(true);
                }

                root.getChildren().add(label);
            }
        });
    }

    private void onProcessingStart() {
        Platform.runLater(() -> {
            if (overlayStage != null && overlayStage.getScene() != null) {
                Pane root = (Pane) overlayStage.getScene().getRoot();
                root.getChildren().removeIf(n -> n != floatingLabel);
            }

            if (floatingLabel != null) {
                floatingLabel.setText("Translating...");
                floatingLabel.setVisible(true);
                floatingLabel.getStyleClass().add("floating-label");
                floatingLabel.toFront();
            }
        });
    }

    @FXML
    private void onOpenSettings() {
        if (settingsStage != null && settingsStage.isShowing()) {
            settingsStage.toFront();
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/settings.fxml")
            );
            javafx.scene.Parent root = loader.load();

            SettingsController controller = loader.getController();
            controller.setMainController(this);

            settingsStage = new Stage();
            settingsStage.initStyle(StageStyle.UTILITY);
            settingsStage.setTitle("Settings");
            settingsStage.setScene(new Scene(root));
            settingsStage.setResizable(false);
            settingsStage.setAlwaysOnTop(true);

            controller.setStage(settingsStage);
            settingsStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onStop() {
        processing.setScanning(false);
        startButton.setDisable(false);
        stopButton.setDisable(true);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        floatingLabel.setVisible(false);

        Platform.runLater(() -> {
            if (overlayStage != null && overlayStage.getScene() != null) {
                Pane root = (Pane) overlayStage.getScene().getRoot();
                root.getChildren().removeIf(n -> n != floatingLabel);
            }
        });
    }

    @FXML
    private void onClose() {
        onStop();
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void onStart() {
        processing.setScanning(true);

        if (isWCSMode()) {
            startButton.setDisable(true);
            stopButton.setDisable(false);
        } else {
            startButton.setDisable(true);
            stopButton.setDisable(false);
        }

        floatingLabel.setVisible(true);
        floatingLabel.setText("Translating...");
        floatingLabel.getStyleClass().add("floating-label");
        floatingLabel.setLayoutX(10);
        floatingLabel.setLayoutY(10);
        floatingLabel.toFront();

        processing.setModel(config.modelName);

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();

        if (isWCSMode()) {
            scheduler.scheduleAtFixedRate(processing::processLoopLogic, 0, 3, TimeUnit.SECONDS);
        } else {
            scheduler.execute(() -> {
                processing.processLoopLogic(true);
            });
        }
    }

    //

    public String getCurrentLanguage() {
        return config.targetLanguage;
    }

    public DragBarPosition getCurrentDragPosition() {
        return config.getDragPositionEnum();
    }

    public String getCurrentApiUrl() {
        return config.apiUrl;
    }

    public String getCurrentModel() {
        return config.modelName;
    }

    public String getCurrentVisionModel() {
        return config.visionModelName;
    }

    public OCRMode getCurrentOCRMode() {
        return config.getOCRModeEnum();
    }

    public boolean isWCSMode() {
        return config != null && config.windowsConstantScan;
    }

    public String getCurrentPrimer() {
        return currentPrimer;
    }
}
