package me.f1nal.trinity.refactor.globalrename.mappings.impl;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.refactor.globalrename.mappings.api.StringTable;
import me.f1nal.trinity.refactor.globalrename.mappings.api.interfaces.NameGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * @author itskekoff
 * @since 23:37 of 18.03.2025
 */
public class RandomNameGenerator implements NameGenerator {
    private final StringTable stringTable;
    private final int nameLength;

    private final Set<String> usedNames;

    public RandomNameGenerator(StringTable stringTable, int nameLength) {
        this.stringTable = stringTable;
        this.nameLength = nameLength;
        this.usedNames = new HashSet<>();
    }

    @Override
    public String generateName(ClassInput context) {
        String name;
        do {
            name = generateRandomName();
        } while (this.usedNames.contains(name));
        this.usedNames.add(name);
        return name;
    }

    private String generateRandomName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.nameLength; i++) {
            sb.append(this.stringTable.getRandomChar());
        }
        return sb.toString();
    }

    @Override
    public void reset() {
        this.usedNames.clear();
    }
}