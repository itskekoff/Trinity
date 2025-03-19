package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.FullGlobalRename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.FullRenameMember;

public class FullRenameClasses extends FullRenameMember {
    public FullRenameClasses(FullGlobalRename parent) {
        super(parent, "Classes", "Class");
    }

    @Override
    public void refactor(GlobalRenameContext context) {
        for (ClassInput classInput : context.execution().getClassList()) {
            if (context.nameHeuristics().isObfuscated(classInput.getDisplaySimpleName(), InputType.CLASS)) {
                String newName = nameGenerator.generateName(null);
                context.renames().add(new Rename(classInput, classInput.getClassTarget().getPackage().getChildrenPath(newName)));
            }
        }
    }
}