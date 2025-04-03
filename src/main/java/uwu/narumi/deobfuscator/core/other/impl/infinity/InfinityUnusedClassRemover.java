package uwu.narumi.deobfuscator.core.other.impl.infinity;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

public class InfinityUnusedClassRemover extends Transformer {

    @Override
    protected void transform() throws Exception {
        Set<String> usedFields = collectUsedFields();

        for (ClassWrapper classWrapper : scopedClasses().toArray(new ClassWrapper[0])) {
            if (isRemovable(classWrapper, usedFields)) {
                context().getClassesMap().remove(classWrapper.name());
                markChange();
            }
        }
    }

    private Set<String> collectUsedFields() {
        Set<String> usedFields = new HashSet<>();
        for (ClassWrapper classWrapper : scopedClasses()) {
            for (MethodNode method : classWrapper.methods()) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn.getOpcode() == GETSTATIC) {
                        FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                        usedFields.add(fieldInsn.owner + "." + fieldInsn.name);
                    }
                }
            }
        }
        return usedFields;
    }

    private boolean isRemovable(ClassWrapper classWrapper, Set<String> usedFields) {
        // Проверяем, что в классе только поля и нет методов (кроме <init> или <clinit>, если он пуст)
        if (classWrapper.methods().stream()
                .anyMatch(method -> !method.name.equals("<init>") && !method.name.equals("<clinit>"))) {
            return false;
        }

        // Проверяем, что все поля — это примитивы или строки
        for (FieldNode field : classWrapper.fields()) {
            String desc = field.desc;
            if (!desc.equals("I") && !desc.equals("J") && !desc.equals("F") && !desc.equals("D") && !desc.equals("Ljava/lang/String;")) {
                return false; // Не примитив и не строка
            }
        }

        for (FieldNode field : classWrapper.fields()) {
            String fieldRef = classWrapper.name() + "." + field.name;
            if (usedFields.contains(fieldRef)) {
                return false; // Поле используется
            }
        }

        return true;
    }
}