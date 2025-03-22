package me.f1nal.trinity.gui.windows.impl.classstructure.popup.edit;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.gui.windows.impl.classstructure.popup.utils.DescriptorValidator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;

public class EditClassPopup extends PopupWindow {
    private final ImString className = new ImString(128);
    private final ImString superName = new ImString(128);
    private final ImString interfaces = new ImString(128);
    private final ImBoolean isPublic = new ImBoolean();
    private final ImBoolean isPrivate = new ImBoolean();
    private final ImBoolean isProtected = new ImBoolean();
    private final ImBoolean isStatic = new ImBoolean();
    private final ImBoolean isFinal = new ImBoolean();
    private final ImBoolean isAbstract = new ImBoolean();
    private final ImBoolean isInterface = new ImBoolean();

    private final ClassInput classInput;
    private final Trinity trinity;

    public EditClassPopup(ClassInput classInput, Trinity trinity) {
        super("Edit Class", trinity);
        this.classInput = classInput;
        this.trinity = trinity;

        ClassNode node = classInput.getNode();
        className.set(node.name);
        superName.set(node.superName != null ? node.superName : "java/lang/Object");
        interfaces.set(node.interfaces != null ? String.join(",", node.interfaces) : "");
        isPublic.set((node.access & Opcodes.ACC_PUBLIC) != 0);
        isPrivate.set((node.access & Opcodes.ACC_PRIVATE) != 0);
        isProtected.set((node.access & Opcodes.ACC_PROTECTED) != 0);
        isStatic.set((node.access & Opcodes.ACC_STATIC) != 0);
        isFinal.set((node.access & Opcodes.ACC_FINAL) != 0);
        isAbstract.set((node.access & Opcodes.ACC_ABSTRACT) != 0);
        isInterface.set((node.access & Opcodes.ACC_INTERFACE) != 0);
    }

    @Override
    protected void renderFrame() {
        ImGui.text("Class Name:");
        ImGui.sameLine();
        ImGui.inputText("##className", className, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.textDisabled("(e.g., com/example/MyClass)");

        ImGui.text("Superclass:");
        ImGui.sameLine();
        ImGui.inputText("##superName", superName, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.textDisabled("(e.g., java/lang/Object)");

        ImGui.text("Interfaces:");
        ImGui.sameLine();
        ImGui.inputText("##interfaces", interfaces, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.textDisabled("(e.g., java/util/List,java/lang/Runnable)");
        ImGui.separator();

        ImGui.text("Access Modifiers:");
        if (ImGui.checkbox("Public", isPublic)) {
            if (isPublic.get()) {
                isPrivate.set(false);
                isProtected.set(false);
            }
        }
        ImGui.sameLine();
        if (ImGui.checkbox("Private", isPrivate)) {
            if (isPrivate.get()) {
                isPublic.set(false);
                isProtected.set(false);
            }
        }
        ImGui.sameLine();
        if (ImGui.checkbox("Protected", isProtected)) {
            if (isProtected.get()) {
                isPublic.set(false);
                isPrivate.set(false);
            }
        }

        ImGui.checkbox("Static", isStatic);
        ImGui.sameLine();
        ImGui.checkbox("Final", isFinal);
        ImGui.sameLine();
        ImGui.checkbox("Abstract", isAbstract);
        ImGui.sameLine();
        ImGui.checkbox("Interface", isInterface);
        ImGui.separator();

        StringBuilder error = new StringBuilder();
        boolean isValid = DescriptorValidator.validateFieldDescriptor("L" + superName.get() + ";", error);
        DescriptorValidator.renderError(error.toString().replace("Invalid field descriptor", "Invalid superclass"));

        if (ImGui.button("Save Changes") && isValid && !className.get().isEmpty()) {
            updateClass();
            trinity.getEventManager().postEvent(new EventClassModified(classInput));
            Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, () -> "Class editor", ColoredStringBuilder.create()
                    .fmt("Updated class {}", generateClassPreview()).get()));
            close();
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) {
            close();
        }

        ImGui.textDisabled("Class preview: " + generateClassPreview());
    }

    private String generateClassPreview() {
        StringBuilder preview = new StringBuilder();

        if (isPublic.get()) preview.append("public ");
        else if (isPrivate.get()) preview.append("private ");
        else if (isProtected.get()) preview.append("protected ");
        if (isStatic.get()) preview.append("static ");
        if (isFinal.get()) preview.append("final ");
        if (isAbstract.get()) preview.append("abstract ");
        if (isInterface.get()) preview.append("interface ");
        else preview.append("class ");

        String name = className.get().isEmpty() ? "<name>" : shortClassName(className.get());
        preview.append(name);

        if (!superName.get().equals("java/lang/Object")) {
            preview.append(" extends ").append(shortClassName(superName.get()));
        }

        if (!interfaces.get().isEmpty()) {
            preview.append(isInterface.get() ? " extends " : " implements ");
            String[] intfArray = interfaces.get().split(",");
            for (int i = 0; i < intfArray.length; i++) {
                if (i > 0) preview.append(", ");
                preview.append(shortClassName(intfArray[i]));
            }
        }

        return preview.toString();
    }

    private String shortClassName(String fullName) {
        int lastSlash = fullName.lastIndexOf('/');
        if (lastSlash == -1) return fullName;
        return fullName.substring(lastSlash + 1);
    }

    private void updateClass() {
        ClassNode classNode = classInput.getNode();
        classNode.name = className.get();
        classNode.superName = superName.get().isEmpty() ? "java/lang/Object" : superName.get();
        classNode.access = getAccessFlags();
        classNode.interfaces.clear();
        if (!interfaces.get().isEmpty()) {
            classNode.interfaces.addAll(Arrays.asList(interfaces.get().split(",")));
        }

        ClassTarget classTarget = new ClassTarget(this.classInput.getRealName(), this.classInput.getSize());
        this.classInput.setNode(classNode);
        classTarget.setInput(this.classInput);

        this.trinity.getExecution().removeClassTarget(this.classInput.getClassTarget());
        this.trinity.getExecution().addClassTarget(classTarget);
    }

    private int getAccessFlags() {
        int access = 0;
        if (isPublic.get()) access |= Opcodes.ACC_PUBLIC;
        if (isPrivate.get()) access |= Opcodes.ACC_PRIVATE;
        if (isProtected.get()) access |= Opcodes.ACC_PROTECTED;
        if (isStatic.get()) access |= Opcodes.ACC_STATIC;
        if (isFinal.get()) access |= Opcodes.ACC_FINAL;
        if (isAbstract.get()) access |= Opcodes.ACC_ABSTRACT;
        if (isInterface.get()) access |= Opcodes.ACC_INTERFACE;
        return access;
    }
}