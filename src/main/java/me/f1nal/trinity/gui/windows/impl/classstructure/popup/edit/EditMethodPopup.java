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
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.gui.windows.impl.classstructure.popup.utils.DescriptorValidator;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;

public class EditMethodPopup extends PopupWindow {
    private final ImString methodName = new ImString(32);
    private final ImString descriptor = new ImString(64);
    private final ImString exceptions = new ImString(128);
    private final ImBoolean isPublic = new ImBoolean();
    private final ImBoolean isPrivate = new ImBoolean();
    private final ImBoolean isProtected = new ImBoolean();
    private final ImBoolean isStatic = new ImBoolean();
    private final ImBoolean isFinal = new ImBoolean();
    private final ImBoolean isAbstract = new ImBoolean();

    private final ClassInput classInput;
    private final MethodInput methodInput;
    private final Trinity trinity;

    public EditMethodPopup(MethodInput methodInput, Trinity trinity) {
        super("Edit Method", trinity);
        this.methodInput = methodInput;
        this.classInput = methodInput.getOwningClass();
        this.trinity = trinity;

        MethodNode node = methodInput.getNode();
        methodName.set(node.name);
        descriptor.set(node.desc);
        exceptions.set(node.exceptions != null ? String.join(",", node.exceptions) : "");
        isPublic.set((node.access & Opcodes.ACC_PUBLIC) != 0);
        isPrivate.set((node.access & Opcodes.ACC_PRIVATE) != 0);
        isProtected.set((node.access & Opcodes.ACC_PROTECTED) != 0);
        isStatic.set((node.access & Opcodes.ACC_STATIC) != 0);
        isFinal.set((node.access & Opcodes.ACC_FINAL) != 0);
        isAbstract.set((node.access & Opcodes.ACC_ABSTRACT) != 0);
    }

    @Override
    protected void renderFrame() {
        ImGui.text("Method Name:");
        ImGui.sameLine();
        ImGui.inputText("##methodName", methodName, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.separator();

        ImGui.text("Descriptor:");
        ImGui.sameLine();
        ImGui.inputText("##descriptor", descriptor, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.textDisabled("(e.g., (Ljava/lang/String;[B)Ljava/util/List;)");

        ImGui.text("Exceptions:");
        ImGui.sameLine();
        ImGui.inputText("##exceptions", exceptions, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.textDisabled("(e.g., java/lang/IOException,java/lang/IllegalArgumentException)");
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
        ImGui.separator();

        StringBuilder error = new StringBuilder();
        boolean isValid = DescriptorValidator.validateMethodDescriptor(descriptor.get(), error);
        DescriptorValidator.renderError(error.toString());

        if (ImGui.button("Save Changes") && isValid && !methodName.get().isEmpty()) {
            updateMethod();
            trinity.getEventManager().postEvent(new EventClassModified(classInput));
            Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, () -> "Method editor", ColoredStringBuilder.create()
                    .fmt("Updated method {} in {}", generateMethodPreview(), classInput.getDisplaySimpleName()).get()));
            Main.getWindowManager().getWindowsOfType(DecompilerWindow.class).forEach(DecompilerWindow::updateClassStructure);

            close();
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) {
            close();
        }

        ImGui.textDisabled("Method preview: " + generateMethodPreview());
    }

    private String generateMethodPreview() {
        StringBuilder preview = new StringBuilder();

        if (isPublic.get()) preview.append("public ");
        else if (isPrivate.get()) preview.append("private ");
        else if (isProtected.get()) preview.append("protected ");
        if (isStatic.get()) preview.append("static ");
        if (isFinal.get()) preview.append("final ");
        if (isAbstract.get()) preview.append("abstract ");

        String name = methodName.get().isEmpty() ? "<name>" : methodName.get();

        if (descriptor.get().isEmpty()) {
            preview.append("void ").append(name).append("()");
        } else {
            try {
                Type methodType = Type.getMethodType(descriptor.get());
                preview.append(DescriptorValidator.typeToString(methodType.getReturnType())).append(" ");
                preview.append(name);

                Type[] argumentTypes = methodType.getArgumentTypes();
                preview.append("(");
                for (int i = 0; i < argumentTypes.length; i++) {
                    if (i > 0) preview.append(", ");
                    preview.append(DescriptorValidator.typeToString(argumentTypes[i])).append(" arg").append(i);
                }
                preview.append(")");
            } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                preview.append("<invalid> ").append(name).append(descriptor.get());
            }
        }

        if (!exceptions.get().isEmpty()) {
            preview.append(" throws ");
            String[] excArray = exceptions.get().split(",");
            for (int i = 0; i < excArray.length; i++) {
                if (i > 0) preview.append(", ");
                preview.append(shortExceptionName(excArray[i]));
            }
        }

        return preview.toString();
    }

    private String shortExceptionName(String fullName) {
        int lastSlash = fullName.lastIndexOf('/');
        if (lastSlash == -1) return fullName;
        return fullName.substring(lastSlash + 1);
    }

    private void updateMethod() {
        MethodNode methodNode = methodInput.getNode();
        methodNode.name = methodName.get();
        methodNode.desc = descriptor.get().isEmpty() ? "()V" : descriptor.get();
        methodNode.access = getAccessFlags();
        methodNode.exceptions.clear();
        if (!exceptions.get().isEmpty()) {
            methodNode.exceptions.addAll(Arrays.asList(exceptions.get().split(",")));
        }

        this.classInput.removeInput(this.methodInput);
        this.classInput.getNode().methods.add(methodNode);
        this.classInput.addInput(new MethodInput(methodNode, this.classInput));
    }

    private int getAccessFlags() {
        int access = 0;
        if (isPublic.get()) access |= Opcodes.ACC_PUBLIC;
        if (isPrivate.get()) access |= Opcodes.ACC_PRIVATE;
        if (isProtected.get()) access |= Opcodes.ACC_PROTECTED;
        if (isStatic.get()) access |= Opcodes.ACC_STATIC;
        if (isFinal.get()) access |= Opcodes.ACC_FINAL;
        if (isAbstract.get()) access |= Opcodes.ACC_ABSTRACT;
        return access;
    }
}