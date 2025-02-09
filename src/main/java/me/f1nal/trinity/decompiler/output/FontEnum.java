package me.f1nal.trinity.decompiler.output;

import imgui.ImFont;
import me.f1nal.trinity.util.INameable;

import java.util.Arrays;
import java.util.Objects;

public enum FontEnum implements INameable {
    JETBRAINS_MONO("JetBrains Mono", "JetBrainsMonoNL-Regular.ttf"),
    ZED_MONO("Zed Mono", "JetBrainsMonoNL-Regular.ttf"),
    INTER("Inter", "inter-regular.ttf");

    private final String name;
    private final String path;

    FontEnum(String name, String path) {
        this.name = name;
        this.path = path;
    }

    @Override
    public String getName() {
        return this.name;
    }
    public String getPath() {
        return this.path;
    }

    public static FontEnum getFont(String name) {
        return Objects.requireNonNull(Arrays.stream(values()).filter(value -> value.getName().equals(name)).findFirst().orElse(null), String.format("Font type '%s'", name));
    }
}
