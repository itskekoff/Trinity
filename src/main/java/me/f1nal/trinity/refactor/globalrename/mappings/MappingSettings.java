package me.f1nal.trinity.refactor.globalrename.mappings;

import me.f1nal.trinity.refactor.globalrename.mappings.api.MappingType;
import me.f1nal.trinity.refactor.globalrename.mappings.api.StringTable;

/**
 * @author itskekoff
 * @since 23:34 of 18.03.2025
 */
public class MappingSettings {
    private MappingType mappingType;
    private StringTable stringTable;
    private int nameLength;

    public MappingSettings(MappingType mappingType, StringTable stringTable, int nameLength) {
        this.mappingType = mappingType;
        this.stringTable = stringTable;
        this.nameLength = nameLength;
    }

    public MappingType getMappingType() { return mappingType; }
    public void setMappingType(MappingType mappingType) { this.mappingType = mappingType; }
    public StringTable getStringTable() { return stringTable; }
    public void setStringTable(StringTable stringTable) { this.stringTable = stringTable; }
    public int getNameLength() { return nameLength; }
    public void setNameLength(int nameLength) { this.nameLength = nameLength; }
}
