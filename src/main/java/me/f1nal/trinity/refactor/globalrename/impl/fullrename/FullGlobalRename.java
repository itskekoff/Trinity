package me.f1nal.trinity.refactor.globalrename.impl.fullrename;

import imgui.ImGui;
import imgui.type.ImInt;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.refactor.globalrename.GlobalRenameType;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.FullRenameMember;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl.FullRenameClasses;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl.FullRenameFields;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl.FullRenameMethods;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl.FullRenamePackages;
import me.f1nal.trinity.refactor.globalrename.mappings.MappingSettings;
import me.f1nal.trinity.refactor.globalrename.mappings.api.MappingType;
import me.f1nal.trinity.refactor.globalrename.mappings.api.StringTable;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

public final class FullGlobalRename extends GlobalRenameType {
    private final List<FullRenameMember> renameMembers = List.of(
            new FullRenamePackages(this),
            new FullRenameClasses(this),
            new FullRenameFields(this),
            new FullRenameMethods(this)
    );

    private final MappingSettings mappingSettings = new MappingSettings(MappingType.SEQUENTIAL, StringTable.EN, 5);
    private final FileSelectorComponent fileSelector = new FileSelectorComponent("Dictionary File",  new File("").getAbsolutePath(), null, 0);

    private final Map<Object, Set<String>> usedNamesMap = new HashMap<>();

    public FullGlobalRename() {
        super("Full Rename", "Renames all classes/methods/fields.");
    }

    @Override
    public void drawInputs() {
        if (ImGui.beginCombo("Mapping Type", mappingSettings.getMappingType().name())) {
            for (MappingType type : MappingType.values()) {
                if (ImGui.selectable(type.name(), mappingSettings.getMappingType() == type)) {
                    mappingSettings.setMappingType(type);
                }
            }
            ImGui.endCombo();
        }

        switch (mappingSettings.getMappingType()) {
            case RANDOM:
                if (ImGui.beginCombo("String Table", mappingSettings.getStringTable().name())) {
                    for (StringTable table : StringTable.values()) {
                        if (table != StringTable.EMPTY) {
                            if (ImGui.selectable(table.name(), mappingSettings.getStringTable() == table)) {
                                mappingSettings.setStringTable(table);
                            }
                        }
                    }
                    ImGui.endCombo();
                }
                ImInt nameLengthRandom = new ImInt(mappingSettings.getNameLength());
                if (ImGui.inputInt("Name Length", nameLengthRandom, 1)) {
                    mappingSettings.setNameLength(Math.max(1, nameLengthRandom.get()));
                }
                break;
            case CUSTOM:
                fileSelector.draw();
                break;
            case KEYWORDS:
                ImGui.text("Using Java keywords as dictionary.");
                break;
            case SEQUENTIAL:
                break;
        }

        ImGui.separator();
        ImGui.text("Rename Options:");
        renameMembers.forEach(FullRenameMember::draw);
    }

    @Override
    public void refactor(GlobalRenameContext context) {
        usedNamesMap.clear();
        renameMembers.stream().filter(FullRenameMember::isEnabled).forEach(r -> {
            r.prepare();
            r.refactor(context);
        });
    }

    public MappingSettings getMappingSettings() {
        return mappingSettings;
    }

    public File getDictionaryFile() {
        return mappingSettings.getMappingType() == MappingType.CUSTOM ? fileSelector.getFile() : null;
    }

    public boolean isNameAvailable(String name, Object context) {
        Set<String> usedNames = usedNamesMap.computeIfAbsent(context, k -> new HashSet<>());
        return !usedNames.contains(name);
    }

    public void registerName(String name, Object context) {
        Set<String> usedNames = usedNamesMap.computeIfAbsent(context, k -> new HashSet<>());
        usedNames.add(name);
    }
}