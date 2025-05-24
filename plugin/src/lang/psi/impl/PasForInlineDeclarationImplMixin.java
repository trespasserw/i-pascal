package com.siberika.idea.pascal.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.siberika.idea.pascal.lang.psi.PasCustomAttributeDecl;
import com.siberika.idea.pascal.lang.psi.PasForInlineDeclaration;
import com.siberika.idea.pascal.lang.psi.PascalNamedElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class PasForInlineDeclarationImplMixin extends PascalPsiElementImpl implements PasForInlineDeclaration {
    public PasForInlineDeclarationImplMixin(ASTNode node) {
        super(node);
    }

    public @NotNull List<PasCustomAttributeDecl> getCustomAttributeDeclList() {
        return Collections.emptyList();
    }

    public @NotNull List<? extends PascalNamedElement> getNamedIdentDeclList() {
        return Collections.singletonList(getNamedIdent());
    }
}
