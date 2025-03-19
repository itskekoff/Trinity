package me.f1nal.trinity.refactor.globalrename.mappings.impl;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.refactor.globalrename.mappings.api.interfaces.NameGenerator;

/**
 * @author itskekoff
 * @since 23:35 of 18.03.2025
 */
public class SequentialNameGenerator implements NameGenerator {
    private final String prefix;
    private int count;

    public SequentialNameGenerator(String prefix) {
        this.prefix = prefix;
        this.count = 0;
    }

    @Override
    public String generateName(ClassInput context) {
        return this.prefix + (++this.count);
    }

    @Override
    public void reset() {
        this.count = 0;
    }
}
