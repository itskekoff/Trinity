package me.f1nal.trinity.gui.windows.impl.refactor;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.compile.SafeClassWriter;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;
import org.checkerframework.checker.units.qual.A;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.*;
import uwu.narumi.deobfuscator.core.other.impl.universal.*;
import uwu.narumi.deobfuscator.core.other.impl.universal.flow.*;
import uwu.narumi.deobfuscator.core.other.impl.universal.number.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class DeobfuscatorWindow extends StaticWindow {
    private final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final List<Class<? extends Transformer>> cleanTransformers = new ArrayList<>();
    private final List<Class<? extends Transformer>> universalTransformers = new ArrayList<>();
    private final List<Class<? extends Transformer>> flowTransformers = new ArrayList<>();
    private final List<Class<? extends Transformer>> numberTransformers = new ArrayList<>();
    private final Map<Class<? extends Transformer>, ImBoolean> selectedTransformers = new HashMap<>();
    private final LinkedList<Class<? extends Transformer>> selectedOrder = new LinkedList<>();

    private DeobfuscationStatus status = DeobfuscationStatus.WAITING;
    public static AtomicInteger changes = new AtomicInteger();

    private int currentProcessed = 0;
    private int totalClasses = 0;

    public DeobfuscatorWindow(Trinity trinity) {
        super("Deobfuscator", 0, 0, trinity);
        this.windowFlags |= ImGuiWindowFlags.NoResize;

        initializeTransformers();
    }

    private void initializeTransformers() {
        // Группа "clean"
        cleanTransformers.add(AnnotationCleanTransformer.class);
        cleanTransformers.add(ClassDebugInfoCleanTransformer.class);
        cleanTransformers.add(InvalidMethodCleanTransformer.class);
        cleanTransformers.add(LineNumberCleanTransformer.class);
        cleanTransformers.add(LocalVariableNamesCleanTransformer.class);
        cleanTransformers.add(MethodDebugInfoCleanTransformer.class);
        cleanTransformers.add(ParametersInfoCleanTransformer.class);
        cleanTransformers.add(SignatureCleanTransformer.class);
        cleanTransformers.add(ThrowsExceptionCleanTransformer.class);
        cleanTransformers.add(TryCatchBlockCleanTransformer.class);
        cleanTransformers.add(UnknownAttributeCleanTransformer.class);

        // Группа "universal" - подгруппа "flow"
        flowTransformers.add(CleanRedundantJumpsTransformer.class);
        flowTransformers.add(CleanRedundantSwitchesTransformer.class);
        flowTransformers.add(UniversalFlowTransformer.class);

        // Группа "universal" - подгруппа "number"
        numberTransformers.add(InlineConstantValuesTransformer.class);
        numberTransformers.add(UniversalNumberTransformer.class);

        // Группа "universal" - остальные трансформеры
        universalTransformers.add(AccessRepairTransformer.class);
        universalTransformers.add(AnnotationFilterTransformer.class);
        universalTransformers.add(RecoverSyntheticsTransformer.class);
        universalTransformers.add(TryCatchRepairTransformer.class);

        // Инициализация состояния выбора (по умолчанию все выключены)
        for (Class<? extends Transformer> transformer : cleanTransformers) {
            selectedTransformers.put(transformer, new ImBoolean(false));
        }
        for (Class<? extends Transformer> transformer : flowTransformers) {
            selectedTransformers.put(transformer, new ImBoolean(false));
        }
        for (Class<? extends Transformer> transformer : numberTransformers) {
            selectedTransformers.put(transformer, new ImBoolean(false));
        }
        for (Class<? extends Transformer> transformer : universalTransformers) {
            selectedTransformers.put(transformer, new ImBoolean(false));
        }
    }

    @Override
    protected void renderFrame() {
        ImGui.text("Select transformers");
        ImGui.sameLine();
        ImGui.dummy(500.F, 0.F);
        ImGui.sameLine();
        ImGui.textDisabled("Author: narumii");

        if (ImGui.beginChild("TransformerListChild", 200.F, 350.F)) {
            if (ImGui.treeNodeEx("Clean Transformers", ImGuiTreeNodeFlags.DefaultOpen)) {
                for (Class<? extends Transformer> transformer : cleanTransformers) {
                    ImBoolean selected = selectedTransformers.get(transformer);
                    boolean wasSelected = selected.get();
                    if (ImGui.checkbox(formatTransformerName(transformer), selected)) {
                        if (selected.get() && !wasSelected) {
                            selectedOrder.add(transformer); // Добавляем в порядок при выборе
                        } else if (!selected.get() && wasSelected) {
                            selectedOrder.remove(transformer); // Удаляем при снятии выбора
                        }
                    }
                }
                ImGui.treePop();
            }

            if (ImGui.treeNodeEx("Universal Transformers", ImGuiTreeNodeFlags.DefaultOpen)) {
                if (ImGui.treeNodeEx("Flow", ImGuiTreeNodeFlags.DefaultOpen)) {
                    for (Class<? extends Transformer> transformer : flowTransformers) {
                        ImBoolean selected = selectedTransformers.get(transformer);
                        boolean wasSelected = selected.get();
                        if (ImGui.checkbox(formatTransformerName(transformer), selected)) {
                            if (selected.get() && !wasSelected) {
                                selectedOrder.add(transformer);
                            } else if (!selected.get() && wasSelected) {
                                selectedOrder.remove(transformer);
                            }
                        }
                    }
                    ImGui.treePop();
                }

                if (ImGui.treeNodeEx("Number", ImGuiTreeNodeFlags.DefaultOpen)) {
                    for (Class<? extends Transformer> transformer : numberTransformers) {
                        ImBoolean selected = selectedTransformers.get(transformer);
                        boolean wasSelected = selected.get();
                        if (ImGui.checkbox(formatTransformerName(transformer), selected)) {
                            if (selected.get() && !wasSelected) {
                                selectedOrder.add(transformer);
                            } else if (!selected.get() && wasSelected) {
                                selectedOrder.remove(transformer);
                            }
                        }
                    }
                    ImGui.treePop();
                }

                for (Class<? extends Transformer> transformer : universalTransformers) {
                    ImBoolean selected = selectedTransformers.get(transformer);
                    boolean wasSelected = selected.get();
                    if (ImGui.checkbox(formatTransformerName(transformer), selected)) {
                        if (selected.get() && !wasSelected) {
                            selectedOrder.add(transformer);
                        } else if (!selected.get() && wasSelected) {
                            selectedOrder.remove(transformer);
                        }
                    }
                }
                ImGui.treePop();
            }
        }
        ImGui.endChild();

        ImGui.sameLine();

        ImGui.beginGroup();
        if (ImGui.beginChild("DeobfuscatorControlsChild", 480.F, 350.F)) {
            ImGui.pushStyleColor(ImGuiCol.Text, CodeColorScheme.TEXT);
            ImGui.textWrapped("Select transformers to apply deobfuscation to the loaded classes.");
            ImGui.popStyleColor();

            ImGui.separator();

            ImGui.text("Selected Transformers:");
            if (selectedOrder.isEmpty()) {
                ImGui.textDisabled("None selected");
            } else {
                for (int i = 0; i < selectedOrder.size(); i++) {
                    ImGui.text(String.format("%d. %s", i + 1, formatTransformerName(selectedOrder.get(i))));
                }
            }

            ImGui.dummy(0.F, 350.F - ImGui.getCursorPosY() - 30.F);
            ImGui.separator();

            ImGui.beginDisabled(!(this.status == DeobfuscationStatus.WAITING || this.status == DeobfuscationStatus.DONE));
            if (ImGui.button("Run Deobfuscation")) {
                executor.submit(this::runDeobfuscation);
            }
            ImGui.endDisabled();
            ImGui.sameLine();
            if (ImGui.button("Select All")) {
                selectAllTransformers(true);
            }
            ImGui.sameLine();
            if (ImGui.button("Deselect All")) {
                selectAllTransformers(false);
            }
            ImGui.sameLine();
            ImGui.text(status.getDisplayText(currentProcessed, totalClasses, changes.get()));
        }
        ImGui.endChild();
        ImGui.endGroup();
    }

    private void runDeobfuscation() {
        changes.set(0);
        currentProcessed = 0;

        List<Class<? extends Transformer>> selectedClasses = getSelectedTransformerClasses();
        if (selectedClasses.isEmpty()) {
            Main.getDisplayManager().addNotification(new Notification(NotificationType.WARNING, () -> "Narumii Deobfuscator",
                    ColoredStringBuilder.create().fmt("No transformers selected!").get()));
            return;
        }

        setStatus(DeobfuscationStatus.LOADING_CLASSES);
        totalClasses = trinity.getExecution().getClassList().size();

        Deobfuscator deobfuscator = Deobfuscator.from(
                DeobfuscatorOptions.builder()
                        .outputJar(Path.of("xueta.jar"))
                        .inputJar(Path.of("shit.jar"))
                        .continueOnError()
                        .build()
        );

        trinity.getExecution().getClassList().forEach(classInput -> {
            SafeClassWriter classWriter = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, this::getType, null);
            classInput.getNode().accept(classWriter);
            deobfuscator.getContext().addCompiledClass("", classWriter.toByteArray());
            currentProcessed++;
        });

        currentProcessed = 0;

        setStatus(DeobfuscationStatus.PROCESSING);
        for (int i = 0; i < totalClasses; i++) {
            currentProcessed = i + 1;
            deobfuscator.transform(getSelectedTransformerSuppliers());
        }

        setStatus(DeobfuscationStatus.DONE);

        deobfuscator.getContext().getClassesMap().values().forEach(classWrapper -> {
            ClassNode node = classWrapper.classNode();
            ClassInput input = trinity.getExecution().getClassInput(node.name);

            if (input != null) {
                ClassTarget classTarget = new ClassTarget(input.getRealName(), input.getSize());
                this.trinity.getExecution().getClassList().remove(input);
                this.trinity.getExecution().removeClassTarget(input.getClassTarget());

                input.setNode(node);
                classTarget.setInput(input);

                this.trinity.getExecution().addClassTarget(classTarget);
                this.trinity.getExecution().getClassList().add(input);

                trinity.getEventManager().postEvent(new EventClassModified(input));
            }
        });

        Main.getDisplayManager().addNotification(new Notification(NotificationType.INFO, () -> "Narumii Deobfuscator",
                ColoredStringBuilder.create().fmt("Deobfuscation completed. Processed {} classes with {} transformers.", totalClasses, selectedClasses.size()).get()));
    }

    private void selectAllTransformers(boolean state) {
        selectedTransformers.values().forEach(imBoolean -> imBoolean.set(state));
        if (state) {
            selectedOrder.clear();
            selectedOrder.addAll(cleanTransformers);
            selectedOrder.addAll(flowTransformers);
            selectedOrder.addAll(numberTransformers);
            selectedOrder.addAll(universalTransformers);
        } else {
            selectedOrder.clear();
        }
    }

    public List<Class<? extends Transformer>> getSelectedTransformerClasses() {
        return new ArrayList<>(selectedOrder); // Возвращаем в порядке выбора
    }

    private String formatTransformerName(Class<? extends Transformer> transformer) {
        return transformer.getSimpleName().split("Transformer")[0];
    }

    public void setStatus(DeobfuscationStatus status) {
        this.status = status;
        if (status == DeobfuscationStatus.LOADING_CLASSES || status == DeobfuscationStatus.ERROR) {
            currentProcessed = 0;
            totalClasses = 0;
        }
    }

    public List<Supplier<Transformer>> getSelectedTransformerSuppliers() {
        List<Supplier<Transformer>> suppliers = new ArrayList<>();
        for (Class<? extends Transformer> transformerClass : selectedOrder) {
            suppliers.add(() -> {
                try {
                    return transformerClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate " + transformerClass.getSimpleName(), e);
                }
            });
        }
        return suppliers;
    }

    public static AtomicInteger getChanges() {
        return changes;
    }

    private ClassNode getType(String typeName) {
        ClassInput classInput = trinity.getExecution().getClassInput(typeName);
        if (classInput != null) {
            return classInput.getNode();
        }
        return trinity.getJrtInput().getClass(typeName);
    }

    public enum DeobfuscationStatus {
        WAITING("Waiting for start"),
        LOADING_CLASSES("Loading Classes (%d/%d)"),
        PROCESSING("Processing (%d/%d)"),
        DONE("Changes -> %d"),
        ERROR("Error");

        private final String format;

        DeobfuscationStatus(String format) {
            this.format = format;
        }

        public String getDisplayText(int current, int total, int totalChanges) {
            if (this == PROCESSING || this == LOADING_CLASSES) {
                return String.format(format, current, total);
            } else if (this == DONE) {
                return String.format(format, totalChanges);
            }
            return format;
        }
    }
}