package me.f1nal.trinity.gui.windows.impl.classstructure.popup;

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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author itskekoff
 * @since 20:15 of 22.03.2025
 */
public class AddMethodPopup extends PopupWindow {
    private final ImString methodName = new ImString(32);
    private final ImString descriptor = new ImString(64);
    private final ImString exceptions = new ImString(128);
    private final ImBoolean isPublic = new ImBoolean(true);
    private final ImBoolean isPrivate = new ImBoolean(false);
    private final ImBoolean isProtected = new ImBoolean(false);
    private final ImBoolean isStatic = new ImBoolean(false);
    private final ImBoolean isFinal = new ImBoolean(false);
    private final ImBoolean isAbstract = new ImBoolean(false);

    private final ClassInput classInput;
    private final Trinity trinity;

    public AddMethodPopup(ClassInput classInput, Trinity trinity) {
        super("Add new method in %s".formatted(classInput.getDisplayName().getName()), trinity);

        this.classInput = classInput;
        this.trinity = trinity;
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

        if (ImGui.button("Add Method") && isValid && !methodName.get().isEmpty()) {
            MethodNode methodNode = this.createMethod();
            this.classInput.getNode().methods.add(methodNode);
            this.classInput.addInput(new MethodInput(methodNode, this.classInput));
            trinity.getEventManager().postEvent(new EventClassModified(this.classInput));
            Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, () -> "Method generator", ColoredStringBuilder.create()
                    .fmt("Created method {} in {}", generateMethodPreview(), this.classInput.getDisplayName().getName()).get()));
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
            } catch (StringIndexOutOfBoundsException | IllegalArgumentException e) {
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

    public String getMethodName() {
        return methodName.get();
    }

    public String getDescriptor() {
        return descriptor.get();
    }

    public int getAccessFlags() {
        int access = 0;
        if (isPublic.get()) access |= org.objectweb.asm.Opcodes.ACC_PUBLIC;
        if (isPrivate.get()) access |= org.objectweb.asm.Opcodes.ACC_PRIVATE;
        if (isProtected.get()) access |= org.objectweb.asm.Opcodes.ACC_PROTECTED;
        if (isStatic.get()) access |= org.objectweb.asm.Opcodes.ACC_STATIC;
        if (isFinal.get()) access |= org.objectweb.asm.Opcodes.ACC_FINAL;
        if (isAbstract.get()) access |= org.objectweb.asm.Opcodes.ACC_ABSTRACT;
        return access;
    }

    public String[] getExceptions() {
        if (exceptions.get().isEmpty()) return null;
        return exceptions.get().split(",");
    }

    private MethodNode createMethod() {
        String name = this.getMethodName();
        String desc = this.getDescriptor();
        int access = this.getAccessFlags();
        String[] exceptions = this.getExceptions();

        MethodNode methodNode = new MethodNode(access, name, desc, null, exceptions);

        InsnList instructions = methodNode.instructions;
        Type returnType = Type.getReturnType(desc);
        switch (returnType.getSort()) {
            case Type.INT: case Type.BYTE: case Type.SHORT: case Type.CHAR: case Type.BOOLEAN:
                instructions.add(new InsnNode(Opcodes.ICONST_0));
                instructions.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.LONG:
                instructions.add(new InsnNode(Opcodes.LCONST_0));
                instructions.add(new InsnNode(Opcodes.LRETURN));
                break;
            case Type.FLOAT:
                instructions.add(new InsnNode(Opcodes.FCONST_0));
                instructions.add(new InsnNode(Opcodes.FRETURN));
                break;
            case Type.DOUBLE:
                instructions.add(new InsnNode(Opcodes.DCONST_0));
                instructions.add(new InsnNode(Opcodes.DRETURN));
                break;
            case Type.OBJECT: case Type.ARRAY:
                instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                instructions.add(new InsnNode(Opcodes.ARETURN));
                break;
            default:
                instructions.add(new InsnNode(Opcodes.RETURN));
        }

        methodNode.maxStack = 1;
        methodNode.maxLocals = Type.getArgumentTypes(desc).length + (access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;

        return methodNode;
    }
}