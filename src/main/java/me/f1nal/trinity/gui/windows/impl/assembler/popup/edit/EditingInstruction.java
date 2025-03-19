package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.flag.ImGuiDataType;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EditingInstruction {
    private final AbstractInsnNode insnNode;
    private final List<EditField<?>> editFieldList = new ArrayList<>();
    private final Trinity trinity;
    /**
     * If this instruction data is valid and can be set.
     */
    private boolean valid;

    public EditingInstruction(Trinity trinity, Map<LabelNode, LabelNode> labelMap, AbstractInsnNode insnNode) {
        this.trinity = trinity;
        this.insnNode = insnNode.clone(labelMap);
    }

    public AbstractInsnNode getInsnNode() {
        return insnNode;
    }

    public boolean isValid() {
        return valid;
    }

    public List<EditField<?>> getEditFieldList() {
        return editFieldList;
    }

    public void update() {
        this.valid = this.computeValid();
    }

    public void addInstructionFields(MethodInput methodInput) {
        Map<Class<?>, Consumer<AbstractInsnNode>> handlers = new HashMap<>();

        addHandler(handlers, TypeInsnNode.class, typeInsn -> {
            addField(new EditFieldDescriptor(() -> typeInsn.desc, desc -> typeInsn.desc = desc));
        });

        addHandler(handlers, IntInsnNode.class, intInsn -> addField(new EditFieldInteger("Operand",
                () -> intInsn.operand, operand -> intInsn.operand = operand, ImGuiDataType.S32)));

        addHandler(handlers, VarInsnNode.class, varInsn -> {
            addField(new EditFieldVariable(methodInput.getVariableTable(),
                    () -> methodInput.getVariableTable().getVariable(varInsn.var),
                    variable -> varInsn.var = variable.findIndex()));
        });

        addHandler(handlers, IincInsnNode.class, iincInsn -> {
            addField(new EditFieldVariable(methodInput.getVariableTable(),
                    () -> methodInput.getVariableTable().getVariable(iincInsn.var),
                    variable -> iincInsn.var = variable.findIndex()));
            addField(new EditFieldInteger("Increment", () -> iincInsn.incr, incr -> iincInsn.incr = incr, ImGuiDataType.S32));
        });

        addHandler(handlers, MultiANewArrayInsnNode.class, multiANewArrayInsn -> {
            addField(new EditFieldDescriptor(() -> multiANewArrayInsn.desc, desc -> multiANewArrayInsn.desc = desc));
            addField(new EditFieldInteger("Dimensions",
                    () -> multiANewArrayInsn.dims, dim -> multiANewArrayInsn.dims = dim, ImGuiDataType.U8));
        });

        addHandler(handlers, JumpInsnNode.class, jumpInsn ->
                addField(new EditFieldLabel(methodInput.getLabelTable(),
                        () -> methodInput.getLabelTable().getLabel(jumpInsn.label.getLabel()),
                        label -> jumpInsn.label = new LabelNode(label.findOriginal()))));

        addHandler(handlers, MethodInsnNode.class, methodInsn -> {
            addField(new EditFieldClass(trinity, "Owner",
                    () -> methodInsn.owner, owner -> methodInsn.owner = owner));
            addField(new EditFieldString(512, "Name", "toString",
                    () -> methodInsn.name, name -> methodInsn.name = name));
            addField(new EditFieldString(512, "Desc", "()Ljava/lang/String",
                    () -> methodInsn.desc, desc -> methodInsn.desc = desc));
        });

        addHandler(handlers, FieldInsnNode.class, fieldInsn -> {
            addField(new EditFieldClass(trinity, "Owner",
                    () -> fieldInsn.owner, owner -> fieldInsn.owner = owner).setHint("java/lang/String"));
            addField(new EditFieldString(512, "Name", "value",
                    () -> fieldInsn.name, name -> fieldInsn.name = name));
            addField(new EditFieldString(512, "Desc", "[B",
                    () -> fieldInsn.desc, desc -> fieldInsn.desc = desc));
        });
        addHandler(handlers, LdcInsnNode.class, ldcInsn ->
                addField(new EditFieldObject<>("Constant", () -> ldcInsn.cst, cst -> ldcInsn.cst = cst)));

        /*
        // idk how to make correct
        addHandler(handlers, InvokeDynamicInsnNode.class, indyInsn -> {
            addField(new EditFieldString(512, "Name", "InvokeDynamic name",
                    () -> indyInsn.name, name -> indyInsn.name = name));
            addField(new EditFieldString(512, "Desc", "InvokeDynamic description",
                    () -> indyInsn.desc, desc -> indyInsn.desc = desc));
            addField(new EditFieldHandle("Bootstrap Method",
                    () -> indyInsn.bsm, bsm -> indyInsn.bsm = bsm));
        });
         */

        addHandler(handlers, FrameNode.class, frameInsn ->
                addField(new EditFieldInteger("Type",
                        () -> frameInsn.type, type -> frameInsn.type = type, ImGuiDataType.S32)));
        addHandler(handlers, LineNumberNode.class, lineInsn ->
                addField(new EditFieldInteger("Line",
                        () -> lineInsn.line, lineNum -> lineInsn.line = lineNum, ImGuiDataType.S32)));

        Consumer<AbstractInsnNode> handler = handlers.get(insnNode.getClass());
        if (handler != null) {
            handler.accept(insnNode);
        }

        this.update();
    }

    private <T extends AbstractInsnNode> void addHandler(Map<Class<?>, Consumer<AbstractInsnNode>> handlers, Class<T> clazz,
                                                         Consumer<T> handler) {
        handlers.put(clazz, insn -> {
            if (clazz.isInstance(insn)) {
                handler.accept(clazz.cast(insn));
            }
        });
    }
    private void addField(EditField<?> field) {
        editFieldList.add(field);
        field.setUpdateEvent(this::update);
    }

    private boolean computeValid() {
        for (EditField<?> editField : editFieldList) {
            if (!editField.isValidInput()) {
                return false;
            }
        }
        return true;
    }
}
