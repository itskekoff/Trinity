package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl;

import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.FullGlobalRename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.FullRenameMember;

import java.util.List;

public class FullRenamePackages extends FullRenameMember {
    public FullRenamePackages(FullGlobalRename parent) {
        super(parent, "Packages", "pkg");
    }

    @Override
    public void refactor(GlobalRenameContext context) {
        for (Package pkg : context.execution().getAllPackages()) {
            if (pkg.isArchive() || !context.nameHeuristics().isObfuscated(pkg.getName(), InputType.PACKAGE) || pkg.getName().equals("META-INF")) {
                continue;
            }
            String newName = nameGenerator.generateName(null);
            context.renames().add(new Rename(pkg::rename, newName));
        }
    }
}
