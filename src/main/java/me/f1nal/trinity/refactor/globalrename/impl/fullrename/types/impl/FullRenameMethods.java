package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl;

import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.FullGlobalRename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.FullRenameClassMember;

public class FullRenameMethods extends FullRenameClassMember<MethodInput> {
    public FullRenameMethods(FullGlobalRename parent) {
        super(parent, "Methods", "method", MethodInput.class);
    }

    @Override
    protected void refactorMember(MethodInput member, GlobalRenameContext context) {
        if (!member.isInitOrClinit() && context.nameHeuristics().isObfuscated(member.getDisplayName().getName(), member.getType())) {
            String newName = nameGenerator.generateName(member.getOwningClass());
            context.renames().add(new Rename(member, newName));
        }
    }
}
