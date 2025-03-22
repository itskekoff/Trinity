package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.impl;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImBoolean;
import imgui.type.ImDouble;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImLong;
import imgui.type.ImString;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.EditField;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class EditFieldConstant<T> extends EditField<T> {
    private final String label;
    private final ImString stringInput = new ImString(256);
    private final ImInt intInput = new ImInt();
    private final ImLong longInput = new ImLong();
    private final ImFloat floatInput = new ImFloat();
    private final ImDouble doubleInput = new ImDouble();
    private final ImBoolean autoDetect = new ImBoolean(true);
    private ValueType selectedType = ValueType.STRING;
    private ValueType detectedType = ValueType.STRING;

    private enum ValueType {
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

    public EditFieldConstant(String label, Supplier<T> getter, Consumer<T> setter) {
        super(getter, setter);
        this.label = label;
        updateField();
    }

    @Override
    public void draw() {
        T value = get();
        detectType(value);

        ImGui.pushID(label);

        if (!autoDetect.get()) {
            if (ImGui.beginCombo("Type", selectedType.getDisplayName())) {
                for (ValueType type : ValueType.values()) {
                    if (ImGui.selectable(type.getDisplayName(), selectedType == type)) {
                        selectedType = type;
                        updateInputs(value);
                    }
                }
                ImGui.endCombo();
            }
        }

        ValueType currentType = autoDetect.get() ? detectedType : selectedType;

        switch (currentType) {
            case STRING:
                if (ImGui.inputText(label, stringInput)) {
                    T newValue = convertFromString(stringInput.get(), currentType);
                    if (newValue != null) set(newValue);
                }
                break;
            case INTEGER:
                if (ImGui.inputScalar(label, ImGuiDataType.S32, intInput)) {
                    set((T) Integer.valueOf(intInput.get()));
                }
                break;
            case LONG:
                if (ImGui.inputScalar(label, ImGuiDataType.S64, longInput)) {
                    set((T) Long.valueOf(longInput.get()));
                }
                break;
            case FLOAT:
                if (ImGui.inputScalar(label, ImGuiDataType.Float, floatInput, 0.1f)) {
                    Float newValue = floatInput.get();
                    set((T) newValue);
                }
                break;
            case DOUBLE:
                if (ImGui.inputScalar(label, ImGuiDataType.Double, doubleInput, 0.1)) {
                    Double newValue = doubleInput.get();
                    set((T) newValue);
                }
                break;
        }
        ImGui.checkbox("Auto Detect", autoDetect);
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("When enabled, automatically detects the type of the constant");
        }
        ImGui.popID();
    }

    @Override
    public void updateField() {
        T value = get();
        detectType(value);
        updateInputs(value);
    }

    private void detectType(T value) {
        if (value == null) {
            detectedType = ValueType.STRING;
        } else if (value instanceof String str) {
            if (str.endsWith("F") || str.endsWith("f")) {
                try {
                    Float.parseFloat(str.substring(0, str.length() - 1));
                    detectedType = ValueType.FLOAT;
                } catch (NumberFormatException e) {
                    detectedType = ValueType.STRING;
                }
            } else if (str.endsWith("D") || str.endsWith("d")) {
                try {
                    Double.parseDouble(str.substring(0, str.length() - 1));
                    detectedType = ValueType.DOUBLE;
                } catch (NumberFormatException e) {
                    detectedType = ValueType.STRING;
                }
            } else if (str.endsWith("L") || str.endsWith("l")) {
                try {
                    Long.parseLong(str.substring(0, str.length() - 1));
                    detectedType = ValueType.LONG;
                } catch (NumberFormatException e) {
                    detectedType = ValueType.STRING;
                }
            } else {
                detectedType = ValueType.STRING;
            }
        } else if (value instanceof Integer) {
            detectedType = ValueType.INTEGER;
        } else if (value instanceof Long) {
            detectedType = ValueType.LONG;
        } else if (value instanceof Float) {
            detectedType = ValueType.FLOAT;
        } else if (value instanceof Double) {
            detectedType = ValueType.DOUBLE;
        }

        if (autoDetect.get()) {
            selectedType = detectedType;
        }
    }

    private void updateInputs(T value) {
        if (value == null) {
            stringInput.set("");
            intInput.set(0);
            longInput.set(0L);
            floatInput.set(0.0f);
            doubleInput.set(0.0);
            return;
        }

        ValueType currentType = autoDetect.get() ? detectedType : selectedType;

        switch (currentType) {
            case STRING:
                stringInput.set(value instanceof String ? (String) value : value.toString());
                break;
            case INTEGER:
                intInput.set(value instanceof Integer ? (Integer) value :
                        (value instanceof String && isValidInteger((String) value) ? Integer.parseInt(((String) value).replaceAll("[^0-9-]", "")) : 0));
                break;
            case LONG:
                longInput.set(value instanceof Long ? (Long) value :
                        (value instanceof String && isValidLong((String) value) ? Long.parseLong(((String) value).replaceAll("[^0-9-]", "")) : 0L));
                break;
            case FLOAT:
                if (value instanceof Float floatValue) {
                    floatInput.set(floatValue);
                } else if (value instanceof String str && isValidFloat(str)) {
                    floatInput.set(Float.parseFloat(str.replaceAll("[Ff]", "")));
                } else {
                    floatInput.set(0.0f);
                }
                break;
            case DOUBLE:
                if (value instanceof Double doubleValue) {
                    doubleInput.set(doubleValue);
                } else if (value instanceof String str && isValidDouble(str)) {
                    doubleInput.set(Double.parseDouble(str.replaceAll("[Dd]", "")));
                } else {
                    doubleInput.set(0.0);
                }
                break;
        }
    }

    private T convertFromString(String input, ValueType type) {
        if (input == null || input.isEmpty()) return null;
        try {
            return switch (type) {
                case STRING -> (T) input;
                case INTEGER -> (T) Integer.valueOf(input.replaceAll("[^0-9-]", ""));
                case LONG -> (T) Long.valueOf(input.replaceAll("[^0-9-]", ""));
                case FLOAT -> (T) Float.valueOf(input);
                case DOUBLE -> (T) Double.valueOf(input);
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isValidInput() {
        T value = get();
        if (value == null) return false;

        ValueType currentType = autoDetect.get() ? detectedType : selectedType;

        return switch (currentType) {
            case STRING -> value instanceof String && !((String) value).isEmpty();
            case INTEGER -> value instanceof Integer || (value instanceof String && isValidInteger((String) value));
            case LONG -> value instanceof Long || (value instanceof String && isValidLong((String) value));
            case FLOAT -> value instanceof Float || (value instanceof String && isValidFloat((String) value));
            case DOUBLE -> value instanceof Double || (value instanceof String && isValidDouble((String) value));
        };
    }

    private boolean isValidInteger(String value) {
        try {
            Integer.parseInt(value.replaceAll("[^0-9-]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidLong(String value) {
        try {
            Long.parseLong(value.replaceAll("[^0-9-]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidFloat(String value) {
        try {
            Float.parseFloat(value.replaceAll("[Ff]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDouble(String value) {
        try {
            Double.parseDouble(value.replaceAll("[Dd]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}