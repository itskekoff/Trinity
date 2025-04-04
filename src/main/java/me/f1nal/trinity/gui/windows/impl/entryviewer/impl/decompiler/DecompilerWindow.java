package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import com.google.common.eventbus.Subscribe;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseDecompiler;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.packages.other.ExtractArchiveEntryRunnable;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.components.popup.MenuBarProgress;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructure;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructureWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.Stopwatch;
import me.f1nal.trinity.util.SystemUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DecompilerWindow extends ArchiveEntryViewerWindow<ClassTarget> implements IEventListener, IDatabaseSavable<DatabaseDecompiler> {
    private ClassInput selectedClass;
    /**
     * Notifies the selected class must be refreshed.
     */
    private boolean forceRefresh = true;
    /**
     * Text component that is currently hovered.
     */
    private DecompilerComponent hoveredComponent;
    private boolean resetLines;
    /**
     * Selection cursor.
     */
    public final DecompilerCursor cursor = new DecompilerCursor(this);
    private DecompilerAutoScroll autoscrollTo;
    private final Stopwatch focusTime = new Stopwatch();
    private static Stopwatch viewMember = new Stopwatch();

    private String searchQuery = "";
    private int currentSearchIndex = -1;
    private final HashMap<DecompilerLine, DecompilerComponent> searchResults = new HashMap<>();
    private final ImString searchBuffer = new ImString(256);

    private boolean scrollToSearchResult = false;
    private boolean searchBarEnabled = false;


    public DecompilerWindow(ClassTarget classTarget, Trinity trinity) {
        super(trinity, classTarget);
        trinity.getEventManager().registerListener(this);
        this.setDecompileTarget(Objects.requireNonNull(classTarget.getInput()));
        this.setMenuBar(new PopupMenuBar(PopupItemBuilder.create().
                menu("File", file -> file
                        .menuItem("Refresh", () -> this.forceRefresh = true)
                        .predicate(() -> getDecompiledClass() != null, b -> b.separator()
                                .menuItem("Copy", this::copyToClipboard)
                                .menuItem("Save", () -> new ExtractArchiveEntryRunnable(classTarget.getDisplaySimpleName() + ".java", getDecompiledClass().getText().getBytes()).run()))).
                menu("Find", find -> find.menuItem("Search", "Ctrl+F", this::toggleSearchBar))));
    }

    private void toggleSearchBar() {
        this.searchBarEnabled = !this.searchBarEnabled;
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) {
            searchResults.clear();
            currentSearchIndex = -1;
            return;
        }
        DecompiledClass decompiledClass = getDecompiledClass();
        if (decompiledClass == null) {
            return;
        }
        searchResults.clear();
        for (DecompilerLine line : decompiledClass.getLines()) {
            for (DecompilerLineText text : line.getComponents()) {
                if (text.getText().contains(query)) {
                    searchResults.put(line, text.getComponent());
                    break;
                }
            }
        }
        currentSearchIndex = searchResults.isEmpty() ? -1 : 0;
        if (currentSearchIndex == 0) {
            this.scrollToSearchResult = true;
        }
    }

    private void copyToClipboard() {
        final String text = cursor.hasTextSelection() ? cursor.getSelectionText() : this.getDecompiledClass().getText();
        SystemUtil.copyToClipboard(text);
    }

    @Override
    public String getTitle() {
        return getArchiveEntry().getDisplayOrRealName() + ".java";
    }

    public void setDecompileTarget(ClassInput classInput) {
        if (classInput == selectedClass) {
            return;
        }
        selectedClass = classInput;
        if (classInput != null && !trinity.getDatabase().isLoading()) this.save();
        if (this.isFocusGained()) this.updateClassStructure();
    }

    @Override
    protected void onFocusGain() {
        this.focusTime.reset();
        this.updateClassStructure();
    }

    public void updateClassStructure() {
        if (this.selectedClass != null) {
            Main.getWindowManager().addStaticWindow(ClassStructureWindow.class).setClassStructure(new ClassStructure(this.selectedClass));
        }
    }

    public ClassInput getSelectedClass() {
        return selectedClass;
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    protected void renderFrame() {
        if (this.searchBarEnabled) {
            this.drawSearchBar();
            ImGui.separator();
        }

        if (selectedClass != null) {
            this.drawDecompileTab();
        } else {
            ImGui.text("No class selected");
        }
        DecompiledClass decompiledClass = this.getDecompiledClass();
        getMenuBar().setProgress(decompiledClass == null ? new MenuBarProgress("Decompiler", "Decompiling Class", -1) : null);
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        if (event.getClassInput() == this.selectedClass) {
            this.forceRefreshDecompiler();
        }
    }

    public void forceRefreshDecompiler() {
        this.forceRefresh = true;
    }

    @Subscribe
    public void onRefreshDecompilerText(EventRefreshDecompilerText event) {
        DecompiledClass decompiledClass = getDecompiledClass();
        if (getDecompiledClass() != null && event.getPredicate().test(decompiledClass)) {
            this.resetLines = true;
        }
    }

    private ClassInput decompilingInput;

    private void drawDecompileTab() {
        this.runControls();

        ImGui.beginChild("DecompilerWindowChild", 0.F, 0.F, false, ImGuiWindowFlags.HorizontalScrollbar);

        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass == null) {
            ImGui.textUnformatted("...");
        } else {
            if (this.resetLines) {
                decompiledClass.resetLines();
                this.resetLines = false;
            }

            FontSettings decompilerFont = Main.getPreferences().getDecompilerFont();
            decompilerFont.pushFont();
            this.drawDecompiledOutput(decompiledClass);
            decompilerFont.popFont();
        }

        ImGui.endChild();
    }

    private void runControls() {
        if (this.forceRefresh) {
            this.forceRefresh = false;

            try {
                trinity.getDecompiler().decompile(selectedClass, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (trinity.getDecompiler().isDecompileFailed(selectedClass)) {
            ImGui.textColored(ImColor.rgb(245, 80, 80), "Decompilation failed");
        }
    }

    public DecompiledClass getDecompiledClass() {
        return trinity.getDecompiler().getFromCache(selectedClass);
    }

    private void drawDecompiledOutput(DecompiledClass decompiledClass) {
        if (ImGui.isWindowFocused() && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(KeyEvent.VK_F)) {
            this.toggleSearchBar();
        }

        this.hoveredComponent = null;
        float mousePosY = ImGui.getMousePosY() + ImGui.getScrollY() - ImGui.getWindowPosY();
        float mousePosX = ImGui.getMousePosX() + ImGui.getScrollX();
        ImVec2 textSize = ImGui.calcTextSize(String.valueOf(decompiledClass.getLines().size() + 1));
        float lineNumberSpacing = 3.F + textSize.x;
        float cursorPosX = ImGui.getCursorPosX();
        cursor.handleInputs(mousePosX, mousePosY);

        for (DecompilerLine line : decompiledClass.getLines()) {
            boolean isSearchResult = searchResults.containsKey(line);
            final float cursorScreenPosX = ImGui.getCursorScreenPosX();
            int textOffset = 0, sameLines = 0;
            ImGui.setCursorPosX(cursorPosX + lineNumberSpacing);
            for (DecompilerLineText text : line.getComponents()) {
                if (!text.getComponent().render()) {
                    if (searchBarEnabled && isSearchResult && text.getComponent().getColor() == CodeColorScheme.STRING) {
                        decompiledClass.setComponentHighlighted(text.getComponent());
                    }
                    text.render(decompiledClass.isComponentHighlighted(text.getComponent()));
                    ImGui.sameLine(0.F, 0.F);
                }
                if ((this.autoscrollTo != null && text.getComponent() == this.autoscrollTo.findComponent(decompiledClass))) {
                    cursor.setCoordinates(new DecompilerCoordinates(line, textOffset));
                    cursor.setScrollToCursor();
                    this.autoscrollTo = null;
                } else if (this.scrollToSearchResult && currentSearchIndex >= 0 && currentSearchIndex < searchResults.size()) {
                    DecompilerLine result = (DecompilerLine) searchResults.keySet().toArray()[currentSearchIndex];
                    if (line == result) {
                        cursor.setCoordinates(new DecompilerCoordinates(line, textOffset));
                        cursor.setScrollToCursor();
                        this.scrollToSearchResult = false;
                    }
                }
                if (this.hoveredComponent == null && ImGui.isItemHovered()) {
                    this.hoveredComponent = text.getComponent();
                }
                textOffset += text.getText().length();
            }

            float cursorPosY = ImGui.getCursorPosY();
            final boolean hovered = ImGui.isWindowHovered() && mousePosY >= cursorPosY && mousePosY < cursorPosY + textSize.y + ImGui.getStyle().getItemSpacingY();
            if (hovered)
                this.cursor.handleHoveredLineInputs(cursorScreenPosX, lineNumberSpacing, mousePosX, line);
            ImGui.setCursorPosX(cursorPosX);
            ImGui.textColored(CodeColorScheme.LINE_NUMBER, String.valueOf(line.getLineNumber()));
            ImGui.sameLine();
            line.pos = ImGui.getCursorScreenPos().minus(2.5F, 0.F);
            this.cursor.handleLineDrawing(line, cursorScreenPosX, lineNumberSpacing, mousePosX, cursorPosY, textSize);
            ImGui.newLine();
        }

        this.cursor.drawSelectionBox();

        boolean rightClick = ImGui.isWindowHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Right);
        boolean leftClick = !rightClick && ImGui.isWindowHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Left);

        if (this.hoveredComponent != null) {
            List<ColoredString> tooltip = this.hoveredComponent.createTooltip();

            if (tooltip != null) {
                ImGui.beginTooltip();

                ColoredString.drawText(tooltip);

                ImGui.endTooltip();
            }

            if (this.hoveredComponent.getViewMember() != null) {
                if (ImGui.getIO().getKeyCtrl()) {
                    ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

                    if (focusTime.hasPassed(150L) && viewMember.hasPassed(250L) && (ImGui.isKeyPressed(GLFW.GLFW_KEY_B) || leftClick)) {
                        Main.getDisplayManager().openDecompilerView(this.hoveredComponent.getViewMember());
                        viewMember.reset();
                    }
                }
            }

            if (rightClick) {
                PopupItemBuilder popup = this.hoveredComponent.createPopup();

                if (!popup.isEmpty()) {
                    Main.getDisplayManager().showPopup(popup);
                    rightClick = false;
                }
            }
        }

        if (rightClick) {
            Main.getDisplayManager().showPopup(PopupItemBuilder.create().disabled(() -> !cursor.hasTextSelection(), items -> {
                items.menuItem("Copy", this::copyToClipboard);
            }));
        }

        if (cursor.hasTextSelection() && ImGui.isWindowFocused() && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(GLFW.GLFW_KEY_C)) {
            this.copyToClipboard();
        }
    }

    private void drawSearchBar() {
        if (searchQuery != null) {
            searchQuery.getChars(0, Math.min(searchQuery.length(), searchBuffer.getLength()), searchBuffer.get().toCharArray(), 0);
        }
        ImGui.setNextItemWidth(200);
        ImGui.inputText("##search", searchBuffer);
        ImGui.sameLine();
        if (ImGui.button("Search")) {
            searchQuery = searchBuffer.get().trim();
            performSearch(searchQuery);
        }

        if (ImGui.button("Find Next")) this.findNext();
        ImGui.sameLine();
        if (ImGui.button("Find Previous")) this.findPrevious();
        if (!searchResults.isEmpty()) {
            ImGui.sameLine();
            ImGui.text("Found %s string occurrences".formatted(searchResults.size()));
        }
        ImGui.separator();
    }

    private void findNext() {
        if (!searchResults.isEmpty()) {
            currentSearchIndex = (currentSearchIndex + 1) % searchResults.size();
            this.scrollToSearchResult = true;
        }
    }

    private void findPrevious() {
        if (!searchResults.isEmpty()) {
            currentSearchIndex = (currentSearchIndex - 1 + searchResults.size()) % searchResults.size();
            this.scrollToSearchResult = true;
        }
    }

    public void setDecompileTarget(Input<?> input) {
        this.autoscrollTo = new DecompilerAutoScroll(input);
        this.setDecompileTarget(input.getOwningClass());
    }

    @Override
    public DatabaseDecompiler createDatabaseObject() {
        return new DatabaseDecompiler(this.selectedClass.getRealName());
    }
}
