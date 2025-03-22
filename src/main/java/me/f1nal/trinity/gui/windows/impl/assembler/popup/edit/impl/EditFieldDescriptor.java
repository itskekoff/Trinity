package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.impl;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditFieldDescriptor extends EditFieldString {
    public EditFieldDescriptor(Supplier<String> getter, Consumer<String> setter) {
        super(256, "Descriptor", "java.lang.Object (Internal Type)", getter, setter);
    }
}
