package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author itskekoff
 * @since 20:13 of 09.02.2025
 */
public class EditFieldObject<T> extends EditField<T> {
    private final String label;
    private final ImString stringInput = new ImString(256);

    public EditFieldObject(String label, Supplier<T> getter, Consumer<T> setter) {
        super(getter, setter);
        this.label = label;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void draw() {
        T value = get();
        String stringValue = value != null ? value.toString() : "";
        this.stringInput.set(stringValue);

        if (ImGui.inputText(label, this.stringInput)) set((T) this.stringInput.get());
    }

    @Override
    public void updateField() {
        this.stringInput.set(get());
    }

    @Override
    public boolean isValidInput() {
        return true;
    }
}
