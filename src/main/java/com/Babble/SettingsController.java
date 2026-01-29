package com.Babble;

import com.Babble.OCR.OCRMode;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SettingsController {
    @FXML
    private TextField urlField;
    @FXML
    private TextField modelField;
    @FXML
    private TextField visionModelField;
    @FXML
    private ComboBox<OCRMode> ocrModeCombo;
    @FXML
    private ComboBox<String> languageCombo;
    @FXML
    private ComboBox<DragBarPosition> dragPosCombo;
    @FXML
    private javafx.scene.control.CheckBox wcsCheckbox;
    @FXML
    private javafx.scene.control.TextArea primerArea;

    private Stage settingsStage;
    private MenuController mainController;

    public void setMainController(MenuController controller) {
        this.mainController = controller;

        ocrModeCombo.getItems().setAll(OCRMode.values());
        dragPosCombo.getItems().setAll(DragBarPosition.values());

        languageCombo.getItems().addAll("jpn");

        urlField.setText(controller.getCurrentApiUrl());
        modelField.setText(controller.getCurrentModel());
        visionModelField.setText(controller.getCurrentVisionModel());
        ocrModeCombo.getSelectionModel().select(controller.getCurrentOCRMode());

        languageCombo.getSelectionModel().select(controller.getCurrentLanguage());
        dragPosCombo.getSelectionModel().select(controller.getCurrentDragPosition());
        wcsCheckbox.setSelected(controller.isWCSMode());

        primerArea.setText(controller.getCurrentPrimer());

        if (!com.sun.jna.Platform.isWindows()) {
            wcsCheckbox.setDisable(true);
            wcsCheckbox.setText("");
        }
    }

    public void setStage(Stage stage) {
        this.settingsStage = stage;
    }

    @FXML
    private void onSave() {
        String
            newUrl = urlField.getText(),
            newModel = modelField.getText(),
            newVisionModel = visionModelField.getText(),
            selectedLang = languageCombo.getSelectionModel().getSelectedItem(),
            primer = primerArea.getText()
        ;
        OCRMode selectedMode = ocrModeCombo.getSelectionModel().getSelectedItem();
        DragBarPosition selectedPos = dragPosCombo.getSelectionModel().getSelectedItem();
        boolean wcs = wcsCheckbox.isSelected();

        if (newUrl == null || newUrl.trim().isEmpty()) {
            newUrl = "http://localhost:1234";
        }

        if (newModel == null || newModel.trim().isEmpty()) {
            newModel = "local-model";
        }

        if (newVisionModel == null || newVisionModel.trim().isEmpty()) {
            newVisionModel = "minicpm-v-2.6";
        }

        if (selectedMode == null)
            selectedMode = OCRMode.DOCUMENT_TESSERACT;

        if (selectedLang == null) selectedLang = "jpn";

        if (selectedPos == null)
            selectedPos = DragBarPosition.TOP;

        if (primer == null) primer = "";

        mainController.updateSettings(
            newUrl,
            newModel,
            newVisionModel,
            selectedMode,
            selectedLang,
            selectedPos,
            wcs,
            primer
        );

        if (settingsStage != null) {
            settingsStage.close();
        }
    }
}
