package com.Babble.Window;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WindowManager {
    private final Stage stage;
    private final Pane rootPane;
    private final Pane transparentPane;

    private double xOff = 0;
    private double yOff = 0;

    private static final int BORDER = 20;
    private static final double MIN_WIDTH = 40;
    private static final double MIN_HEIGHT = 40;

    private Stage overlayStage;
    private Label floatingLabel;

    public WindowManager(Stage stage, Pane rootPane, Pane transparentPane) {
        this.stage = stage;
        this.rootPane = rootPane;
        this.transparentPane = transparentPane;

        initWindowInput();
    }

    private void initWindowInput() {
        rootPane.setOnMouseMoved(this::handleMouseMoved);
        rootPane.setOnMousePressed(this::handleMousePressed);
        rootPane.setOnMouseDragged(this::handleMouseDragged);
    }

    private void handleMouseMoved(MouseEvent event) {
        double mouseEventX = event.getSceneX();
        double mouseEventY = event.getSceneY();
        double sceneWidth = rootPane.getWidth();
        double sceneHeight = rootPane.getHeight();

        Cursor cursor = Cursor.DEFAULT;

        if (mouseEventX < BORDER && mouseEventY < BORDER)
            cursor = Cursor.NW_RESIZE;
        else if (mouseEventX > sceneWidth - BORDER && mouseEventY > sceneHeight - BORDER)
            cursor = Cursor.SE_RESIZE;
        else if (mouseEventX < BORDER && mouseEventY > sceneHeight - BORDER)
            cursor = Cursor.SW_RESIZE;
        else if (mouseEventX > sceneWidth - BORDER && mouseEventY < BORDER)
            cursor = Cursor.NE_RESIZE;
        else if (mouseEventX < BORDER)
            cursor = Cursor.W_RESIZE;
        else if (mouseEventX > sceneWidth - BORDER)
            cursor = Cursor.E_RESIZE;
        else if (mouseEventY < BORDER)
            cursor = Cursor.N_RESIZE;
        else if (mouseEventY > sceneHeight - BORDER)
            cursor = Cursor.S_RESIZE;

        rootPane.setCursor(cursor);
    }

    private void handleMousePressed(MouseEvent event) {
        xOff = event.getSceneX();
        yOff = event.getSceneY();
    }

    private void handleMouseDragged(MouseEvent event) {
        Cursor cursor = rootPane.getCursor();

        if (cursor == null || cursor == Cursor.DEFAULT) {
            stage.setX(event.getScreenX() - xOff);
            stage.setY(event.getScreenY() - yOff);
        } else {
            performResize(event, cursor);
        }
    }

    private void performResize(MouseEvent event, Cursor cursor) {
        double mouseEventX = event.getScreenX();
        double mouseEventY = event.getScreenY();

        double currentMinW = MIN_WIDTH;
        double currentMinH = MIN_HEIGHT;

        // account for internal content size
        if (cursor.equals(Cursor.W_RESIZE) || cursor.equals(Cursor.NW_RESIZE) || cursor.equals(Cursor.SW_RESIZE)) {
            double newWidth = stage.getX() + stage.getWidth() - mouseEventX;
            if (newWidth > currentMinW) {
                stage.setWidth(newWidth);
                stage.setX(mouseEventX);
            }
        }
        if (cursor.equals(Cursor.E_RESIZE) || cursor.equals(Cursor.NE_RESIZE) || cursor.equals(Cursor.SE_RESIZE)) {
            double newWidth = mouseEventX - stage.getX();
            if (newWidth > currentMinW) {
                stage.setWidth(newWidth);
            }
        }
        if (cursor.equals(Cursor.N_RESIZE) || cursor.equals(Cursor.NW_RESIZE) || cursor.equals(Cursor.NE_RESIZE)) {
            double newHeight = stage.getY() + stage.getHeight() - mouseEventY;
            if (newHeight > currentMinH) {
                stage.setHeight(newHeight);
                stage.setY(mouseEventY);
            }
        }
        if (cursor.equals(Cursor.S_RESIZE) || cursor.equals(Cursor.SW_RESIZE) || cursor.equals(Cursor.SE_RESIZE)) {
            double newHeight = mouseEventY - stage.getY();
            if (newHeight > currentMinH) {
                stage.setHeight(newHeight);
            }
        }
    }

    public void updateWindowRegion() {
        PlatformUtils.updateWindowRegion(stage, transparentPane);
    }

    public void initOverlayStage() {
        if (overlayStage != null)
            return;

        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);

        Pane root = new Pane();
        root.setStyle("-fx-background-color: transparent;");

        floatingLabel = new Label("");
        floatingLabel.getStyleClass().add("floating-label");
        floatingLabel.setWrapText(true);
        floatingLabel.setVisible(false);

        root.getChildren().add(floatingLabel);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/mStyle.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        overlayStage.setScene(scene);

        String title = "BabbleOverlay" + System.currentTimeMillis();
        overlayStage.setTitle(title);
        overlayStage.show();

        updateOverlayPosition();

        // JNA click-through
        PlatformUtils.makeWindowTransparentRaw(overlayStage);

        javafx.beans.value.ChangeListener<Number> sizeListener = (obs, oldVal, newVal) -> {
            updateWindowRegion();
            updateOverlayPosition();
        };
        stage.widthProperty().addListener(sizeListener);
        stage.heightProperty().addListener(sizeListener);

        javafx.beans.value.ChangeListener<Number> posListener = (obs, oldVal, newVal) -> {
            updateOverlayPosition();
        };
        stage.xProperty().addListener(posListener);
        stage.yProperty().addListener(posListener);

        transparentPane.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            updateWindowRegion();
            updateOverlayPosition();
        });
        transparentPane.localToSceneTransformProperty().addListener((obs, old, nev) -> {
            updateWindowRegion();
            updateOverlayPosition();
        });
    }

    public void updateOverlayPosition() {
        if (overlayStage != null && transparentPane != null) {
            Point2D p = transparentPane.localToScreen(0, 0);
            if (p != null) {
                overlayStage.setX(p.getX());
                overlayStage.setY(p.getY());
                overlayStage.setWidth(transparentPane.getWidth());
                overlayStage.setHeight(transparentPane.getHeight());
            }
        }
    }

    public Stage getOverlayStage() {
        return overlayStage;
    }

    public Label getFloatingLabel() {
        return floatingLabel;
    }

    public void setWDA(boolean enable) {
        if (overlayStage != null) {
            PlatformUtils.setWindowDisplayAffinity(overlayStage, enable);
        }
    }
}
