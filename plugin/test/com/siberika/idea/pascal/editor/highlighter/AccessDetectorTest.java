package com.siberika.idea.pascal.editor.highlighter;

import com.intellij.psi.util.PsiTreeUtil;
import com.siberika.idea.pascal.PascalLightCodeInsightFixtureTestCase;
import com.siberika.idea.pascal.lang.psi.*;
import com.siberika.idea.pascal.lang.psi.impl.PascalModuleImpl;
import com.siberika.idea.pascal.lang.references.PasReferenceUtil;

import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AccessDetectorTest extends PascalLightCodeInsightFixtureTestCase {
    @Override
    protected String getTestDataPath() {
        return "testData/misc";
    }

    public void testAccessDetector() {
        myFixture.configureByFiles("accessDetector.pas");
        PascalModuleImpl mod = (PascalModuleImpl) PasReferenceUtil.findUnit(myFixture.getProject(),
                PasReferenceUtil.findUnitFiles(myFixture.getProject(), getModule()), "accessDetector");
        doTestRefs(PsiTreeUtil.findChildrenOfAnyType(mod, PasNamedIdent.class, PasSubIdent.class, PasRefNamedIdent.class));
        doTestDecls(PsiTreeUtil.findChildrenOfAnyType(mod, PasVarDeclaration.class, PasConstDeclaration.class));
    }

    private void doTestDecls(Collection<PascalPsiElement> decls) {
        PascalReadWriteAccessDetector ad = new PascalReadWriteAccessDetector();
        for (PascalPsiElement decl : decls) {
            PascalNamedElement ident = PsiTreeUtil.findChildOfType(decl, PasNamedIdent.class);
            ident = ident != null ? ident : PsiTreeUtil.findChildOfType(decl, PasRefNamedIdent.class);
            assertNotNull("Ident is null: " + decl, ident);
            boolean writable = (decl instanceof PasConstDeclaration) || (ident.getName().endsWith("W"));
            assertTrue(!writable || ad.isDeclarationWriteAccess(ident));
        }
    }

    private void doTestRefs(Collection<PascalNamedElement> symbols) {
        for (PascalNamedElement symbol : symbols) {
            boolean res = PascalReadWriteAccessDetector.isWriteAccess(symbol);
            assertTrue(symbol.getName().endsWith("W") || !res);
        }
    }

}
