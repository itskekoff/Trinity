package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

public enum ValueType {
    STRING("String"),
    INTEGER("Integer"),
    LONG("Long"),
    FLOAT("Float"),
    DOUBLE("Double");

    private final String displayName;

    ValueType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}