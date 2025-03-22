package me.f1nal.trinity.gui.windows.impl.classstructure.nodes;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.main.ClassesProcessor;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.execution.*;
import me.f1nal.trinity.execution.access.SimpleAccessFlagsMaskProvider;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.windows.impl.classstructure.popup.edit.EditFieldPopup;
import me.f1nal.trinity.gui.windows.impl.classstructure.popup.edit.EditMethodPopup;
import me.f1nal.trinity.gui.windows.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.NameUtil;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractClassStructureNodeInput<I extends Input> extends ClassStructureNode {
    private final I input;

    protected AbstractClassStructureNodeInput(String icon, I input) {
        super(icon);
        this.input = input;
    }

    @Override
    protected void populatePopup(PopupItemBuilder popup) {
        Trinity trinity = Main.getTrinity();
        popup.separator();

        getInput().populatePopup(popup);
        if (!(this.input instanceof ClassInput)) {
            popup.separator();
            switch (this.input.getNode().getClass().getSimpleName()) {
                case "MethodNode" -> popup.menuItem("Edit method", () ->
                        Main.getDisplayManager().getWindowManager().addPopup(new EditMethodPopup((MethodInput) this.input, trinity)));
                case "FieldNode" -> popup.menuItem("Edit field", () ->
                        Main.getDisplayManager().getWindowManager().addPopup(new EditFieldPopup((FieldInput) this.input, trinity)));
            }

            popup.menuItem("Remove", () -> {
                ClassInput owner = this.input.getOwningClass();
                owner.removeInput((MemberInput<?>) this.input);
                Main.getTrinity().getEventManager().postEvent(new EventClassModified(this.input.getOwningClass()));
                Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, () -> "Class Structure", ColoredStringBuilder.create()
                        .fmt("Removed {} in {}", this.input.getDisplayName().getName(), owner.getDisplayName().getName()).get()));

                Main.getWindowManager().getWindowsOfType(DecompilerWindow.class).forEach(DecompilerWindow::updateClassStructure);
            });
        }
    }

    @Override
    protected void handleLeftClick() {
        Main.getDisplayManager().openDecompilerView(this.input);
    }

    @Override
    protected final RenameHandler getRenameFunction() {
        return (r, newName) -> this.getInput().rename(r, newName);
    }

    protected abstract void appendType(ColoredStringBuilder text, String suffix);
    protected abstract void appendParameters(ColoredStringBuilder text);

    protected final void appendReturnType(ColoredStringBuilder text, String descriptor, String suffix) {
        final Type type = Type.getType(descriptor);
        final int sort = type.getSort();

        if (sort == Type.ARRAY || sort == Type.OBJECT) {
            // For referencing
            String className = NameUtil.internalToNormal((sort == Type.ARRAY ? type.getElementType() : type).getClassName());
            ClassTarget classTarget = Main.getTrinity().getExecution().getClassTarget(className);
            StringBuilder formattedName = new StringBuilder(classTarget == null ? NameUtil.getSimpleName(className) : classTarget.getDisplaySimpleName());

            if (sort == Type.ARRAY) {
                for (int i = 0, dimensions = type.getDimensions(); i < dimensions; i++) {
                    formattedName.append("[]");
                }
            }

            text.text(CodeColorScheme.CLASS_REF, formattedName.toString());
        } else {
            text.text(CodeColorScheme.KEYWORD, type.getClassName());
        }

        text.text(CodeColorScheme.DISABLED, suffix);
    }

    @Override
    protected BrowserViewerNode createBrowserViewerNode() {
        BrowserViewerNode node = super.createBrowserViewerNode();
        AccessFlags accessFlags = new AccessFlags(new SimpleAccessFlagsMaskProvider(getInput().getAccessFlagsMask()));
        node.setPrefix(safeText(prefix -> {
            appendAccessFlags(prefix, accessFlags);
            appendType(prefix, " ");
        }));
        node.setSuffix(safeText(this::appendParameters));
        return node;
    }

    private List<ColoredString> safeText(Consumer<ColoredStringBuilder> builder) {
        ColoredStringBuilder csb = ColoredStringBuilder.create();
        try {
            builder.accept(csb);
        } catch (Throwable throwable) {
            Logging.warn("Decoding type of '{}': {}", getInput().toString(), throwable);
            return ColoredStringBuilder.create().text(CodeColorScheme.NOTIFY_ERROR, "<ERROR>").get();
        }
        return csb.get();
    }

    protected void appendAccessFlags(ColoredStringBuilder text, AccessFlags accessFlags) {
        AccessFlags.Flag[] flagsArray = AccessFlags.getFlags();
        for (AccessFlags.Flag flag : flagsArray) {
            if (accessFlags.isFlag(flag) && this.getInput().isAccessFlagValid(flag)) {
                text.text(CodeColorScheme.DISABLED, flag.getName().toLowerCase() + " ");
            }
        }
    }

    public final I getInput() {
        return input;
    }
}
