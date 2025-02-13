package me.f1nal.trinity.gui.components.general;

import imgui.ImGui;
import imgui.type.ImString;
import jnafilechooser.api.JnaFileChooser;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.GuiUtil;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;

public class FileSelectorComponent {
    public static final FilenameFilter TDB_FILE_FILTER = (f, n) -> n.toLowerCase().endsWith(".tdb");

    private final String label;
    private final ImString path = new ImString(256);
    private String lastDirectory;
    private final FilenameFilter filenameFilter;
    private final int mode;
    private final String componentId = ComponentId.getId(this.getClass());

    public FileSelectorComponent(String label, String path, FilenameFilter filenameFilter, int mode) {
        this.label = label;
        this.path.set(path);
        this.filenameFilter = filenameFilter;
        this.mode = mode;
    }

    public void draw() {
        ImGui.text(this.label);
        ImGui.inputText("###" + this.componentId, this.path);
        ImGui.sameLine();
        if (ImGui.smallButton("...")) {
            File result = this.openFileChooser();
            if (result != null) this.path.set(result);
        }
        GuiUtil.tooltip("Open File Chooser");
    }

    public File getFile() {
        return new File(this.path.get());
    }

    public void setFile(File file) {
        this.path.set(file.getAbsolutePath());
    }

    public File openFileChooser() {
        JnaFileChooser fc = new JnaFileChooser();
        fc.addFilter("Java files", "zip", "jar");
        fc.addFilter("All Files", "*");
        fc.setCurrentDirectory(lastDirectory != null ? lastDirectory : getParentFromPath());
        fc.setDefaultFileName(path.get());
        if (fc.showOpenDialog(null)) {
            if (fc.getCurrentDirectory() != null) {
                this.lastDirectory = fc.getCurrentDirectory().getAbsolutePath();
            }
            return fc.getSelectedFile();
        }
        return null;
    }

    private String getParentFromPath() {
        File file = new File(path.get());
        if (!file.isDirectory()) return file.getParentFile().getAbsolutePath();
        return file.getAbsolutePath();
    }
}
