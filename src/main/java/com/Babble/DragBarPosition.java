package com.Babble;

public enum DragBarPosition {
    TOP("Top"),
    BOTTOM("Bottom"),
    LEFT("Left"),
    RIGHT("Right");

    private final String label;

    DragBarPosition(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
