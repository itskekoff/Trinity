package me.f1nal.trinity.refactor.globalrename.mappings.impl;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.FullGlobalRename;
import me.f1nal.trinity.refactor.globalrename.mappings.api.MappingType;
import me.f1nal.trinity.refactor.globalrename.mappings.api.interfaces.NameGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author itskekoff
 * @since 23:55 of 18.03.2025
 */
public class CustomNameGenerator implements NameGenerator {
    private final FullGlobalRename parent;
    private final List<String> dictionary;
    private final MappingType mappingType;
    private final File dictionaryFile;
    private int index;

    private static final List<String> JAVA_KEYWORDS = Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while"
    );

    public CustomNameGenerator(FullGlobalRename parent, MappingType mappingType, File dictionaryFile) {
        this.parent = parent;
        this.mappingType = mappingType;
        this.dictionaryFile = dictionaryFile;
        this.dictionary = new ArrayList<>();
        this.index = 0;
        loadDictionary();
    }

    private void loadDictionary() {
        if (mappingType == MappingType.KEYWORDS) {
            dictionary.addAll(JAVA_KEYWORDS);
        } else if (mappingType == MappingType.CUSTOM && dictionaryFile != null && dictionaryFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(dictionaryFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && line.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                        dictionary.add(line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to load dictionary file: " + e.getMessage());
            }
        }
        if (dictionary.isEmpty()) {
            dictionary.add("unnamed");
        }
    }

    @Override
    public String generateName(ClassInput context) {
        for (int i = 0; i < dictionary.size(); i++) {
            String candidate = dictionary.get((index + i) % dictionary.size());
            if (parent.isNameAvailable(candidate, context)) {
                parent.registerName(candidate, context);
                index = (index + i + 1) % dictionary.size();
                return candidate;
            }
        }
        String fallback = "unnamed_" + index++;
        parent.registerName(fallback, context);
        return fallback;
    }

    @Override
    public void reset() {
        index = 0;
    }
}