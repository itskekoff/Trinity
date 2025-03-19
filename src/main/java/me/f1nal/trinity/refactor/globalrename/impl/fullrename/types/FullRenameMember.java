package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types;

import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.FullGlobalRename;
import me.f1nal.trinity.refactor.globalrename.mappings.MappingSettings;
import me.f1nal.trinity.refactor.globalrename.mappings.api.MappingType;
import me.f1nal.trinity.refactor.globalrename.mappings.api.interfaces.NameGenerator;
import me.f1nal.trinity.refactor.globalrename.mappings.impl.CustomNameGenerator;
import me.f1nal.trinity.refactor.globalrename.mappings.impl.RandomNameGenerator;
import me.f1nal.trinity.refactor.globalrename.mappings.impl.SequentialNameGenerator;

public abstract class FullRenameMember {
    protected final FullGlobalRename parent;
    private final String label, namePrefix;
    private final MemorableCheckboxComponent enabled;
    protected NameGenerator nameGenerator;

    protected FullRenameMember(FullGlobalRename parent, String label, String namePrefix) {
        this.parent = parent;
        this.label = label;
        this.namePrefix = namePrefix;
        this.enabled = new MemorableCheckboxComponent("fullRename" + label, label, true);
    }

    public abstract void refactor(GlobalRenameContext context);

    public final void prepare() {
        MappingSettings settings = this.parent.getMappingSettings();
        switch (settings.getMappingType()) {
            case SEQUENTIAL:
                this.nameGenerator = new SequentialNameGenerator(namePrefix);
                break;
            case RANDOM:
                this.nameGenerator = new RandomNameGenerator(settings.getStringTable(), settings.getNameLength());
                break;
            case CUSTOM:
            case KEYWORDS:
                this.nameGenerator = new CustomNameGenerator(parent, settings.getMappingType(), parent.getDictionaryFile());
                break;
        }
        this.nameGenerator.reset();
    }

    public final void draw() {
        this.enabled.draw();
    }

    public final boolean isEnabled() {
        return enabled.isChecked();
    }
}