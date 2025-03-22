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
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.gui.windows.impl.classstructure.popup.utils.DescriptorValidator;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author itskekoff
 * @since 20:15 of 22.03.2025
 */
public class AddFieldPopup extends PopupWindow {
    private final ImString fieldName = new ImString(32);
    private final ImString fieldType = new ImString(64);
    private final ImString initialValue = new ImString(64);
    private final ImBoolean isPublic = new ImBoolean(true);
    private final ImBoolean isPrivate = new ImBoolean(false);
    private final ImBoolean isProtected = new ImBoolean(false);
    private final ImBoolean isStatic = new ImBoolean(false);
    private final ImBoolean isFinal = new ImBoolean(false);
    private final ImBoolean isVolatile = new ImBoolean(false);

    private final ClassInput classInput;
    private final Trinity trinity;

    public AddFieldPopup(ClassInput classInput, Trinity trinity) {
        super("Add New Field", trinity);
        this.classInput = classInput;
        this.trinity = trinity;
    }

    @Override
    protected void renderFrame() {
        ImGui.text("Field Name:");
        ImGui.sameLine();
        ImGui.inputText("##fieldName", fieldName, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.separator();

        ImGui.text("Field Type:");
        ImGui.sameLine();
        ImGui.inputText("##fieldType", fieldType, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.textDisabled("(e.g., I for int, Ljava/util/List; for List)");

        ImGui.text("Initial Value:");
        ImGui.sameLine();
        ImGui.inputText("##initialValue", initialValue, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.textDisabled("(e.g., 42 for int, \"text\" for String, optional)");
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
        ImGui.checkbox("Volatile", isVolatile);
        ImGui.separator();

        StringBuilder error = new StringBuilder();
        boolean isValid = DescriptorValidator.validateFieldDescriptor(fieldType.get(), error);
        DescriptorValidator.renderError(error.toString());

        if (ImGui.button("Add Field") && isValid && !fieldName.get().isEmpty()) {
            FieldNode fieldNode = this.createField();
            this.classInput.getNode().fields.add(fieldNode);
            this.classInput.addInput(new FieldInput(fieldNode, this.classInput));
            trinity.getEventManager().postEvent(new EventClassModified(this.classInput));
            Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, () -> "Field generator", ColoredStringBuilder.create()
                    .fmt("Created field {} in {}", generateFieldPreview(), this.classInput.getDisplaySimpleName()).get()));
            Main.getWindowManager().getWindowsOfType(DecompilerWindow.class).forEach(DecompilerWindow::updateClassStructure);

            close();
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) {
            close();
        }

        ImGui.textDisabled("Field preview: " + generateFieldPreview());
    }

    private String generateFieldPreview() {
        StringBuilder preview = new StringBuilder();

        if (isPublic.get()) preview.append("public ");
        else if (isPrivate.get()) preview.append("private ");
        else if (isProtected.get()) preview.append("protected ");
        if (isStatic.get()) preview.append("static ");
        if (isFinal.get()) preview.append("final ");
        if (isVolatile.get()) preview.append("volatile ");

        String typeStr;
        if (fieldType.get().isEmpty()) {
            typeStr = "<type>";
        } else {
            try {
                Type type = Type.getType(fieldType.get());
                typeStr = DescriptorValidator.typeToString(type);
            } catch (StringIndexOutOfBoundsException | IllegalArgumentException e) {
                typeStr = "<invalid>";
            }
        }

        String name = fieldName.get().isEmpty() ? "<name>" : fieldName.get();
        preview.append(typeStr).append(" ").append(name);

        if (!initialValue.get().isEmpty()) {
            preview.append(" = ").append(initialValue.get());
        }

        preview.append(";");

        return preview.toString();
    }

    public String getFieldName() {
        return fieldName.get();
    }

    public String getFieldType() {
        return fieldType.get();
    }

    public int getAccessFlags() {
        int access = 0;
        if (isPublic.get()) access |= Opcodes.ACC_PUBLIC;
        if (isPrivate.get()) access |= Opcodes.ACC_PRIVATE;
        if (isProtected.get()) access |= Opcodes.ACC_PROTECTED;
        if (isStatic.get()) access |= Opcodes.ACC_STATIC;
        if (isFinal.get()) access |= Opcodes.ACC_FINAL;
        if (isVolatile.get()) access |= Opcodes.ACC_VOLATILE;
        return access;
    }

    public Object getInitialValue() {
        if (initialValue.get().isEmpty()) return null;
        try {
            Type type = Type.getType(fieldType.get().isEmpty() ? "I" : fieldType.get());
            return switch (type.getSort()) {
                case Type.INT -> Integer.parseInt(initialValue.get());
                case Type.LONG -> Long.parseLong(initialValue.get());
                case Type.FLOAT -> Float.parseFloat(initialValue.get());
                case Type.DOUBLE -> Double.parseDouble(initialValue.get());
                case Type.BOOLEAN -> Boolean.parseBoolean(initialValue.get());
                case Type.OBJECT -> initialValue.get();
                default -> null;
            };
        } catch (Exception e) {
            return initialValue.get();
        }
    }

    private FieldNode createField() {
        String name = this.getFieldName();
        String desc = this.getFieldType().isEmpty() ? "I" : this.getFieldType();
        int access = this.getAccessFlags();
        Object value = this.getInitialValue();

        return new FieldNode(access, name, desc, null, value);
    }
}