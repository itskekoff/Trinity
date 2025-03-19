package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.gui.windows.impl.ClassPickerPopup;
import me.f1nal.trinity.util.GuiUtil;

import java.util.function.Predicate;

public class ClassSelectComponent {
    private final ImString className = new ImString(256);
    private final String id = ComponentId.getId(this.getClass());
    private final Trinity trinity;
    private ClassTarget classInput;
    private final Predicate<ClassTarget> validClassPredicate;
    private final String componentName;
    private String hint = "java/lang/Object";

    public ClassSelectComponent(Trinity trinity, String componentName, Predicate<ClassTarget> validClassPredicate) {
        this.trinity = trinity;
        this.componentName = componentName;
        this.validClassPredicate = validClassPredicate;
    }

    public ClassTarget getClassInput() {
        return classInput;
    }

    public String getClassName() {
        return className.get();
    }

    public boolean draw() {
        final String oldClassName = getClassName();

        if (ImGui.inputTextWithHint(componentName + "###ClassName" + id, this.hint, className)) {
            this.queryClassInput();
        }
        ImGui.sameLine();
        if (ImGui.smallButton("...")) {
            Main.getWindowManager().addPopup(new ClassPickerPopup(this.trinity, validClassPredicate, classInput -> {
                className.set(classInput.getDisplayOrRealName());
                this.queryClassInput();
            }));
        }
        GuiUtil.tooltip("Open Class Picker");
        return !getClassName().equals(oldClassName);
    }

    public void setClassName(String className) {
        this.className.set(className);
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    private void queryClassInput() {
        this.classInput = trinity.getExecution().getClassTargetByDisplayName(this.className.get());
    }
}
