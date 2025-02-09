package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import org.objectweb.asm.Handle;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author itskekoff
 * @since 20:13 of 09.02.2025
 */
public class EditFieldHandle extends EditField<Handle> {
    private final String label;
    private final ImString inputValue = new ImString(256);

    public EditFieldHandle(String label, Supplier<Handle> getter, Consumer<Handle> setter) {
        super(getter, setter);
        this.label = label;
    }

    @Override
    public void draw() {
        Handle value = get();
        String stringValue = inputValue.get().isBlank() ? "" : value != null ? value.toString() : "";
        if (ImGui.inputText(label, inputValue)) {
            try {
                Handle parsedValue = parse(stringValue);
                set(parsedValue);
            } catch (Exception e) {
                Main.getDisplayManager().addNotification(new Notification(NotificationType.ERROR, () -> "Instruction assembler",
                        ColoredStringBuilder.create().fmt("Can't parse handle from input string").get()));
            }
        }
    }

    @Override
    public void updateField() {
        this.inputValue.set(get());
    }

    @Override
    public boolean isValidInput() {
        return true;
    }

    protected Handle parse(String input) {
        String[] parts = input.split(":");
        if (parts.length == 4) {
            return new Handle(
                    Integer.parseInt(parts[0]),
                    parts[1],
                    parts[2],
                    parts[3]
            );
        }
        throw new IllegalArgumentException("Invalid Handle format");
    }
}
