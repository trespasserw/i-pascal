package com.siberika.idea.pascal;

import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;

@RunWith(JUnit38ClassRunner.class)
public abstract class PascalLightCodeInsightFixtureTestCase extends BasePlatformTestCase {
    @NotNull
    public Module getModule() {
        return myFixture.getModule();
    }
}
