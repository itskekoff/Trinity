package me.f1nal.trinity.gui.windows.impl.entryviewer.impl;

import imgui.ImGui;
import imgui.extension.texteditor.TextEditor;
import imgui.extension.texteditor.TextEditorLanguageDefinition;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;

import java.awt.*;

public class TextEditorWindow extends ArchiveEntryViewerWindow<ResourceArchiveEntry> implements ICaption {
    private final TextEditor textEditor = new TextEditor();

    public TextEditorWindow(Trinity trinity, ResourceArchiveEntry archiveEntry) {
        super(trinity, archiveEntry);
        textEditor.setLanguageDefinition(new TextEditorLanguageDefinition());
        textEditor.setPalette(this.getPalette());
        textEditor.insertText(new String(archiveEntry.getBytes()));
        textEditor.setShowWhitespaces(false);
        textEditor.setColorizerEnable(true);
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
    }

    @Override
    protected void renderFrame() {
        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Save")) {
                    this.saveBytes(textEditor.getText().getBytes(), this);
                }

                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Edit")) {
                final boolean ro = textEditor.isReadOnly();
                if (ImGui.menuItem("Read-only mode", "", ro)) {
                    textEditor.setReadOnly(!ro);
                }

                ImGui.separator();

                if (ImGui.menuItem("Undo", "Ctrl+Z", !ro && textEditor.canUndo())) {
                    textEditor.undo(1);
                }
                if (ImGui.menuItem("Redo", "Ctrl+Y", !ro && textEditor.canRedo())) {
                    textEditor.redo(1);
                }

                ImGui.separator();

                if (ImGui.menuItem("Copy", "Ctrl+C", textEditor.hasSelection())) {
                    textEditor.copy();
                }
                if (ImGui.menuItem("Cut", "Ctrl+X", !ro && textEditor.hasSelection())) {
                    textEditor.cut();
                }
                if (ImGui.menuItem("Delete", "Del", !ro && textEditor.hasSelection())) {
                    textEditor.delete();
                }
                if (ImGui.menuItem("Paste", "Ctrl+V", !ro && ImGui.getClipboardText() != null)) {
                    textEditor.paste();
                }

                ImGui.endMenu();
            }

            ImGui.endMenuBar();
        }
        FontSettings decompilerFont = Main.getPreferences().getDecompilerFont();
        decompilerFont.pushFont();
        textEditor.render(this.getTitle());
        decompilerFont.popFont();
    }

    @Override
    public String getCaption() {
        return "Text Editor";
    }

    private int[] getPalette() {
        Color defaultText = new Color(0xD0, 0xD0, 0xD0, 0xFF);
        Color keyword = new Color(0x56, 0x9C, 0xD6, 0xFF);
        Color number = new Color(0xB5, 0xCE, 0xA8, 0xFF);
        Color string = new Color(0xCE, 0x91, 0x78, 0xFF);
        Color charLiteral = new Color(0xCE, 0x91, 0x78, 0xFF);
        Color punctuation = new Color(0xFF, 0xFF, 0xFF, 0xFF);
        Color preprocessor = new Color(0x40, 0x80, 0x80, 0xFF);
        Color identifier = new Color(0xAA, 0xAA, 0xAA, 0xFF);
        Color knownIdentifier = new Color(0x9B, 0xC6, 0x4D, 0xFF);
        Color preprocIdentifier = new Color(0xC0, 0x40, 0xA0, 0xFF);
        Color comment = new Color(0x6A, 0x99, 0x55, 0xFF);
        Color multiLineComment = new Color(0x6A, 0x99, 0x55, 0xFF);
        Color background = new Color(0x1E, 0x1F, 0x22, 0x1);
        Color cursor = new Color(0xE0, 0xE0, 0xE0, 0xFF);
        Color selection = new Color(0x1E, 0x1F, 0x22, 0xE1);
        Color errorMarker = new Color(0x00, 0x20, 0xFF, 0x80);
        Color breakpoint = new Color(0xF0, 0x80, 0x00, 0x40);
        Color lineNumber = new Color(0x4F, 0x51, 0x51, 0xFF);
        Color currentLineFill = new Color(0x1E, 0x1F, 0x22, 0x40);
        Color currentLineFillInactive = new Color(0x80, 0x80, 0x80, 0x40);
        Color currentLineEdge = new Color(0xA0, 0xA0, 0xA0, 0x40);

        return new int[] {
                defaultText.getRGB(), keyword.getRGB(), number.getRGB(),
                string.getRGB(), charLiteral.getRGB(), punctuation.getRGB(),
                preprocessor.getRGB(), identifier.getRGB(), knownIdentifier.getRGB(),
                preprocIdentifier.getRGB(), comment.getRGB(), multiLineComment.getRGB(),
                background.getRGB(), cursor.getRGB(), selection.getRGB(),
                errorMarker.getRGB(), breakpoint.getRGB(), lineNumber.getRGB(),
                currentLineFill.getRGB(), currentLineFillInactive.getRGB(), currentLineEdge.getRGB()
        };
    }
}
