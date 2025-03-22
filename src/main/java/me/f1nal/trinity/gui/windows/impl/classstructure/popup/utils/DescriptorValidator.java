package me.f1nal.trinity.gui.windows.impl.classstructure.popup.utils;

import imgui.ImColor;
import imgui.ImGui;
import org.objectweb.asm.Type;

public class DescriptorValidator {
    public static boolean validateMethodDescriptor(String descriptor, StringBuilder error) {
        if (descriptor.isEmpty()) {
            return true;
        }
        try {
            Type.getMethodType(descriptor);
            return true;
        } catch (IllegalArgumentException e) {
            error.append("Invalid method descriptor: ").append(e.getMessage());
            return false;
        }
    }

    public static boolean validateFieldDescriptor(String descriptor, StringBuilder error) {
        if (descriptor.isEmpty()) {
            return true;
        }
        try {
            Type.getType(descriptor);
            return true;
        } catch (IllegalArgumentException e) {
            error.append("Invalid field descriptor: ").append(e.getMessage());
            return false;
        }
    }

    public static String typeToString(Type type) {
        return switch (type.getSort()) {
            case Type.VOID -> "void";
            case Type.BOOLEAN -> "boolean";
            case Type.CHAR -> "char";
            case Type.BYTE -> "byte";
            case Type.SHORT -> "short";
            case Type.INT -> "int";
            case Type.FLOAT -> "float";
            case Type.LONG -> "long";
            case Type.DOUBLE -> "double";
            case Type.ARRAY -> typeToString(type.getElementType()) + "[]".repeat(type.getDimensions());
            case Type.OBJECT -> {
                String className = type.getClassName();
                yield className.substring(className.lastIndexOf('.') + 1);
            }
            default -> type.getDescriptor();
        };
    }

    public static void renderError(String error) {
        if (!error.isEmpty()) {
            ImGui.textColored(ImColor.rgb(245, 80, 80), error);
        }
    }
}