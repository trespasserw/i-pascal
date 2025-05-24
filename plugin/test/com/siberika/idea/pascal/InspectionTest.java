package com.siberika.idea.pascal;

import com.siberika.idea.pascal.lang.inspection.*;

@SuppressWarnings("unchecked")
public class InspectionTest extends PascalLightCodeInsightFixtureTestCase {
    @Override
    protected String getTestDataPath() {
        return "testData/annotator";
    }

    @Override
    protected boolean isWriteActionRequired() {
        return false;
    }

    public void testUnusedUnits() {
        myFixture.enableInspections(UnusedUnitsInspection.class);
        myFixture.configureByFiles("unusedUnits.pas", "types.pas", "objects.pas", "scoped.types.pas", "exception.pas", "interfaces.pas", "routines.pas", "enumTypes.pas");
        myFixture.checkHighlighting(true, false, false);
    }

    public void testUsesInImplementation() {
        myFixture.enableInspections(UnusedUnitsInspection.class);
        myFixture.configureByFiles("usesInImplementation.pas", "types.pas");
        myFixture.checkHighlighting(true, false, false);
    }

    public void testUsesUnusedIdents() {
        myFixture.enableInspections(UnusedIdentsInspection.class);
        myFixture.configureByFiles("unusedIdents.pas");
        myFixture.checkHighlighting(true, false, false);
    }

    public void testUnimplementedMethods() {
        myFixture.enableInspections(NotImplementedInspection.class);
        myFixture.configureByFiles("unimplementedMethods.pas");
        myFixture.checkHighlighting(true, false, false);
    }

    public void testDestructorInherited() {
        myFixture.enableInspections(DestructorInheritedInspection.class);
        myFixture.configureByFiles("destructorInherited.pas");
        myFixture.checkHighlighting(true, false, false);
    }

    public void testResultAssignment() {
        myFixture.enableInspections(ResultAssignmentInspection.class);
        myFixture.configureByFiles("resultAssignment.pas");
        myFixture.checkHighlighting(true, false, false);
    }

}
