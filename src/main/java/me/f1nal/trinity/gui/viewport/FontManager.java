package me.f1nal.trinity.gui.viewport;

import com.google.common.io.Resources;
import imgui.*;
import imgui.gl3.ImGuiImplGl3;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.FontEnum;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.FontSettings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private Map<String, byte[]> resourceCache;

    public void setupFonts() {
        this.resourceCache = new HashMap<>();

        this.buildFonts(Main.getPreferences().getDefaultFont());
        this.buildFonts(Main.getPreferences().getDecompilerFont());
        this.resourceCache = null;
    }

    private void buildFonts(FontSettings fontSettings) {
        final float size = fontSettings.getSize();
        fontSettings.setBuiltSize(size);

        final ImGuiIO io = ImGui.getIO();
        final ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setMergeMode(true);
        ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
        rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());
        rangesBuilder.addRanges(io.getFonts().getGlyphRangesCyrillic());
        rangesBuilder.addRanges(FontAwesomeIcons._IconRange);
        short[] glyphRanges = rangesBuilder.buildRanges();

        this.addTextFont(FontEnum.INTER, fontSettings, io, fontConfig, glyphRanges, size);
        fontConfig.setMergeMode(true);
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fa-solid-900.ttf"), size, fontConfig, glyphRanges));
        this.addTextFont(FontEnum.JETBRAINS_MONO, fontSettings, io, fontConfig, glyphRanges, size);
        fontConfig.setMergeMode(true);
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fa-solid-900.ttf"), size, fontConfig, glyphRanges));
        this.addTextFont(FontEnum.ZED_MONO, fontSettings, io, fontConfig, glyphRanges, size);
        fontConfig.setMergeMode(true);
        fontSettings.setIconFont(io.getFonts().addFontFromMemoryTTF(loadFromResources("fa-solid-900.ttf"), size, fontConfig, glyphRanges));
    }

    private void addTextFont(FontEnum fontEnum, FontSettings fontSettings,
                             ImGuiIO io, ImFontConfig fontConfig, short[] glyphRanges, float size) {
        fontConfig.setMergeMode(false);
        fontSettings.registerFont(fontEnum, io.getFonts().addFontFromMemoryTTF(loadFromResources(fontEnum.getPath()), size, fontConfig, glyphRanges));
    }

    private byte[] loadFromResources(String name) {
        return resourceCache.computeIfAbsent(name, (k) -> {
            try {
                return Resources.toByteArray(Resources.getResource("fonts/".concat(k)));
            } catch (IOException e) {
                throw new RuntimeException(String.format("Loading font resource '%s'", name), e);
            }
        });
    }
}
