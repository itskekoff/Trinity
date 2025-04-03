package uwu.narumi.deobfuscator.core.other.impl.infinity;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.*;

public class InfinityConstantDeobfuscator extends Transformer {

    @Override
    protected void transform() throws Exception {
        for (ClassWrapper classWrapper : scopedClasses()) {
            if (hasDecryptMethod(classWrapper)) {
                processClass(classWrapper);
            }
        }
    }

    private boolean hasDecryptMethod(ClassWrapper classWrapper) {
        MethodNode staticBlock = classWrapper.findMethod("<clinit>", "()V").orElse(null);
        if (staticBlock == null) {
            return false;
        }
        for (AbstractInsnNode insn : staticBlock.instructions) {
            if (insn.getOpcode() == INVOKESTATIC) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (methodInsn.owner.equals(classWrapper.name()) && methodInsn.name.equals("decrypt") && methodInsn.desc.equals("()V")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void processClass(ClassWrapper classWrapper) {
        MethodNode decryptMethod = classWrapper.findMethod("decrypt", "()V").orElseThrow();
        Map<String, Integer> decryptionKeys = extractDecryptionKeys(classWrapper);
        Map<String, Object> fieldValues = analyzeDecryptMethod(decryptMethod, decryptionKeys);

        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            FieldNode field = classWrapper.findField(fieldName.split(":")[0], fieldName.split(":")[1]).orElseThrow();
            field.value = value;
        }

        classWrapper.methods().remove(decryptMethod);
        MethodNode staticBlock = classWrapper.findMethod("<clinit>", "()V").orElse(null);
        if (staticBlock != null) {
            classWrapper.methods().remove(staticBlock);
        }

        markChange();
    }

    private Map<String, Integer> extractDecryptionKeys(ClassWrapper classWrapper) {
        Map<String, Integer> keys = new HashMap<>();
        for (MethodNode method : classWrapper.methods()) {
            if ((method.access & ACC_STATIC) != 0 && method.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
                Integer key = findXorKey(method);
                if (key != null) {
                    keys.put(method.name, key);
                }
            }
        }
        return keys;
    }

    private Integer findXorKey(MethodNode method) {
        Set<Integer> possibleKeys = new HashSet<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() == IXOR) {
                AbstractInsnNode prev = insn.getPrevious();
                while (prev != null && !isPushInstruction(prev)) {
                    prev = prev.getPrevious();
                }
                if (prev != null) {
                    if (prev.getOpcode() == LDC && ((LdcInsnNode) prev).cst instanceof Integer) {
                        possibleKeys.add((Integer) ((LdcInsnNode) prev).cst);
                    } else if (prev.getOpcode() >= ICONST_M1 && prev.getOpcode() <= ICONST_5) {
                        possibleKeys.add(prev.getOpcode() - ICONST_0);
                    } else if (prev.getOpcode() == BIPUSH || prev.getOpcode() == SIPUSH) {
                        possibleKeys.add(((IntInsnNode) prev).operand);
                    }
                }
            }
        }
        return possibleKeys.size() == 1 ? possibleKeys.iterator().next() : null;
    }

    private boolean isPushInstruction(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return opcode == LDC || (opcode >= ICONST_M1 && opcode <= ICONST_5) || opcode == BIPUSH || opcode == SIPUSH;
    }

  private Map<String, Object> analyzeDecryptMethod(MethodNode decryptMethod, Map<String, Integer> decryptionKeys) {
    Map<String, Object> fieldValues = new HashMap<>();
    List<Object> stack = new ArrayList<>();

    for (AbstractInsnNode insn : decryptMethod.instructions) {
      switch (insn.getOpcode()) {
        case NOP:
        case GOTO:
        case RETURN:
          break;
        case LDC:
          stack.add(((LdcInsnNode) insn).cst);
          break;
        case ICONST_M1:
          stack.add(-1);
          break;
        case ICONST_0:
        case ICONST_1:
        case ICONST_2:
        case ICONST_3:
        case ICONST_4:
        case ICONST_5:
          stack.add(insn.getOpcode() - ICONST_0);
          break;
        case LCONST_0:
          stack.add(0L);
          break;
        case LCONST_1:
          stack.add(1L);
          break;
        case FCONST_0:
          stack.add(0.0f);
          break;
        case FCONST_1:
          stack.add(1.0f);
          break;
        case FCONST_2:
          stack.add(2.0f);
          break;
        case DCONST_0:
          stack.add(0.0);
          break;
        case DCONST_1:
          stack.add(1.0);
          break;
        case BIPUSH:
        case SIPUSH:
          stack.add(((IntInsnNode) insn).operand);
          break;
        case INVOKESTATIC:
          MethodInsnNode methodInsn = (MethodInsnNode) insn;
          if (decryptionKeys.containsKey(methodInsn.name)) {
            String encrypted = (String) stack.remove(stack.size() - 1);
            int key = decryptionKeys.get(methodInsn.name);
            String decrypted = decryptString(encrypted, key);
            stack.add(decrypted);
          } else if (methodInsn.owner.equals("java/lang/Double") && methodInsn.name.equals("longBitsToDouble")) {
            long bits = (Long) stack.remove(stack.size() - 1);
            stack.add(Double.longBitsToDouble(bits));
          } else if (methodInsn.owner.equals("java/lang/Float") && methodInsn.name.equals("intBitsToFloat")) {
            int bits = (Integer) stack.remove(stack.size() - 1);
            stack.add(Float.intBitsToFloat(bits));
          } else {
            System.err.println("Unexpected INVOKESTATIC: " + methodInsn.owner + "." + methodInsn.name);
          }
          break;
        case LXOR:
          long l2 = (Long) stack.remove(stack.size() - 1);
          long l1 = (Long) stack.remove(stack.size() - 1);
          stack.add(l1 ^ l2);
          break;
        case IXOR:
          int i2 = (Integer) stack.remove(stack.size() - 1);
          int i1 = (Integer) stack.remove(stack.size() - 1);
          stack.add(i1 ^ i2);
          break;
        case L2I:
          long l = (Long) stack.remove(stack.size() - 1);
          stack.add((int) l);
          break;
        case I2L:
          int i = (Integer) stack.remove(stack.size() - 1);
          stack.add((long) i);
          break;
        case PUTSTATIC:
          FieldInsnNode fieldInsn = (FieldInsnNode) insn;
          Object value = stack.remove(stack.size() - 1);
          fieldValues.put(fieldInsn.name + ":" + fieldInsn.desc, value);
          break;
        default:
          System.err.println("Unsupported opcode in decrypt method: " + insn.getOpcode());
          break;
      }
    }
    return fieldValues;
  }

    private String decryptString(String encrypted, int key) {
        StringBuilder sb = new StringBuilder();
        for (char c : encrypted.toCharArray()) {
            sb.append((char) (c ^ key));
        }
        return sb.toString();
    }
}