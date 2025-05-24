package com.siberika.idea.pascal.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.siberika.idea.pascal.lang.psi.PasInlineVarDeclaration;
import com.siberika.idea.pascal.lang.psi.PascalNamedElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class PasInlineVarDeclarationImplMixin extends PascalPsiElementImpl implements PasInlineVarDeclaration {
    public PasInlineVarDeclarationImplMixin(ASTNode node) {
        super(node);
    }

    public @NotNull List<? extends PascalNamedElement> getNamedIdentDeclList() {
        return getNamedIdentList();
    }
}
