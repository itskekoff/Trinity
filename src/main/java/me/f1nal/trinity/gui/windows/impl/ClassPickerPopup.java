package me.f1nal.trinity.gui.windows.impl;

import imgui.ImGui;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.gui.components.SearchBar;
import me.f1nal.trinity.gui.windows.api.PopupWindow;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ClassPickerPopup extends PopupWindow {
    private final SearchBar searchBar = new SearchBar();
    private final Predicate<ClassTarget> valid;
    private final Consumer<ClassTarget> consumer;
    private final ImBoolean exactSearch = new ImBoolean(false);
    public ClassPickerPopup(Trinity trinity, Predicate<ClassTarget> valid, Consumer<ClassTarget> consumer) {
        super("Class Picker", trinity);
        this.valid = valid;
        this.consumer = consumer;
    }

    @Override
    protected void renderFrame() {
        String search = searchBar.drawAndGet();
        ImGui.sameLine();

        float fixedWidth = 430.0f;
        float checkboxWidth = 20.0f;
        float posX = fixedWidth - checkboxWidth;
        ImGui.setCursorPosX(posX);

        ImGui.checkbox("##", exactSearch);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("Enable exact phrase matching");
            ImGui.endTooltip();
        }
        ClassTarget input = null;
        ImGui.beginChild("class search child" + getPopupId(), 430, 200);
        if (!ImGui.beginTable("class search table" + getPopupId(), 1, ImGuiTableFlags.Borders | ImGuiTableFlags.SizingFixedFit)) {
            return;
        }
        ImGui.tableSetupColumn("Name", ImGuiTableColumnFlags.NoResize);
        ImGui.tableHeadersRow();
        for (Map.Entry<String, ClassTarget> className : trinity.getExecution().getClassTargetMap().entrySet()) {
            if (!valid.test(className.getValue())) {
                continue;
            }

            String name = className.getValue().getDisplayOrRealName();
            if ((!this.exactSearch.get() && name.contains(search)) || this.exactSearch.get() && name.equals(search)) {
                if (input == null) input = className.getValue();
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);
                ImGui.text(name);
                if (ImGui.isItemHovered()) ImGui.setTooltip("Click to select");
                if (ImGui.isItemClicked(0)) {
                    searchBar.getSearchText().set(name);
                }
            }
        }
        ImGui.endTable();
        ImGui.endChild();
        if (input == null) ImGui.beginDisabled();
        else ImGui.text(input.getDisplayOrRealName());
        if (ImGui.smallButton("Select this class")) {
            this.closeWithClass(input);
        }
        if (input == null) ImGui.endDisabled();
    }

    public void closeWithClass(ClassTarget classInput) {
        this.consumer.accept(classInput);
        this.close();
    }
}
