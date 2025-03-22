package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.impl;

import imgui.ImGui;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.EditField;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditFieldObjectArray extends EditField<Object[]> {
    private final String label;
    private final List<EditFieldConstant<?>> argumentFields = new ArrayList<>();

    public EditFieldObjectArray(String label, Supplier<Object[]> getter, Consumer<Object[]> setter) {
        super(getter, setter);
        this.label = label;
        updateField();
    }

    @Override
    public void draw() {
        ImGui.text(label);
        ImGui.pushID(label);

        if (ImGui.button("Add Argument")) {
            argumentFields.add(new EditFieldConstant<>("Arg " + argumentFields.size(),
                    () -> null, value -> {}));
            updateArray();
        }

        for (int i = 0; i < argumentFields.size(); i++) {
            EditFieldConstant<?> field = argumentFields.get(i);
            ImGui.pushID(i);
            field.draw();
            ImGui.sameLine();
            if (ImGui.button("Remove")) {
                argumentFields.remove(i);
                updateArray();
                i--;
            }
            ImGui.popID();
        }

        ImGui.popID();
    }

    @Override
    public void updateField() {
        Object[] args = get();
        argumentFields.clear();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                final int index = i;
                argumentFields.add(new EditFieldConstant<>("Arg " + i,
                        () -> args[index],
                        value -> args[index] = value));
            }
        }
    }

    private void updateArray() {
        Object[] newArgs = new Object[argumentFields.size()];
        for (int i = 0; i < argumentFields.size(); i++) {
            newArgs[i] = argumentFields.get(i).get();
        }
        set(newArgs);
    }

    @Override
    public boolean isValidInput() {
        return argumentFields.stream().allMatch(EditField::isValidInput);
    }
}