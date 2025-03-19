package me.f1nal.trinity.refactor.globalrename.mappings.api.interfaces;

import me.f1nal.trinity.execution.ClassInput;

/**
 * @author itskekoff
 * @since 23:35 of 18.03.2025
 */
public interface NameGenerator {
    String generateName(ClassInput context);
    void reset();
}
