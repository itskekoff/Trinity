package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl;

import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.FullGlobalRename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.FullRenameClassMember;

public class FullRenameFields extends FullRenameClassMember<FieldInput> {
    public FullRenameFields(FullGlobalRename parent) {
        super(parent, "Fields", "field", FieldInput.class);
    }

    @Override
    protected void refactorMember(FieldInput member, GlobalRenameContext context) {
        if (context.nameHeuristics().isObfuscated(member.getDisplayName().getName(), member.getType())) {
            String newName = nameGenerator.generateName(member.getOwningClass());
            context.renames().add(new Rename(member, newName));
        }
    }
}