package me.f1nal.trinity.gui.windows.impl.classstructure.nodes;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.AccessFlags;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.classstructure.StructureKind;
import me.f1nal.trinity.gui.windows.impl.classstructure.popup.AddFieldPopup;
import me.f1nal.trinity.gui.windows.impl.classstructure.popup.AddMethodPopup;
import me.f1nal.trinity.theme.CodeColorScheme;

public class ClassStructureNodeClass extends AbstractClassStructureNodeInput<ClassInput> {
    public ClassStructureNodeClass(ClassInput classInput) {
        super(FontAwesomeIcons.FileCode, classInput);
    }

    @Override
    protected String getText() {
        return this.getInput().getDisplaySimpleName();
    }

    @Override
    public StructureKind getKind() {
        return StructureKind.CLASSES;
    }

    @Override
    protected void appendAccessFlags(ColoredStringBuilder text, AccessFlags accessFlags) {
        accessFlags.unsetFlag(AccessFlags.FLAG_SUPER);

        if (accessFlags.isInterface()) {
            accessFlags.unsetFlag(AccessFlags.FLAG_INTERFACE);
            accessFlags.unsetFlag(AccessFlags.FLAG_ABSTRACT);
        } else if (accessFlags.isEnum()) {
            accessFlags.unsetFlag(AccessFlags.FLAG_ENUM);
        }

        super.appendAccessFlags(text, accessFlags);
    }

    @Override
    protected void appendType(ColoredStringBuilder text, String suffix) {
        AccessFlags accessFlags = getInput().getAccessFlags();
        String keyword;
        if (accessFlags.isEnum()) {
            keyword = "enum";
        } else if (accessFlags.isInterface()) {
            keyword = "interface";
        } else {
            keyword = "class";
        }
        text.text(CodeColorScheme.DISABLED, keyword + suffix);
    }

    @Override
    protected void appendParameters(ColoredStringBuilder text) {
    }

    @Override
    protected void populatePopup(PopupItemBuilder popup) {
        super.populatePopup(popup);
        popup.separator();
        popup.menuItem("Add method", () -> Main.getWindowManager().addPopup(new AddMethodPopup(this.getInput(), Main.getTrinity())));
        popup.menuItem("Add field", () -> Main.getWindowManager().addPopup(new AddFieldPopup(this.getInput(), Main.getTrinity())));
    }
}
